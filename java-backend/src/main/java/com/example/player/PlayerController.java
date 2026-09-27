package com.example.player;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class PlayerController implements HttpController {

    private final PlayerService playerService;

    public PlayerController() {
        this.playerService = new PlayerService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.route("/api/auth/*").handler(BodyHandler.create());

        // --- Auth-Routen ---
        router.post("/api/auth/register").handler(this::handleRegister);
        router.post("/api/auth/login").handler(this::handleLogin);
        router.put("/api/auth/rfid").handler(this::handleAddOrUpdateRfid);
        router.delete("/api/auth/rfid").handler(this::handleDeleteRfid);
        router.put("/api/auth/profile").handler(this::handleUpdateProfile);
        router.put("/api/auth/profile/:userId").handler(this::handleUpdateProfileByUserId);
    }

    // Register-Route
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body != null ? body.getString("username") : null;
        String password = body != null ? body.getString("password") : null;
        String rfidUid = body != null ? body.getString("rfid_uid") : null;

        playerService.register(username, password, rfidUid, ar -> {
            if (ar.succeeded()) {
                ctx.response()
                        .setStatusCode(201)
                        .putHeader("content-type", "application/json")
                        .end(ar.result().encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Login-Route
    private void handleLogin(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String username = body != null ? body.getString("username") : null;
        String password = body != null ? body.getString("password") : null;

        playerService.login(username, password, ar -> {
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

    // RFID speichern/aktualisieren
    private void handleAddOrUpdateRfid(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String rfidUid = body != null ? body.getString("rfid_uid") : null;
        String authHeader = ctx.request().getHeader("Authorization");

        playerService.addOrUpdateRfid(authHeader, rfidUid, ar -> {
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

    // RFID entfernen
    private void handleDeleteRfid(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");

        playerService.deleteRfid(authHeader, ar -> {
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

    // Profildaten aktualisieren
    private void handleUpdateProfile(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String displayName = body != null ? body.getString("display_name") : null;
        String profilePhotoUrl = body != null ? body.getString("profile_photo_url") : null;
        String authHeader = ctx.request().getHeader("Authorization");

        playerService.updateProfile(authHeader, displayName, profilePhotoUrl, ar -> {
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

    // Profildaten eines Nutzers per userId aktualisieren (ohne Token-Pruefung).
    private void handleUpdateProfileByUserId(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String displayName = body != null ? body.getString("display_name") : null;
        String profilePhotoUrl = body != null ? body.getString("profile_photo_url") : null;
        String userIdParam = ctx.pathParam("userId");

        Long userId;
        try {
            userId = Long.parseLong(userIdParam);
        } catch (Exception _err) {
            ctx.response()
                    .setStatusCode(400)
                    .putHeader("content-type", "application/json")
                    .end(new JsonObject().put("error", "Invalid user_id").encode());
            return;
        }

        playerService.updateProfileByUserId(userId, displayName, profilePhotoUrl, ar -> {
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

    // Einheitliches Fehlerformat fuer Auth-Routen
    private void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof PlayerService.ApiException) {
            PlayerService.ApiException apiError = (PlayerService.ApiException) err;
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
