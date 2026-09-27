package com.example.controller;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

public class ControllerService {

    private final ControllerRepository controllerRepository;

    public ControllerService() {
        this.controllerRepository = new ControllerRepository();
    }

    // --- Registrierung ---
    // Legt einen Controller an oder aktualisiert den bestehenden Eintrag.
    public void registerController(String controllerId, String controllerType,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID is required")));
            return;
        }

        String controllerIdFinal = controllerId.trim();
        if (controllerIdFinal.length() > 128) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID too long")));
            return;
        }

        String controllerTypeFinal = (controllerType == null || controllerType.trim().isEmpty())
                ? "WEB"
                : controllerType.trim().toUpperCase();

        if (!"WEB".equals(controllerTypeFinal) && !"HARDWARE".equals(controllerTypeFinal)) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid controller type")));
            return;
        }

        controllerRepository.fetchControllerByExternalId(controllerIdFinal)
                .onSuccess(rows -> {
                    if (rows.iterator().hasNext()) {
                        Row row = rows.iterator().next();
                        controllerRepository.touchLastSeenByControllerId(controllerIdFinal)
                                .onSuccess(v -> resultHandler.handle(Future.succeededFuture(mapController(row))))
                                .onFailure(v -> resultHandler.handle(Future.succeededFuture(mapController(row))));
                        return;
                    }

                    controllerRepository.insertController(controllerIdFinal, controllerTypeFinal)
                            .onSuccess(res -> controllerRepository.fetchControllerByExternalId(controllerIdFinal)
                                    .onSuccess(fresh -> {
                                        if (!fresh.iterator().hasNext()) {
                                            resultHandler.handle(Future.failedFuture(
                                                    new ApiException(500, "Internal server error")));
                                            return;
                                        }
                                        Row row = fresh.iterator().next();
                                        resultHandler.handle(Future.succeededFuture(mapController(row)));
                                    })
                                    .onFailure(err -> resultHandler.handle(
                                            Future.failedFuture(new ApiException(500, "Internal server error")))))
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler.handle(
                        Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    /** Heartbeat-Aktualisierung, damit der Controller in der Verfuegbarkeitsliste bleibt. */
    public void touchLastSeen(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        controllerRepository.touchLastSeenByControllerId(controllerId.trim())
                .onSuccess(v -> resultHandler.handle(Future.succeededFuture()))
                .onFailure(err -> resultHandler.handle(Future.failedFuture(err)));
    }

    /** Setzt den Controller auf OFFLINE, wenn kein Ping/Pong mehr kommt. */
    public void setControllerOffline(String controllerId, Handler<AsyncResult<Void>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        controllerRepository.setControllerOffline(controllerId.trim())
                .onSuccess(v -> resultHandler.handle(Future.succeededFuture()))
                .onFailure(err -> resultHandler.handle(Future.failedFuture(err)));
    }

    // --- Verfuegbare Controller ---
    public void getAvailableControllers(Handler<AsyncResult<JsonArray>> resultHandler) {
        controllerRepository.fetchAvailableControllers()
                .onSuccess(rows -> {
                    JsonArray list = new JsonArray();
                    for (Row row : rows) {
                        list.add(mapController(row));
                    }
                    resultHandler.handle(Future.succeededFuture(list));
                })
                .onFailure(err -> resultHandler.handle(
                        Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    // --- Mapper / Fehler ---
    private JsonObject mapController(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("controller_id", row.getString("controller_id"))
                .put("controller_type", row.getString("controller_type"))
                .put("status", row.getString("status"))
                .put("assigned_user_id", row.getLong("assigned_user_id"));
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
