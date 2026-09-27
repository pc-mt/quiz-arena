package com.example.highscore;

import com.example.http.HttpController;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class HighscoreController implements HttpController {

    private final HighscoreService highscoreService;

    public HighscoreController() {
        this.highscoreService = new HighscoreService();
    }

    @Override
    public void registerRoutes(Router router) {
        // --- Highscore-Route ---
        router.get("/api/highscores/:mode").handler(this::handleGetByMode);
    }

    // Highscores fuer den angeforderten Modus laden (Q5/Q10/Q20)
    private void handleGetByMode(RoutingContext ctx) {
        String mode = ctx.pathParam("mode");
        highscoreService.getHighscores(mode, ar -> {
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

    // Einheitliche Fehlerantwort fuer Highscore-Route
    private void handleError(RoutingContext ctx, Throwable err) {
        if (err instanceof HighscoreService.ApiException) {
            HighscoreService.ApiException apiError = (HighscoreService.ApiException) err;
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
