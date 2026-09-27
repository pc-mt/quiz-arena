package com.example.highscore;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class HighscoreService {

    private final HighscoreRepository highscoreRepository;

    public HighscoreService() {
        this.highscoreRepository = new HighscoreRepository();
    }

    // --- Highscores lesen ---
    // Liefert die Top-Eintraege fuer den angeforderten Modus.
    public void getHighscores(String mode, Handler<AsyncResult<JsonObject>> resultHandler) {
        String roundLength = normalizeMode(mode);
        if (roundLength == null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Mode must be Q5, Q10 or Q20")));
            return;
        }

        highscoreRepository.fetchHighscoresByRoundLength(roundLength)
                .onSuccess(rows -> {
                    JsonArray list = new JsonArray();
                    int rank = 1;
                    for (Row row : rows) {
                        if (list.size() >= 20) {
                            break; // max 20 entries per mode
                        }
                        Long userId = row.getLong("user_id");
                        list.add(new JsonObject()
                                .put("rank", rank++)
                                .put("user_id", userId)
                                .put("username", row.getString("username"))
                                .put("display_name", row.getString("display_name"))
                                .put("total_points", row.getBigDecimal("total_points") != null
                                        ? row.getBigDecimal("total_points").doubleValue()
                                        : 0.0)
                                .put("total_response_time_ms", row.getLong("total_response_time_ms"))
                                .put("created_at", row.getValue("created_at") != null
                                        ? row.getValue("created_at").toString()
                                        : null));
                    }
                    resultHandler.handle(Future.succeededFuture(new JsonObject()
                            .put("round_length", roundLength)
                            .put("highscores", list)));
                })
                .onFailure(err -> resultHandler.handle(
                        Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    // Modus normalisieren (Q5/Q10/Q20).
    private String normalizeMode(String mode) {
        if (mode == null) return null;
        String value = mode.trim().toUpperCase();
        return switch (value) {
            case "Q5" -> "Q5";
            case "Q10" -> "Q10";
            case "Q20" -> "Q20";
            default -> null;
        };
    }

    // Fehlerklasse fuer HTTP-Rueckgaben mit Statuscode.
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
