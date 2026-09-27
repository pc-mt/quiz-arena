package com.example.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.example.player.PlayerService;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.sqlclient.Row;

public class GameService {

    private final GameRepository gameRepository;
    private final JWTAuth jwtAuth;
    private final GameStateManager gameStateManager;
    private final Vertx vertx;
    private final Map<Long, Long> currentQuestionBySession = new ConcurrentHashMap<>();
    private final Map<Long, Integer> currentQuestionIndexBySession = new ConcurrentHashMap<>();
    private final Map<Long, Integer> totalQuestionsBySession = new ConcurrentHashMap<>();

    public GameService(Vertx vertx) {
        this.gameRepository = new GameRepository();
        this.jwtAuth = PlayerService.getJwtAuth();
        this.vertx = vertx;
        this.gameStateManager = GameStateManager.getInstance(vertx);
    }

    // --- Lobby-Konfiguration ---
    // Liefert die Anzahl der Fragen pro Kategorie und Schwierigkeit.
    public void getCategoryQuestionCounts(Handler<AsyncResult<JsonArray>> resultHandler) {
        gameRepository.fetchCategoryDifficultyCounts()
                .onSuccess(rows -> {
                    Map<Long, int[]> byCategory = new LinkedHashMap<>();
                    for (Row row : rows) {
                        Long categoryId = row.getLong("category_id");
                        String diff = row.getString("difficulty");
                        int cnt = row.getInteger("cnt") != null ? row.getInteger("cnt") : 0;
                        byCategory.computeIfAbsent(categoryId, k -> new int[3]);
                        int[] counts = byCategory.get(categoryId);
                        if ("EASY".equalsIgnoreCase(diff)) counts[0] = cnt;
                        else if ("MEDIUM".equalsIgnoreCase(diff)) counts[1] = cnt;
                        else if ("HARD".equalsIgnoreCase(diff)) counts[2] = cnt;
                    }
                    JsonArray arr = new JsonArray();
                    byCategory.forEach((catId, counts) -> arr.add(new JsonObject()
                            .put("category_id", catId)
                            .put("easy", counts[0])
                            .put("medium", counts[1])
                            .put("hard", counts[2])));
                    resultHandler.handle(Future.succeededFuture(arr));
                })
                .onFailure(err -> resultHandler.handle(Future.failedFuture(err)));
    }

    public void configureGame(String authHeader, JsonObject body, Handler<AsyncResult<JsonObject>> resultHandler) {
        ParsedConfig config;
        try {
            config = parseConfig(body);
        } catch (ApiException apiError) {
            resultHandler.handle(Future.failedFuture(apiError));
            return;
        }

        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();

            gameRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }

                        Row sessionRow = rows.iterator().next();
                        Long sessionId = sessionRow.getLong("id");
                        String sessionState = sessionRow.getString("state");

                        // Host-Pruefung ist aktuell bewusst deaktiviert.
                        // Long hostUserId = sessionRow.getLong("host_user_id");
                        // if (hostUserId != null && !hostUserId.equals(userId)) {
                        //     resultHandler.handle(
                        //             Future.failedFuture(new ApiException(403, "Only host can configure the game")));
                        //     return;
                        // }

                        if (!"LOBBY".equalsIgnoreCase(sessionState)) {
                            resultHandler.handle(Future.failedFuture(new ApiException(409, "Game is already running")));
                            return;
                        }

                        gameRepository.countExistingCategories(config.categoryIds)
                                .compose(countRows -> {
                                    long existing = countRows.iterator().next().getLong("total");
                                    if (existing != config.categoryIds.size()) {
                                        return Future.failedFuture(new ApiException(400, "One or more categories are invalid"));
                                    }
                                    return gameRepository.countQuestionsByFilters(config.categoryIds, config.difficulties);
                                })
                                .compose(questionRows -> {
                                    long available = questionRows.iterator().next().getLong("total");
                                    if (available < config.questionsNeeded) {
                                        return Future.failedFuture(new ApiException(422, "Not enough questions for selected configuration"));
                                    }

                                    return gameRepository.updateSessionConfig(
                                            sessionId,
                                            config.roundLength,
                                            config.allowEasy,
                                            config.allowMedium,
                                            config.allowHard)
                                            .compose(updated -> gameRepository.clearSessionCategories(sessionId))
                                            .compose(cleared -> insertCategories(sessionId, config.categoryIds))
                                            .map(v -> new JsonObject()
                                                    .put("message", "Game configuration saved")
                                                    .put("session_id", sessionId)
                                                    .put("round_length", config.roundLength)
                                                    .put("questions_needed", config.questionsNeeded)
                                                    .put("questions_available", available)
                                                    .put("categories", new JsonArray(config.categoryIds))
                                                    .put("difficulties", new JsonArray(config.difficulties)));
                                })
                                .onSuccess(response -> resultHandler.handle(Future.succeededFuture(response)))
                                .onFailure(err -> {
                                    if (err instanceof ApiException) {
                                        resultHandler.handle(Future.failedFuture(err));
                                    } else {
                                        resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error")));
                                    }
                                });
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    public void startGame(String authHeader, Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();

            gameRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }

                        Row sessionRow = rows.iterator().next();
                        Long sessionId = sessionRow.getLong("id");
                        // Host-Pruefung ist aktuell bewusst deaktiviert.
                        // Long hostUserId = sessionRow.getLong("host_user_id");
                        // if (hostUserId != null && !hostUserId.equals(userId)) {
                        //     resultHandler
                        //             .handle(Future.failedFuture(new ApiException(403, "Only host can start the game")));
                        //     return;
                        // }
                        String sessionState = sessionRow.getString("state");
                        totalQuestionsBySession.put(sessionId,
                                roundLengthToQuestions(sessionRow.getString("round_length")));

                        if (!"LOBBY".equalsIgnoreCase(sessionState)) {
                            resultHandler.handle(Future.failedFuture(new ApiException(409, "Game is already running")));
                            return;
                        }

                        boolean allowEasy = sessionRow.getBoolean("allow_easy");
                        boolean allowMedium = sessionRow.getBoolean("allow_medium");
                        boolean allowHard = sessionRow.getBoolean("allow_hard");
                        if (!allowEasy && !allowMedium && !allowHard) {
                            resultHandler.handle(Future.failedFuture(new ApiException(400, "Game configuration is incomplete")));
                            return;
                        }

                        gameRepository.countSessionCategories(sessionId)
                                .compose(categoryRows -> {
                                    long totalCategories = categoryRows.iterator().next().getLong("total");
                                    if (totalCategories <= 0) {
                                        return Future.failedFuture(
                                                new ApiException(400, "Game configuration is incomplete"));
                                    }
                                    return gameRepository.countConnectedPlayers(sessionId);
                                })
                                .compose(connectedRows -> {
                                    long connected = connectedRows.iterator().next().getLong("total");
                                    if (connected <= 0) {
                                        return Future.failedFuture(new ApiException(400, "No connected players in session"));
                                    }
                                    return gameRepository.countNotReadyConnectedPlayers(sessionId);
                                })
                                .compose(notReadyRows -> {
                                    long notReady = notReadyRows.iterator().next().getLong("total");
                                    if (notReady > 0) {
                                        return Future.failedFuture(new ApiException(400, "Not all players are ready"));
                                    }
                                    return gameRepository.startSessionCountdown(sessionId);
                                })
                                .onSuccess(updateRows -> {
                                    if (updateRows.rowCount() == 0) {
                                        resultHandler.handle(
                                                Future.failedFuture(new ApiException(409, "Game is already running")));
                                        return;
                                    }

                                    resultHandler.handle(Future.succeededFuture(new JsonObject()
                                            .put("message", "Game started")
                                            .put("session_id", sessionId)
                                            .put("state", "COUNTDOWN")));

                                    vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                    gameStateManager.startCountdown(sessionId, () -> {
                                        gameRepository.setSessionStateIfCurrent(sessionId, "COUNTDOWN", "QUESTION")
                                                .onSuccess(update -> {
                                                    if (update.rowCount() > 0) {
                                                        vertx.eventBus().publish("game.state", new JsonObject()
                                                                .put("state", "QUESTION")
                                                                .put("session_id", sessionId)
                                                                .put("timestamp", System.currentTimeMillis()));
                                                        publishFirstQuestion(sessionId);
                                                    }
                                                });
                                    });
                                })
                                .onFailure(err -> {
                                    if (err instanceof ApiException) {
                                        resultHandler.handle(Future.failedFuture(err));
                                    } else {
                                        resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error")));
                                    }
                                });
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    public void abortCountdown(String authHeader, Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            gameRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }

                        Row sessionRow = rows.iterator().next();
                        Long sessionId = sessionRow.getLong("id");
                        String sessionState = sessionRow.getString("state");

                        if (!"COUNTDOWN".equalsIgnoreCase(sessionState)) {
                            resultHandler.handle(Future.failedFuture(new ApiException(409, "No countdown is running")));
                            return;
                        }

                        gameStateManager.stopCountdown();
                        gameStateManager.stopQuestionTimer();
                        currentQuestionBySession.remove(sessionId);
                        currentQuestionIndexBySession.remove(sessionId);
                        totalQuestionsBySession.remove(sessionId);
                        gameRepository.setSessionStateIfCurrent(sessionId, "COUNTDOWN", "LOBBY")
                                .onSuccess(update -> {
                                    vertx.eventBus().publish("game.state", new JsonObject()
                                            .put("state", "LOBBY")
                                            .put("session_id", sessionId)
                                            .put("timestamp", System.currentTimeMillis()));
                                    resultHandler.handle(Future.succeededFuture(new JsonObject()
                                            .put("message", "Countdown aborted")
                                            .put("session_id", sessionId)
                                            .put("state", "LOBBY")));
                                })
                                .onFailure(err -> resultHandler
                                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    private Future<Void> insertCategories(Long sessionId, List<Long> categoryIds) {
        Future<Void> chain = Future.succeededFuture();
        for (Long categoryId : categoryIds) {
            chain = chain.compose(v -> gameRepository.insertSessionCategory(sessionId, categoryId).mapEmpty());
        }
        return chain;
    }

    // --- Laufendes Spiel ---
    // Wenn kein verbundener Spieler mehr uebrig ist, wird die laufende Frage direkt beendet.
    public void checkNoPlayersLeftAndFinishIfNeeded(Handler<AsyncResult<Void>> resultHandler) {
        gameRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                        return;
                    }
                    Row row = rows.iterator().next();
                    String state = row.getString("state");
                    if (!"QUESTION".equalsIgnoreCase(state)) {
                        if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                        return;
                    }
                    Long sessionId = row.getLong("id");
                    gameRepository.countConnectedPlayers(sessionId)
                            .onSuccess(connectedRows -> {
                                long connected = connectedRows.iterator().next().getLong("total");
                                if (connected == 0) {
                                    finishCurrentQuestion(sessionId, "no_players");
                                }
                                if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                            })
                            .onFailure(err -> {
                                if (resultHandler != null) resultHandler.handle(Future.failedFuture(err));
                            });
                })
                .onFailure(err -> {
                    if (resultHandler != null) resultHandler.handle(Future.failedFuture(err));
                });
    }

    private void publishFirstQuestion(Long sessionId) {
        gameRepository.countConnectedPlayers(sessionId)
                .onSuccess(connectedRows -> {
                    long connected = connectedRows.iterator().next().getLong("total");
                    if (connected == 0) {
                        endGame(sessionId);
                        return;
                    }
                    gameRepository.fetchFirstQuestionForSession(sessionId)
                            .onSuccess(questionRows -> {
                                if (!questionRows.iterator().hasNext()) {
                                    endGame(sessionId);
                                    return;
                                }
                                Row firstRow = questionRows.iterator().next();
                                Long questionId = firstRow.getLong("id");
                                gameRepository.insertSessionQuestion(sessionId, 1, questionId)
                                        .onSuccess(insert -> publishQuestion(sessionId, 1, firstRow))
                                        .onFailure(ignore -> endGame(sessionId));
                            })
                            .onFailure(ignore -> endGame(sessionId));
                })
                .onFailure(ignore -> endGame(sessionId));
    }

    public void submitAnswer(JsonObject payload, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (payload == null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Answer payload is required")));
            return;
        }

        Long sessionId = toLong(payload.getValue("session_id"));
        String controllerId = payload.getString("controller_id");
        String answer = payload.getString("answer");
        Long questionIdFromPayload = toLong(payload.getValue("question_id"));
        Long botResponseTimeMs = toLong(payload.getValue("response_time_ms"));
        boolean isBot = controllerId != null && controllerId.toUpperCase().startsWith("BOT-");

        if (sessionId == null || sessionId <= 0) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "session_id is required")));
            return;
        }
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "controller_id is required")));
            return;
        }
        String normalizedAnswer = answer != null ? answer.trim().toUpperCase() : "";
        if (!"A".equals(normalizedAnswer) && !"B".equals(normalizedAnswer)
                && !"C".equals(normalizedAnswer) && !"D".equals(normalizedAnswer)) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "answer must be A, B, C or D")));
            return;
        }

        gameRepository.fetchActiveSession()
                .compose(sessionRows -> {
                    if (!sessionRows.iterator().hasNext()) {
                        return Future.failedFuture(new ApiException(404, "No active session"));
                    }
                    Row sessionRow = sessionRows.iterator().next();
                    Long activeSessionId = sessionRow.getLong("id");
                    String state = sessionRow.getString("state");
                    if (!sessionId.equals(activeSessionId)) {
                        return Future.failedFuture(new ApiException(409, "Session mismatch"));
                    }
                    if (!"QUESTION".equalsIgnoreCase(state)) {
                        return Future.failedFuture(new ApiException(409, "Question is not active"));
                    }
                    return gameRepository.fetchCurrentSessionQuestion(sessionId);
                })
                .compose(questionRows -> {
                    if (!questionRows.iterator().hasNext()) {
                        return Future.failedFuture(new ApiException(409, "No active question"));
                    }
                    Row questionRow = questionRows.iterator().next();
                    Long currentQuestionId = questionRow.getLong("question_id");
                    if (questionIdFromPayload != null && !currentQuestionId.equals(questionIdFromPayload)) {
                        return Future.failedFuture(new ApiException(409, "Question mismatch"));
                    }
                    return gameRepository.fetchSessionPlayerByControllerExternalId(sessionId, controllerId.trim())
                            .compose(playerRows -> {
                                if (!playerRows.iterator().hasNext()) {
                                    return Future.failedFuture(new ApiException(404, "Player/controller not in session"));
                                }
                                Long userId = playerRows.iterator().next().getLong("user_id");
                                return gameRepository.hasAnswerForQuestion(sessionId, userId, currentQuestionId)
                                        .compose(answerRows -> {
                                            long existing = answerRows.iterator().next().getLong("total");
                                            if (existing > 0) {
                                                return Future.succeededFuture(new JsonObject()
                                                        .put("message", "Answer already submitted")
                                                        .put("session_id", sessionId)
                                                        .put("question_id", currentQuestionId)
                                                        .put("answer", normalizedAnswer));
                                            }
                                            Long finalBotTime = isBot ? botResponseTimeMs : null;
                                            return gameRepository.insertAnswer(sessionId, userId, currentQuestionId, normalizedAnswer, finalBotTime)
                                                    .compose(insert -> gameRepository.countAnswersForQuestion(sessionId, currentQuestionId))
                                                    .compose(countRows -> {
                                                        long totalAnswers = countRows.iterator().next().getLong("total");
                                                        return gameRepository.countConnectedPlayers(sessionId)
                                                                .map(connectedRows -> {
                                                                    long connectedPlayers = connectedRows.iterator().next().getLong("total");
                                                                    if (connectedPlayers == 0 || totalAnswers >= connectedPlayers) {
                                                                        finishCurrentQuestion(sessionId, connectedPlayers == 0 ? "no_players" : "all_answered");
                                                                    }
                                                                    return new JsonObject()
                                                                            .put("message", "Answer accepted")
                                                                            .put("session_id", sessionId)
                                                                            .put("question_id", currentQuestionId)
                                                                            .put("answer", normalizedAnswer)
                                                                            .put("answers_received", totalAnswers)
                                                                            .put("players_total", connectedPlayers);
                                                                });
                                                    });
                                        });
                            });
                })
                .onSuccess(response -> resultHandler.handle(Future.succeededFuture(response)))
                .onFailure(err -> {
                    if (err instanceof ApiException) {
                        resultHandler.handle(Future.failedFuture(err));
                    } else {
                        resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
                    }
                });
    }

    private void finishCurrentQuestion(Long sessionId, String reason) {
        gameRepository.setSessionStateIfCurrent(sessionId, "QUESTION", "EVALUATION")
                .onSuccess(updateRows -> {
                    if (updateRows.rowCount() == 0) {
                        return;
                    }
                    gameStateManager.stopQuestionTimer();
                    Long questionId = currentQuestionBySession.get(sessionId);
                    gameRepository.fetchCurrentSessionQuestion(sessionId)
                            .onSuccess(questionRows -> {
                                String correctOption = null;
                                Long effectiveQuestionId = questionId;
                                if (questionRows.iterator().hasNext()) {
                                    Row questionRow = questionRows.iterator().next();
                                    effectiveQuestionId = questionRow.getLong("question_id");
                                    correctOption = questionRow.getString("correct_option");
                                }
                                vertx.eventBus().publish("game.state", new JsonObject()
                                        .put("state", "EVALUATION")
                                        .put("session_id", sessionId)
                                        .put("timestamp", System.currentTimeMillis()));
                                Long finalQuestionId = effectiveQuestionId;
                                String finalCorrectOption = correctOption;

                                gameRepository.fetchQuestionOptions(finalQuestionId)
                                        .onSuccess(optionRows -> {
                                            String correctAnswerText = null;
                                            if (finalCorrectOption != null) {
                                                for (Row optionRow : optionRows) {
                                                    String letter = optionRow.getString("option_letter");
                                                    String text = optionRow.getString("option_text");
                                                    if (letter != null && letter.equalsIgnoreCase(finalCorrectOption) && text != null) {
                                                        correctAnswerText = text;
                                                        break;
                                                    }
                                                }
                                            }
                                            final String finalCorrectAnswerText = correctAnswerText;

                                            gameRepository.fetchEvaluationRows(sessionId, finalQuestionId)
                                                    .onSuccess(rows -> {
                                                        JsonArray results = new JsonArray();
                                                        for (Row row : rows) {
                                                            Number isCorrectRaw = row.getValue("is_correct") instanceof Number
                                                                    ? (Number) row.getValue("is_correct")
                                                                    : 0;
                                                            Number pointsRaw = row.getValue("points_awarded") instanceof Number
                                                                    ? (Number) row.getValue("points_awarded")
                                                                    : 0;
                                                            Number responseTimeRaw = row.getValue("response_time_ms") instanceof Number
                                                                    ? (Number) row.getValue("response_time_ms")
                                                                    : null;
                                                            Number basePointsRaw = row.getValue("base_points") instanceof Number
                                                                    ? (Number) row.getValue("base_points")
                                                                    : null;
                                                            Number factorRaw = row.getValue("time_factor") instanceof Number
                                                                    ? (Number) row.getValue("time_factor")
                                                                    : null;
                                                            results.add(new JsonObject()
                                                                    .put("user_id", row.getLong("user_id"))
                                                                    .put("player_name", row.getString("player_name"))
                                                                    .put("answered_option", row.getString("answered_option"))
                                                                    .put("is_correct", isCorrectRaw.intValue() == 1)
                                                                    .put("points_awarded", pointsRaw.doubleValue())
                                                                    .put("response_time_ms",
                                                                            responseTimeRaw != null ? responseTimeRaw.longValue()
                                                                                    : null)
                                                                    .put("time_bucket", row.getString("time_bucket"))
                                                                    .put("base_points", basePointsRaw != null ? basePointsRaw.intValue() : null)
                                                                    .put("time_factor", factorRaw != null ? factorRaw.doubleValue() : null));
                                                        }
                                                        vertx.eventBus().publish("game.evaluation", new JsonObject()
                                                                .put("session_id", sessionId)
                                                                .put("question_id", finalQuestionId)
                                                                .put("correct_option", finalCorrectOption)
                                                                .put("correct_answer_text", finalCorrectAnswerText)
                                                                .put("correct_answer_full",
                                                                        finalCorrectOption != null && finalCorrectAnswerText != null
                                                                                ? finalCorrectOption + " - " + finalCorrectAnswerText
                                                                                : finalCorrectOption)
                                                                .put("reason", reason)
                                                                .put("results", results)
                                                                .put("timestamp", System.currentTimeMillis()));
                                                        vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                                        vertx.setTimer(3000, id -> advanceAfterEvaluation(sessionId));
                                                    })
                                                    .onFailure(err -> {
                                                        vertx.eventBus().publish("game.evaluation", new JsonObject()
                                                                .put("session_id", sessionId)
                                                                .put("question_id", finalQuestionId)
                                                                .put("correct_option", finalCorrectOption)
                                                                .put("correct_answer_text", finalCorrectAnswerText)
                                                                .put("correct_answer_full",
                                                                        finalCorrectOption != null && finalCorrectAnswerText != null
                                                                                ? finalCorrectOption + " - " + finalCorrectAnswerText
                                                                                : finalCorrectOption)
                                                                .put("reason", reason)
                                                                .put("results", new JsonArray())
                                                                .put("timestamp", System.currentTimeMillis()));
                                                        vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                                        vertx.setTimer(3000, id -> advanceAfterEvaluation(sessionId));
                                                    });
                                        })
                                        .onFailure(err -> {
                                            // Fallback, falls kein Antworttext geladen wurde.
                                            gameRepository.fetchEvaluationRows(sessionId, finalQuestionId)
                                                    .onSuccess(rows -> {
                                                        JsonArray results = new JsonArray();
                                                        for (Row row : rows) {
                                                            Number isCorrectRaw = row.getValue("is_correct") instanceof Number
                                                                    ? (Number) row.getValue("is_correct")
                                                                    : 0;
                                                            Number pointsRaw = row.getValue("points_awarded") instanceof Number
                                                                    ? (Number) row.getValue("points_awarded")
                                                                    : 0;
                                                            Number responseTimeRaw = row.getValue("response_time_ms") instanceof Number
                                                                    ? (Number) row.getValue("response_time_ms")
                                                                    : null;
                                                            Number basePointsRaw = row.getValue("base_points") instanceof Number
                                                                    ? (Number) row.getValue("base_points")
                                                                    : null;
                                                            Number factorRaw = row.getValue("time_factor") instanceof Number
                                                                    ? (Number) row.getValue("time_factor")
                                                                    : null;
                                                            results.add(new JsonObject()
                                                                    .put("user_id", row.getLong("user_id"))
                                                                    .put("player_name", row.getString("player_name"))
                                                                    .put("answered_option", row.getString("answered_option"))
                                                                    .put("is_correct", isCorrectRaw.intValue() == 1)
                                                                    .put("points_awarded", pointsRaw.doubleValue())
                                                                    .put("response_time_ms",
                                                                            responseTimeRaw != null ? responseTimeRaw.longValue()
                                                                                    : null)
                                                                    .put("time_bucket", row.getString("time_bucket"))
                                                                    .put("base_points", basePointsRaw != null ? basePointsRaw.intValue() : null)
                                                                    .put("time_factor", factorRaw != null ? factorRaw.doubleValue() : null));
                                                        }
                                                        vertx.eventBus().publish("game.evaluation", new JsonObject()
                                                                .put("session_id", sessionId)
                                                                .put("question_id", finalQuestionId)
                                                                .put("correct_option", finalCorrectOption)
                                                                .put("reason", reason)
                                                                .put("results", results)
                                                                .put("timestamp", System.currentTimeMillis()));
                                                        vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                                        vertx.setTimer(3000, id -> advanceAfterEvaluation(sessionId));
                                                    })
                                                    .onFailure(innerErr -> {
                                                        vertx.eventBus().publish("game.evaluation", new JsonObject()
                                                                .put("session_id", sessionId)
                                                                .put("question_id", finalQuestionId)
                                                                .put("correct_option", finalCorrectOption)
                                                                .put("reason", reason)
                                                                .put("results", new JsonArray())
                                                                .put("timestamp", System.currentTimeMillis()));
                                                        vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                                        vertx.setTimer(3000, id -> advanceAfterEvaluation(sessionId));
                                                    });
                                        });
                            })
                            .onFailure(ignore -> {
                                vertx.eventBus().publish("game.state", new JsonObject()
                                        .put("state", "EVALUATION")
                                        .put("session_id", sessionId)
                                        .put("timestamp", System.currentTimeMillis()));
                                vertx.eventBus().send("heartbeat.preQuestionPing", new JsonObject().put("session_id", sessionId));
                                vertx.setTimer(3000, id -> advanceAfterEvaluation(sessionId));
                            });
                });
    }

    private void advanceAfterEvaluation(Long sessionId) {
        gameRepository.countSessionQuestions(sessionId)
                .compose(countRows -> {
                    long asked = countRows.iterator().next().getLong("total");
                    return gameRepository.fetchSessionRoundLength(sessionId)
                            .map(roundRows -> {
                                if (!roundRows.iterator().hasNext()) return 0;
                                String roundLength = roundRows.iterator().next().getString("round_length");
                                int needed = roundLengthToQuestions(roundLength);
                                return asked >= needed ? 1 : 0;
                            })
                            .compose(doneFlag -> {
                                if (doneFlag == 1) {
                                    endGame(sessionId);
                                    return Future.succeededFuture();
                                }

                                int nextIndex = (int) asked + 1;
                                return gameRepository.countConnectedPlayers(sessionId)
                                        .compose(connectedRows -> {
                                            long connected = connectedRows.iterator().next().getLong("total");
                                            if (connected == 0) {
                                                endGame(sessionId);
                                                return Future.succeededFuture();
                                            }
                                            return gameRepository.fetchNextQuestionForSession(sessionId)
                                                    .compose(nextRows -> {
                                                        if (!nextRows.iterator().hasNext()) {
                                                            endGame(sessionId);
                                                            return Future.succeededFuture();
                                                        }
                                                        Row nextQuestion = nextRows.iterator().next();
                                                        Long questionId = nextQuestion.getLong("id");
                                                        return gameRepository.insertSessionQuestion(sessionId, nextIndex, questionId)
                                                                .compose(insert -> gameRepository.setSessionStateIfCurrent(sessionId, "EVALUATION", "QUESTION"))
                                                                .onSuccess(update -> {
                                                                    if (update.rowCount() > 0) {
                                                                        vertx.eventBus().publish("game.state", new JsonObject()
                                                                                .put("state", "QUESTION")
                                                                                .put("session_id", sessionId)
                                                                                .put("timestamp", System.currentTimeMillis()));
                                                                        publishQuestion(sessionId, nextIndex, nextQuestion);
                                                                    }
                                                                })
                                                                .mapEmpty();
                                                    });
                                        });
                            });
                })
                .onFailure(ignore -> endGame(sessionId));
    }

    private void publishQuestion(Long sessionId, int questionIndex, Row questionRow) {
        Long questionId = questionRow.getLong("id");
        String questionText = questionRow.getString("question_text");
        String categoryName = questionRow.getString("category_name");
        String difficulty = questionRow.getString("difficulty");
        currentQuestionBySession.put(sessionId, questionId);
        currentQuestionIndexBySession.put(sessionId, questionIndex);

        gameRepository.fetchQuestionOptions(questionId)
                .onSuccess(optionRows -> {
                    JsonObject answers = new JsonObject()
                            .put("A", "—")
                            .put("B", "—")
                            .put("C", "—")
                            .put("D", "—");
                    for (Row optionRow : optionRows) {
                        String letter = optionRow.getString("option_letter");
                        String text = optionRow.getString("option_text");
                        if (letter != null && text != null) {
                            answers.put(letter, text);
                        }
                    }

                    vertx.eventBus().publish("game.question", new JsonObject()
                            .put("session_id", sessionId)
                            .put("question_index", questionIndex)
                            .put("total_questions", totalQuestionsBySession.getOrDefault(sessionId, 5))
                            .put("question_id", questionId)
                            .put("text", questionText)
                            .put("category", categoryName)
                            .put("difficulty", difficulty)
                            .put("duration_sec", 30)
                            .put("answers", answers));
                    gameStateManager.startQuestionTimer(
                            sessionId,
                            30,
                            secondsLeft -> vertx.eventBus().publish("game.question.timer", new JsonObject()
                                    .put("session_id", sessionId)
                                    .put("question_id", questionId)
                                    .put("seconds_left", secondsLeft)
                                    .put("timestamp", System.currentTimeMillis())),
                            () -> finishCurrentQuestion(sessionId, "timeout"));
                })
                .onFailure(ignore -> endGame(sessionId));
    }

    private void endGame(Long sessionId) {
        gameStateManager.stopQuestionTimer();
        currentQuestionBySession.remove(sessionId);
        currentQuestionIndexBySession.remove(sessionId);
        totalQuestionsBySession.remove(sessionId);

        gameRepository.setSessionState(sessionId, "RESULTS")
                .compose(v -> gameRepository.fetchFinalResults(sessionId))
                .onSuccess(rows -> {
                    JsonArray results = new JsonArray();
                    for (Row row : rows) {
                        Number pts = row.getValue("total_points") instanceof Number
                                ? (Number) row.getValue("total_points") : 0;
                        Number time = row.getValue("total_response_time_ms") instanceof Number
                                ? (Number) row.getValue("total_response_time_ms") : 0;
                        Number correct = row.getValue("correct_count") instanceof Number
                                ? (Number) row.getValue("correct_count") : 0;
                        Number answered = row.getValue("answered_count") instanceof Number
                                ? (Number) row.getValue("answered_count") : 0;
                        results.add(new JsonObject()
                                .put("rank", results.size() + 1)
                                .put("user_id", row.getLong("user_id"))
                                .put("player_name", row.getString("player_name"))
                                .put("total_points", pts.doubleValue())
                                .put("correct_count", correct.intValue())
                                .put("answered_count", answered.intValue())
                                .put("total_response_time_ms", time.longValue()));
                    }

                    vertx.eventBus().publish("game.state", new JsonObject()
                            .put("state", "RESULTS")
                            .put("session_id", sessionId)
                            .put("results", results)
                            .put("timestamp", System.currentTimeMillis()));

                    gameRepository.fetchSessionRoundLength(sessionId).onSuccess(roundRows -> {
                        String rl = "Q5";
                        if (roundRows.iterator().hasNext()) {
                            rl = roundRows.iterator().next().getString("round_length");
                        }
                        final String roundLength = rl;
                        for (int i = 0; i < results.size(); i++) {
                            JsonObject r = results.getJsonObject(i);
                            Long uid = r.getLong("user_id");
                            double tp = r.getDouble("total_points");
                            long tt = r.getLong("total_response_time_ms");
                            int cc = r.getInteger("correct_count");
                            int ac = r.getInteger("answered_count");
                            String playerName = r.getString("player_name");
                            gameRepository.insertSessionResult(sessionId, uid, tp, tt, cc, ac);

                            // Bots kommen nicht in die Highscore-Tabelle.
                            if (playerName != null && playerName.toLowerCase().startsWith("bot")) {
                                continue;
                            }

                            // Jeder Lauf wird als eigener Highscore-Eintrag gespeichert.
                            gameRepository.insertHighscore(roundLength, uid, sessionId, tp, tt);
                        }
                    });
                })
                .onFailure(err -> vertx.eventBus().publish("game.state", new JsonObject()
                        .put("state", "RESULTS")
                        .put("session_id", sessionId)
                        .put("results", new JsonArray())
                        .put("timestamp", System.currentTimeMillis())));
    }

    public void restartGame(String authHeader, Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, authResult -> {
            if (authResult.failed()) {
                resultHandler.handle(Future.failedFuture(authResult.cause()));
                return;
            }

            gameRepository.fetchActiveSession().onSuccess(rows -> {
                if (!rows.iterator().hasNext()) {
                    resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                    return;
                }
                var session = rows.iterator().next();
                String state = session.getString("state");
                Long sessionId = session.getLong("id");

                boolean allowed = "RESULTS".equalsIgnoreCase(state)
                        || "COUNTDOWN".equalsIgnoreCase(state)
                        || "QUESTION".equalsIgnoreCase(state)
                        || "EVALUATION".equalsIgnoreCase(state);
                if (!allowed) {
                    resultHandler.handle(Future.failedFuture(
                            new ApiException(409, "Session is not in RESULTS, COUNTDOWN, QUESTION or EVALUATION state")));
                    return;
                }

                // Bei Abbruch aus COUNTDOWN/QUESTION/EVALUATION erst Timer und Session-Cache aufraeumen.
                if ("COUNTDOWN".equalsIgnoreCase(state) || "QUESTION".equalsIgnoreCase(state)
                        || "EVALUATION".equalsIgnoreCase(state)) {
                    gameStateManager.stopCountdown();
                    gameStateManager.stopQuestionTimer();
                    currentQuestionBySession.remove(sessionId);
                    currentQuestionIndexBySession.remove(sessionId);
                    totalQuestionsBySession.remove(sessionId);
                }

                gameRepository.setSessionState(sessionId, "LOBBY")
                        .compose(v -> gameRepository.resetAllPlayersReady(sessionId))
                        .compose(v -> gameRepository.deleteGameAnswersForSession(sessionId))
                        .compose(v -> gameRepository.deleteGameSessionQuestionsForSession(sessionId))
                        .onSuccess(v -> {
                            vertx.eventBus().publish("game.state", new JsonObject()
                                    .put("state", "LOBBY")
                                    .put("session_id", sessionId)
                                    .put("timestamp", System.currentTimeMillis()));
                            vertx.eventBus().publish("lobby.updated", new JsonObject()
                                    .put("reason", "restart"));

                            resultHandler.handle(Future.succeededFuture(new JsonObject()
                                    .put("message", "Game restarted")
                                    .put("session_id", sessionId)));
                        })
                        .onFailure(err -> resultHandler.handle(
                                Future.failedFuture(new ApiException(500, "Internal server error"))));
            }).onFailure(err -> resultHandler.handle(
                    Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    /**
     * Wird aufgerufen, wenn waehrend des Countdowns "Not Ready" gesendet wurde.
     * Der Countdown wird gestoppt und die Session geht sauber zurueck in die Lobby.
     */
    public void handleNotReadyDuringCountdown(Handler<AsyncResult<Void>> resultHandler) {
        gameRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                        return;
                    }
                    Row session = rows.iterator().next();
                    String state = session.getString("state");
                    Long sessionId = session.getLong("id");
                    if (!"COUNTDOWN".equalsIgnoreCase(state)) {
                        if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                        return;
                    }

                    gameStateManager.stopCountdown();
                    gameStateManager.stopQuestionTimer();
                    currentQuestionBySession.remove(sessionId);
                    currentQuestionIndexBySession.remove(sessionId);
                    totalQuestionsBySession.remove(sessionId);

                    gameRepository.setSessionStateIfCurrent(sessionId, "COUNTDOWN", "LOBBY")
                            .compose(v -> gameRepository.resetAllPlayersReady(sessionId))
                            .compose(v -> gameRepository.deleteGameAnswersForSession(sessionId))
                            .compose(v -> gameRepository.deleteGameSessionQuestionsForSession(sessionId))
                            .onSuccess(v -> {
                                vertx.eventBus().publish("game.state", new JsonObject()
                                        .put("state", "LOBBY")
                                        .put("session_id", sessionId)
                                        .put("timestamp", System.currentTimeMillis()));
                                vertx.eventBus().publish("lobby.updated", new JsonObject()
                                        .put("reason", "restart"));
                                if (resultHandler != null) resultHandler.handle(Future.succeededFuture());
                            })
                            .onFailure(err -> {
                                if (resultHandler != null) resultHandler.handle(Future.failedFuture(err));
                            });
                })
                .onFailure(err -> {
                    if (resultHandler != null) resultHandler.handle(Future.failedFuture(err));
                });
    }

    private int roundLengthToQuestions(String roundLength) {
        if ("Q20".equalsIgnoreCase(roundLength)) return 20;
        if ("Q10".equalsIgnoreCase(roundLength)) return 10;
        return 5;
    }

    private ParsedConfig parseConfig(JsonObject body) {
        if (body == null) {
            throw new ApiException(400, "Request body is required");
        }

        String roundLength = parseRoundLength(body.getValue("mode"));
        if (roundLength == null) {
            throw new ApiException(400, "Mode must be 5, 10 or 20");
        }

        JsonArray categoriesArray = body.getJsonArray("categories");
        if (categoriesArray == null || categoriesArray.isEmpty()) {
            throw new ApiException(400, "Categories are required");
        }

        Set<Long> categoryIdSet = new LinkedHashSet<>();
        for (Object categoryObj : categoriesArray) {
            Long categoryId = toLong(categoryObj);
            if (categoryId == null || categoryId <= 0) {
                throw new ApiException(400, "Categories must contain valid numeric IDs");
            }
            categoryIdSet.add(categoryId);
        }
        if (categoryIdSet.isEmpty()) {
            throw new ApiException(400, "Categories are required");
        }

        JsonArray difficultiesArray = body.getJsonArray("difficulties");
        if (difficultiesArray == null || difficultiesArray.isEmpty()) {
            throw new ApiException(400, "At least one difficulty is required");
        }

        Set<String> difficultySet = new LinkedHashSet<>();
        for (Object difficultyObj : difficultiesArray) {
            if (!(difficultyObj instanceof String)) {
                throw new ApiException(400, "Difficulties must be strings");
            }
            String difficulty = ((String) difficultyObj).trim().toUpperCase();
            if (!"EASY".equals(difficulty) && !"MEDIUM".equals(difficulty) && !"HARD".equals(difficulty)) {
                throw new ApiException(400, "Difficulty must be EASY, MEDIUM or HARD");
            }
            difficultySet.add(difficulty);
        }
        if (difficultySet.isEmpty()) {
            throw new ApiException(400, "At least one difficulty is required");
        }

        ParsedConfig config = new ParsedConfig();
        config.roundLength = roundLength;
        config.questionsNeeded = switch (roundLength) {
            case "Q5" -> 5;
            case "Q10" -> 10;
            default -> 20;
        };
        config.categoryIds = new ArrayList<>(categoryIdSet);
        config.difficulties = new ArrayList<>(difficultySet);
        config.allowEasy = difficultySet.contains("EASY");
        config.allowMedium = difficultySet.contains("MEDIUM");
        config.allowHard = difficultySet.contains("HARD");
        return config;
    }

    private String parseRoundLength(Object modeObj) {
        if (modeObj == null) return null;
        String value = String.valueOf(modeObj).trim().toUpperCase();
        return switch (value) {
            case "5", "Q5" -> "Q5";
            case "10", "Q10" -> "Q10";
            case "20", "Q20" -> "Q20";
            default -> null;
        };
    }

    private Long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignore) {
                return null;
            }
        }
        return null;
    }

    private void getUserIdFromAuthHeader(String authHeader, Handler<AsyncResult<Long>> resultHandler) {
        if (authHeader == null || authHeader.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required")));
            return;
        }

        String header = authHeader.trim();
        if (!header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required")));
            return;
        }

        String token = header.substring(7).trim().replaceAll("^\"+|\"+$", "");
        if (token.isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required")));
            return;
        }

        jwtAuth.authenticate(new JsonObject().put("token", token))
                .onSuccess(user -> {
                    String userIdStr = user.principal().getString("sub");
                    if (userIdStr == null) {
                        resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: invalid token")));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture(Long.parseLong(userIdStr)));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required"))));
    }

    public void getCorrectAnswerForBot(Long questionId, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (questionId == null || questionId <= 0) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid question ID")));
            return;
        }

        gameRepository.fetchCorrectOptionForQuestion(questionId)
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.failedFuture(new ApiException(404, "Question not found")));
                        return;
                    }
                    String correctOption = rows.iterator().next().getString("correct_option");
                    resultHandler.handle(Future.succeededFuture(
                            new JsonObject()
                                    .put("question_id", questionId)
                                    .put("correct_option", correctOption)
                    ));
                })
                .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Database error"))));
    }

    private static class ParsedConfig {
        String roundLength;
        int questionsNeeded;
        List<Long> categoryIds;
        List<String> difficulties;
        boolean allowEasy;
        boolean allowMedium;
        boolean allowHard;
    }

    public static class ApiException extends RuntimeException {
        private final int status;

        public ApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
