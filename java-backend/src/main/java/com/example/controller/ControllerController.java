package com.example.controller;

import com.example.http.HttpController;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class ControllerController implements HttpController {

    private final ControllerService controllerService;

    public ControllerController() {
        this.controllerService = new ControllerService();
    }

    @Override
    public void registerRoutes(Router router) {
        router.route("/api/controllers/*").handler(BodyHandler.create());
        // --- Controller-Routen ---
        router.post("/api/controllers/register").handler(this::handleRegister);
        router.get("/api/controllers/available").handler(this::handleAvailable);
    }

    // Controller registrieren oder aktualisieren
    private void handleRegister(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        String controllerId = body != null ? body.getString("controller_id") : null;
        String controllerType = body != null ? body.getString("controller_type") : null;

        controllerService.registerController(controllerId, controllerType, ar -> {
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

    // Alle verfuegbaren Controller laden
    private void handleAvailable(RoutingContext ctx) {
        controllerService.getAvailableControllers(ar -> {
            if (ar.succeeded()) {
                JsonArray list = ar.result();
                ctx.response()
                        .setStatusCode(200)
                        .putHeader("content-type", "application/json")
                        .end(list.encode());
            } else {
                handleError(ctx, ar.cause());
            }
        });
    }

    // Einheitliche Fehlerantwort fuer Controller-Routen
    private void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof ControllerService.ApiException) {
            ControllerService.ApiException apiError = (ControllerService.ApiException) err;
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
