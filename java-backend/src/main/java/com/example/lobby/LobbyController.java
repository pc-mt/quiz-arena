package com.example.lobby;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class LobbyController implements HttpController {

    private final LobbyService lobbyService;
    private final Vertx vertx;

    public LobbyController(Vertx vertx) {
        this.vertx = vertx;
        this.lobbyService = new LobbyService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.route("/api/lobby/*").handler(BodyHandler.create());
        // --- Lobby-Routen ---
        router.get("/api/lobby/session").handler(this::handleGetOrCreateSession);
        router.get("/api/lobby/status").handler(this::handleStatus);
        router.post("/api/lobby/join").handler(this::handleJoin);
        router.post("/api/lobby/leave").handler(this::handleLeave);
        router.post("/api/lobby/kick").handler(this::handleKick);
        router.post("/api/lobby/ready").handler(this::handleReady);
        router.post("/api/lobby/reset").handler(this::handleReset);
    }

    // Session holen oder neu anlegen
    private void handleGetOrCreateSession(RoutingContext ctx) {
        lobbyService.getOrCreateSession(ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Aktuellen Lobby-Status (Session + Spieler) liefern
    private void handleStatus(RoutingContext ctx) {
        lobbyService.getLobbyStatus(ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Spieler der Session beitreten lassen
    private void handleJoin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String controllerId = body != null ? body.getString("controller_id") : null;
        String controllerType = body != null ? body.getString("controller_type") : null;
        String authHeader = ctx.request().getHeader("Authorization");

        lobbyService.joinSession(authHeader, controllerId, controllerType, ar -> {
            if (ar.succeeded()) {
                vertx.eventBus().publish("controllers.updated", "join");
                vertx.eventBus().publish("lobby.updated", "join");
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Spieler aus Session austragen
    private void handleLeave(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        lobbyService.leaveSession(authHeader, ar -> {
            if (ar.succeeded()) {
                vertx.eventBus().publish("lobby.updated", "leave");
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Spieler ueber user_id aus der Session entfernen
    private void handleKick(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        Long userId = body != null ? body.getLong("user_id") : null;
        String authHeader = ctx.request().getHeader("Authorization");

        lobbyService.kickPlayer(authHeader, userId, ar -> {
            if (ar.succeeded()) {
                vertx.eventBus().publish("lobby.updated", "kick");
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Ready-Status setzen
    private void handleReady(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        Boolean ready = body != null && body.containsKey("ready") ? body.getBoolean("ready") : null;
        String authHeader = ctx.request().getHeader("Authorization");

        lobbyService.setReady(authHeader, ready, ar -> {
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

    // Session auf Lobby-Zustand zuruecksetzen
    private void handleReset(RoutingContext ctx) {
        lobbyService.resetSession(ar -> {
            if (ar.succeeded()) {
                vertx.eventBus().publish("lobby.updated", "reset");
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Einheitliche Fehlerantwort fuer Lobby-Routen
    private void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof LobbyService.ApiException) {
            LobbyService.ApiException apiError = (LobbyService.ApiException) err;
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
