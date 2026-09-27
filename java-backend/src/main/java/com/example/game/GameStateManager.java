package com.example.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

public class GameStateManager {

    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    private final List<String> activePlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeControllers = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> currentSequence = Collections.synchronizedList(new ArrayList<>());

    private static GameStateManager instance;
    private final Vertx vertx;
    private final EventBus eventBus;
    private Long countdownTimerId;
    private Long countdownSessionId;
    private Long questionTimerId;
    private Long questionSessionId;

    private GameStateManager(Vertx vertx) {
        this.vertx = vertx;
        this.eventBus = vertx.eventBus();
    }

    // --- Singleton ---
    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameStateManager(vertx);
        }
        return instance;
    }

    // --- Countdown-Handling ---
    public synchronized void startCountdown(Long sessionId, Runnable onFinished) {
        stopCountdown();
        countdownSessionId = sessionId;
        runCountdownTick(3, onFinished);
    }

    public synchronized void stopCountdown() {
        if (countdownTimerId != null) {
            vertx.cancelTimer(countdownTimerId);
            countdownTimerId = null;
        }
        countdownSessionId = null;
    }

    public synchronized boolean isCountdownRunningFor(Long sessionId) {
        return countdownTimerId != null && sessionId != null && sessionId.equals(countdownSessionId);
    }

    // --- Fragetimer-Handling ---
    public synchronized void startQuestionTimer(Long sessionId, int seconds, Consumer<Integer> onTick, Runnable onTimeout) {
        stopQuestionTimer();
        questionSessionId = sessionId;
        runQuestionTick(Math.max(1, seconds), onTick, onTimeout);
    }

    public synchronized void stopQuestionTimer() {
        if (questionTimerId != null) {
            vertx.cancelTimer(questionTimerId);
            questionTimerId = null;
        }
        questionSessionId = null;
    }

    public synchronized boolean isQuestionTimerRunningFor(Long sessionId) {
        return questionTimerId != null && sessionId != null && sessionId.equals(questionSessionId);
    }

    // Interner Tick fuer den Fragetimer.
    private void runQuestionTick(int secondsLeft, Consumer<Integer> onTick, Runnable onTimeout) {
        if (onTick != null) {
            onTick.accept(secondsLeft);
        }
        if (secondsLeft <= 1) {
            questionTimerId = null;
            questionSessionId = null;
            if (onTimeout != null) {
                onTimeout.run();
            }
            return;
        }
        questionTimerId = vertx.setTimer(1000, id -> runQuestionTick(secondsLeft - 1, onTick, onTimeout));
    }

    // Interner Tick fuer den Start-Countdown.
    private void runCountdownTick(int value, Runnable onFinished) {
        JsonObject payload = new JsonObject()
                .put("countdown", value)
                .put("session_id", countdownSessionId)
                .put("timestamp", System.currentTimeMillis());
        eventBus.publish("game.countdown", payload);
        logger.info("Countdown tick {} for session {}", value, countdownSessionId);

        if (value <= 1) {
            countdownTimerId = null;
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        countdownTimerId = vertx.setTimer(1000, id -> runCountdownTick(value - 1, onFinished));
    }

}
