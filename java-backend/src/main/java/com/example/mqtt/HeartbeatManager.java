package com.example.mqtt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.controller.ControllerService;
import com.example.lobby.LobbyRepository;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

/**
 * Verwaltet den Ping/Pong-Heartbeat fuer Controller.
 * - Regelmaessig: Controller werden zyklisch angepingt.
 * - Vor neuer Frage: kurzer Zusatzcheck, damit nur erreichbare Controller aktiv bleiben.
 */
public class HeartbeatManager {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatManager.class);
    private static final long PERIODIC_INTERVAL_MS = 3_000;  // Ping-Intervall
    private static final long PONG_TIMEOUT_MS = 3_000;       // Wartezeit auf Pong
    private static final int MISSED_THRESHOLD = 2;           // Ab hier wird OFFLINE gesetzt
    private static final long PRE_QUESTION_WAIT_MS = 3_000;

    private final Vertx vertx;
    private final MqttService mqttService;
    private final ControllerService controllerService;
    private final LobbyRepository lobbyRepository;
    private final EventBus eventBus;

    private final Map<String, Integer> missedPingsByController = new ConcurrentHashMap<>();
    private final Map<String, Long> pendingPingTimerIds = new ConcurrentHashMap<>();
    private final Set<String> preQuestionAwaitingPongs = ConcurrentHashMap.newKeySet();
    private Long periodicTimerId;

    public HeartbeatManager(Vertx vertx, MqttService mqttService, ControllerService controllerService,
            LobbyRepository lobbyRepository) {
        this.vertx = vertx;
        this.mqttService = mqttService;
        this.controllerService = controllerService;
        this.lobbyRepository = lobbyRepository;
        this.eventBus = vertx.eventBus();
    }

    public void start() {
        if (periodicTimerId != null) {
            return;
        }
        periodicTimerId = vertx.setPeriodic(PERIODIC_INTERVAL_MS, id -> runPeriodicPing());
        eventBus.consumer("heartbeat.preQuestionPing", msg -> {
            JsonObject body = msg.body() instanceof JsonObject ? (JsonObject) msg.body() : new JsonObject();
            Long sessionId = body.getLong("session_id");
            if (sessionId == null) {
                msg.reply(new JsonObject().put("error", "session_id required"));
                return;
            }
            runPreQuestionPing(sessionId, () -> msg.reply(new JsonObject().put("ok", true)));
        });
        logger.info("HeartbeatManager started (periodic {}s, pre-question {}s)", PERIODIC_INTERVAL_MS / 1000, PRE_QUESTION_WAIT_MS / 1000);
    }

    public void stop() {
        if (periodicTimerId != null) {
            vertx.cancelTimer(periodicTimerId);
            periodicTimerId = null;
        }
        pendingPingTimerIds.values().forEach(vertx::cancelTimer);
        pendingPingTimerIds.clear();
        missedPingsByController.clear();
        preQuestionAwaitingPongs.clear();
    }

    /** Wird aufgerufen, sobald fuer einen Controller ein Pong ankommt. */
    public void onPongReceived(String controllerId) {
        if (controllerId == null || controllerId.isBlank()) {
            return;
        }
        String cid = controllerId.trim();
        controllerService.touchLastSeen(cid, ar -> {});
        missedPingsByController.put(cid, 0);
        Long timerId = pendingPingTimerIds.remove(cid);
        if (timerId != null) {
            vertx.cancelTimer(timerId);
        }
        preQuestionAwaitingPongs.remove(cid);
    }

    private void runPeriodicPing() {
        lobbyRepository.fetchControllerIdsInActiveSession()
                .onSuccess(rows -> {
                    for (Row row : rows) {
                        String controllerId = row.getString("controller_id");
                        if (controllerId != null && !controllerId.isBlank()) {
                            sendPingAndSchedulePongTimeout(controllerId);
                        }
                    }
                })
                .onFailure(err -> logger.warn("Failed to fetch controllers for ping: {}", err.getMessage()));
    }

    private void sendPingAndSchedulePongTimeout(String controllerId) {
        String requestId = UUID.randomUUID().toString();
        long ts = System.currentTimeMillis();
        mqttService.publishPing(controllerId, requestId, ts);

        long timerId = vertx.setTimer(PONG_TIMEOUT_MS, id -> {
            pendingPingTimerIds.remove(controllerId);
            int missed = missedPingsByController.merge(controllerId, 1, Integer::sum);
            if (missed >= MISSED_THRESHOLD) {
                markOfflineAndNotify(controllerId);
                missedPingsByController.remove(controllerId);
            }
        });
        pendingPingTimerIds.put(controllerId, timerId);
    }

    private void runPreQuestionPing(Long sessionId, Runnable onDone) {
        lobbyRepository.fetchControllerIdsBySessionId(sessionId)
                .onSuccess(rows -> {
                    List<String> ids = new ArrayList<>();
                    for (Row row : rows) {
                        String c = row.getString("controller_id");
                        if (c != null && !c.isBlank()) ids.add(c.trim());
                    }
                    if (ids.isEmpty()) {
                        onDone.run();
                        return;
                    }
                    preQuestionAwaitingPongs.addAll(ids);
                    String requestId = UUID.randomUUID().toString();
                    long ts = System.currentTimeMillis();
                    for (String cid : ids) {
                        mqttService.publishPing(cid, requestId, ts);
                    }
                    vertx.setTimer(PRE_QUESTION_WAIT_MS, id -> {
                        Set<String> toMarkOffline = new HashSet<>(preQuestionAwaitingPongs);
                        preQuestionAwaitingPongs.clear();
                        for (String cid : toMarkOffline) {
                            markOfflineAndNotify(cid);
                        }
                        onDone.run();
                    });
                })
                .onFailure(err -> {
                    logger.warn("Pre-question ping failed to get controllers: {}", err.getMessage());
                    onDone.run();
                });
    }

    private void markOfflineAndNotify(String controllerId) {
        logger.info("Controller {} marked OFFLINE (no pong)", controllerId);
        controllerService.setControllerOffline(controllerId, ar -> {
            eventBus.publish("lobby.updated", new JsonObject().put("controller_id", controllerId).put("status", "OFFLINE"));
            eventBus.publish("game.checkNoPlayersLeft", null);
        });
    }
}
