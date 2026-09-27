package com.example.mqtt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.controller.ControllerService;
import com.example.game.GameService;
import com.example.lobby.LobbyService;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttController {

    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
    private final MqttService mqttService;
    private final ControllerService controllerService;
    private final LobbyService lobbyService;
    private final GameService gameService;
    private final MqttClient mqttClient;
    private final EventBus eventBus;
    private final HeartbeatManager heartbeatManager;

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "quiz-arena/";

    public MqttController(MqttClient mqttClient, Vertx vertx) {
        this.mqttClient = mqttClient;
        this.mqttService = new MqttService(mqttClient);
        this.controllerService = new ControllerService();
        this.lobbyService = new LobbyService();
        this.gameService = new GameService(vertx);
        this.eventBus = vertx.eventBus();
        this.heartbeatManager = new HeartbeatManager(vertx, mqttService, controllerService, new com.example.lobby.LobbyRepository());
        this.heartbeatManager.start();
    }

    // --- EventBus -> MQTT ---
    public void registerEventBusConsumers() {
        this.eventBus.consumer("game.start", msg -> {
            logger.info("Message received via EventBus: 'game.start'");
            mqttService.publishGameStart();
        });

        this.eventBus.consumer("game.state", msg -> {
            logger.info("Message received via EventBus: 'game.state'");
            Object body = msg.body();
            JsonObject payload = body instanceof JsonObject
                    ? (JsonObject) body
                    : new JsonObject().put("state", String.valueOf(body));
            mqttService.publishGameState(payload);
        });

        this.eventBus.consumer("game.countdown", msg -> {
            logger.info("Message received via EventBus: 'game.countdown'");
            Object body = msg.body();
            JsonObject payload = body instanceof JsonObject
                    ? (JsonObject) body
                    : new JsonObject().put("countdown", body);
            mqttService.publishGameCountdown(payload);
        });

        this.eventBus.consumer("game.question", msg -> {
            logger.info("Message received via EventBus: 'game.question'");
            Object body = msg.body();
            JsonObject payload = body instanceof JsonObject
                    ? (JsonObject) body
                    : new JsonObject().put("text", String.valueOf(body));
            mqttService.publishGameQuestion(payload);
        });

        this.eventBus.consumer("game.question.timer", msg -> {
            Object body = msg.body();
            JsonObject payload = body instanceof JsonObject
                    ? (JsonObject) body
                    : new JsonObject().put("seconds_left", body);
            mqttService.publishGameQuestionTimer(payload);
        });

        this.eventBus.consumer("game.evaluation", msg -> {
            logger.info("Message received via EventBus: 'game.evaluation'");
            Object body = msg.body();
            JsonObject payload = body instanceof JsonObject
                    ? (JsonObject) body
                    : new JsonObject().put("message", String.valueOf(body));
            mqttService.publishGameEvaluation(payload);
        });

        this.eventBus.consumer("game.stop", msg -> {
            logger.info("Message received via EventBus: 'game.stop'");
            mqttService.publishGameStop();
        });

        this.eventBus.consumer("object.created", msg -> {
            logger.info("Message received via EventBus: 'object.created'");
            mqttService.publishObjectCreated(msg.body().toString());
        });
        this.eventBus.consumer("mqtt.demo.message", msg -> {
            logger.info("Message received via EventBus: 'mqtt.message'");
            mqttService.publishDemoMessage(msg.body().toString());
        });
        this.eventBus.consumer("controllers.updated", msg -> {
            logger.info("Message received via EventBus: 'controllers.updated'");
            mqttService.publishControllersUpdated();
        });

        this.eventBus.consumer("lobby.updated", msg -> {
            logger.info("Message received via EventBus: 'lobby.updated'");
            mqttService.publishLobbyUpdated();
        });
        this.eventBus.consumer("game.checkNoPlayersLeft", msg -> {
            gameService.checkNoPlayersLeftAndFinishIfNeeded(null);
        });
    }

    // --- MQTT -> Service-Handler ---
    public void registerMqttConsumers() {

        mqttClient.publishHandler(message -> {

            Buffer payload = message.payload();
            String topic = message.topicName();

            logger.info("Message received via Mqtt. Topic: {}, Payload: {}", topic, payload);

            if (topic.equals(mqttMessagePrefix + "demo/hello_world")) {
                this.eventBus.publish("mqtt.demo.message", payload);
            } else if (topic.equals(mqttMessagePrefix + "output")) {
                logger.info("Output message received: {}", payload.toString());
            } else if (topic.equals(mqttMessagePrefix + "controller/register")) {
                handleControllerRegister(payload);
            } else if (topic.equals(mqttMessagePrefix + "player/ready")) {
                handlePlayerReady(payload);
            } else if (topic.equals(mqttMessagePrefix + "controller/status/request")) {
                handleControllerStatusRequest(payload);
            } else if (topic.equals(mqttMessagePrefix + "game/answer")) {
                handleGameAnswer(payload);
            } else if (isRfidScanTopic(topic)) {
                handleRfidScan(topic, payload);
            } else if (isPongTopic(topic)) {
                handlePong(topic, payload);
            } else {
                logger.info("Handler for topic: {} not implemented", topic);
            }

        }
        );

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "demo/hello_world", 0,
                mqttMessagePrefix + "output", 0,
                mqttMessagePrefix + "controller/register", 0,
                mqttMessagePrefix + "player/ready", 0,
                mqttMessagePrefix + "controller/status/request", 0,
                mqttMessagePrefix + "game/answer", 0,
                mqttMessagePrefix + "controller/+/rfid/scan", 0,
                mqttMessagePrefix + "controller/+/pong", 0
        ));

    }

    // Verarbeitet controller/register.
    private void handleControllerRegister(Buffer payload) {
        JsonObject json;
        try {
            json = payload.toJsonObject();
        } catch (Exception e) {
            logger.warn("Invalid JSON payload for controller/register: {}", payload);
            return;
        }

        String controllerId = json.getString("controller_id");
        String controllerType = json.getString("controller_type");

        controllerService.registerController(controllerId, controllerType, ar -> {
            if (ar.succeeded()) {
                JsonObject controller = ar.result();
                logger.info("Controller registered via MQTT: {}", controller.encode());
                eventBus.publish("controllers.updated", controller.encode());
                publishControllerStatus(controllerId);
            } else {
                logger.warn("Failed to register controller via MQTT: {}", ar.cause().getMessage());
            }
        });
    }

    // Verarbeitet player/ready.
    private void handlePlayerReady(Buffer payload) {
        JsonObject json;
        try {
            json = payload.toJsonObject();
        } catch (Exception e) {
            logger.warn("Invalid JSON payload for player/ready: {}", payload);
            return;
        }

        String controllerId = json.getString("controller_id");
        Boolean ready = json.getBoolean("ready");

        // Ready-Status fuer den zugeordneten Spieler aktualisieren.
        lobbyService.setReadyByControllerId(controllerId, ready, ar -> {
            if (ar.succeeded()) {
                logger.info("Player ready state updated via MQTT: {}", ar.result().encode());
                publishControllerStatus(controllerId);
                eventBus.publish("lobby.updated", ar.result().encode());

                // Bei "Not Ready" waehrend COUNTDOWN wird der Countdown abgebrochen.
                if (Boolean.FALSE.equals(ready)) {
                    gameService.handleNotReadyDuringCountdown(notReadyAr -> {
                        if (notReadyAr.succeeded()) {
                            logger.info("Countdown aborted due to Not Ready during countdown");
                        } else {
                            logger.warn("Failed to abort countdown on Not Ready: {}", notReadyAr.cause().getMessage());
                        }
                    });
                }
            } else {
                logger.warn("Failed to update ready state via MQTT: {}", ar.cause().getMessage());
            }
        });
    }

    // Verarbeitet controller/status/request.
    private void handleControllerStatusRequest(Buffer payload) {
        String controllerId = null;
        try {
            JsonObject json = payload.toJsonObject();
            String controllerIdFromPayload = json.getString("controller_id");
            if (controllerIdFromPayload != null && !controllerIdFromPayload.trim().isEmpty()) {
                controllerId = controllerIdFromPayload.trim();
            }
        } catch (Exception e) {
            logger.warn("Invalid JSON payload for controller/status/request: {}", payload);
            return;
        }

        if (controllerId == null || controllerId.isBlank()) {
            logger.warn("Missing controller_id for controller/status/request");
            return;
        }

        final String cid = controllerId;
        controllerService.touchLastSeen(cid, ar -> publishControllerStatus(cid));
    }

    // Sendet den aktuellen Controller-Status auf das Response-Topic.
    private void publishControllerStatus(String controllerId) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            return;
        }

        String controllerIdFinal = controllerId.trim();
        lobbyService.getControllerRealtimeStatus(controllerIdFinal, ar -> {
            String responseTopic = mqttMessagePrefix + "controller/" + controllerIdFinal + "/status/response";
            if (ar.succeeded()) {
                mqttService.publishJson(responseTopic, ar.result());
            } else {
                JsonObject error = new JsonObject()
                        .put("controller_id", controllerIdFinal)
                        .put("error", ar.cause().getMessage());
                mqttService.publishJson(responseTopic, error);
                logger.warn("Failed to publish controller status: {}", ar.cause().getMessage());
            }
        });
    }

    // Topic-Muster: controller/{id}/pong
    private boolean isPongTopic(String topic) {
        String prefix = mqttMessagePrefix + "controller/";
        String suffix = "/pong";
        return topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length();
    }

    // Topic-Muster: controller/{id}/rfid/scan
    private boolean isRfidScanTopic(String topic) {
        String prefix = mqttMessagePrefix + "controller/";
        String suffix = "/rfid/scan";
        return topic.startsWith(prefix) && topic.endsWith(suffix) && topic.length() > prefix.length() + suffix.length();
    }

    // Verarbeitet eingehende Pongs fuer den Heartbeat.
    private void handlePong(String topic, Buffer payload) {
        String prefix = mqttMessagePrefix + "controller/";
        String suffix = "/pong";
        String controllerId = topic.substring(prefix.length(), topic.length() - suffix.length());
        if (controllerId != null && !controllerId.isBlank()) {
            logger.debug("Pong received from controller {}", controllerId);
            heartbeatManager.onPongReceived(controllerId);
        }
    }

    // Verarbeitet RFID-Scans vom Hardware-Controller.
    private void handleRfidScan(String topic, Buffer payload) {
        String prefix = mqttMessagePrefix + "controller/";
        String suffix = "/rfid/scan";
        String controllerId = topic.substring(prefix.length(), topic.length() - suffix.length());
        if (controllerId == null || controllerId.isBlank()) {
            return;
        }
        String controllerIdFinal = controllerId.trim();

        JsonObject json;
        try {
            json = payload.toJsonObject();
        } catch (Exception e) {
            logger.warn("Invalid JSON payload for RFID scan: {}", payload);
            publishRfidResult(controllerIdFinal, false, "Invalid RFID payload", null, null);
            return;
        }

        String rfidUid = json.getString("rfid_uid");
        if (rfidUid == null || rfidUid.trim().isEmpty()) {
            publishRfidResult(controllerIdFinal, false, "RFID UID is required", null, null);
            return;
        }

        lobbyService.joinSessionByRfid(controllerIdFinal, rfidUid.trim(), ar -> {
            if (ar.succeeded()) {
                JsonObject result = ar.result();
                Long sessionId = result.getLong("session_id");
                String playerName = result.getString("player_name");
                publishRfidResult(controllerIdFinal, true, "RFID login successful", playerName, sessionId);
                publishControllerStatus(controllerIdFinal);
                
                // Token fuer RFID-Autologin im Frontend senden.
                String token = result.getString("token");
                Long userId = result.getLong("user_id");
                String username = result.getString("username");
                if (token != null && userId != null) {
                    publishRfidAuthToken(userId, username, playerName, token);
                }
                
                eventBus.publish("controllers.updated", new JsonObject()
                        .put("controller_id", controllerIdFinal)
                        .put("event", "controllers.updated"));
                eventBus.publish("lobby.updated", new JsonObject()
                        .put("controller_id", controllerIdFinal)
                        .put("event", "lobby.updated"));
            } else {
                publishRfidResult(controllerIdFinal, false, ar.cause().getMessage(), null, null);
                publishControllerStatus(controllerIdFinal);
            }
        });
    }

    // Sendet das RFID-Ergebnis an den konkreten Controller.
    private void publishRfidResult(String controllerId, boolean success, String message, String playerName, Long sessionId) {
        String topic = mqttMessagePrefix + "controller/" + controllerId + "/rfid/result";
        JsonObject payload = new JsonObject()
                .put("controller_id", controllerId)
                .put("success", success)
                .put("message", message)
                .put("timestamp", System.currentTimeMillis());
        if (playerName != null) {
            payload.put("player_name", playerName);
        }
        if (sessionId != null) {
            payload.put("session_id", sessionId);
        }
        mqttService.publishJson(topic, payload);
    }

    // Sendet den RFID-Login-Token ans Frontend fuer Auto-Login.
    private void publishRfidAuthToken(Long userId, String username, String displayName, String token) {
        String topic = mqttMessagePrefix + "auth/rfid-login";
        JsonObject payload = new JsonObject()
                .put("user_id", userId)
                .put("username", username)
                .put("display_name", displayName != null ? displayName : username)
                .put("token", token)
                .put("timestamp", System.currentTimeMillis());
        mqttService.publishJson(topic, payload);
        logger.info("📤 RFID auth token published for user: {}", username);
    }

    // Verarbeitet game/answer und gibt an den GameService weiter.
    private void handleGameAnswer(Buffer payload) {
        JsonObject json;
        try {
            json = payload.toJsonObject();
        } catch (Exception e) {
            logger.warn("Invalid JSON payload for game/answer: {}", payload);
            return;
        }

        gameService.submitAnswer(json, ar -> {
            if (ar.succeeded()) {
                logger.info("Game answer accepted: {}", ar.result().encode());
            } else {
                logger.warn("Game answer rejected: {}", ar.cause().getMessage());
            }
        });
    }
}
