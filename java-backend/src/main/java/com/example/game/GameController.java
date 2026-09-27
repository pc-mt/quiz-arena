package com.example.game;

import com.example.http.HttpController;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.core.Vertx;

public class GameController implements HttpController {

    private final GameService gameService;
    private final Vertx vertx;

    public GameController(Vertx vertx) {
        this.vertx = vertx;
        this.gameService = new GameService(vertx);
    }

    @Override
    public void registerRoutes(Router router) {
        router.route("/api/game/*").handler(BodyHandler.create());
        // --- Game-Routen ---
        router.get("/api/game/category-question-counts").handler(this::handleCategoryQuestionCounts);
        router.post("/api/game/config").handler(this::handleConfig);
        router.post("/api/game/start").handler(this::handleStart);
        router.post("/api/game/countdown/abort").handler(this::handleAbortCountdown);
        router.post("/api/game/restart").handler(this::handleRestart);
        router.get("/api/game/bot/answer/:questionId").handler(this::handleBotAnswer);
    }

    // Frageanzahlen pro Kategorie/Schwierigkeit fuer die Lobby laden
    private void handleCategoryQuestionCounts(RoutingContext ctx) {
        gameService.getCategoryQuestionCounts(ar -> {
            if (ar.succeeded()) {
                JsonArray counts = ar.result();
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("counts", counts).encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Spielkonfiguration speichern (Mode, Kategorien, Schwierigkeiten)
    private void handleConfig(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String authHeader = ctx.request().getHeader("Authorization");

        gameService.configureGame(authHeader, body, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Countdown starten und Spiel in den Laufmodus bringen
    private void handleStart(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        gameService.startGame(authHeader, ar -> {
            if (ar.succeeded()) {
                JsonObject response = ar.result();
                JsonObject mqttState = new JsonObject()
                        .put("state", "COUNTDOWN")
                        .put("session_id", response.getLong("session_id"))
                        .put("timestamp", System.currentTimeMillis());
                vertx.eventBus().publish("game.state", mqttState);

                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(response.encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Laufenden Countdown abbrechen
    private void handleAbortCountdown(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        gameService.abortCountdown(authHeader, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Runde neu starten und in die Lobby zuruecksetzen
    private void handleRestart(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        gameService.restartGame(authHeader, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Korrekte Antwort fuer Bot-Logik liefern
    private void handleBotAnswer(RoutingContext ctx) {
        String questionIdParam = ctx.pathParam("questionId");
        Long questionId;
        try {
            questionId = Long.parseLong(questionIdParam);
        } catch (NumberFormatException e) {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Invalid question ID").encode());
            return;
        }

        gameService.getCorrectAnswerForBot(questionId, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Einheitliche Fehlerantwort fuer Game-Routen
    private void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof GameService.ApiException) {
            GameService.ApiException apiError = (GameService.ApiException) err;
            ctx.response()
                    .setStatusCode(apiError.getStatus())
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", apiError.getMessage()).encode());
            return;
        }

        ctx.response()
                .setStatusCode(500)
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("error", "Internal server error").encode());
    }
}
