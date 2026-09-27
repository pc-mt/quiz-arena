package com.example.lobby;

import com.example.player.PlayerService;
import com.example.player.PlayerRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.sqlclient.Row;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LobbyService {

    private static final Logger logger = LoggerFactory.getLogger(LobbyService.class);

    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;
    private final JWTAuth jwtAuth;

    public LobbyService() {
        this.lobbyRepository = new LobbyRepository();
        this.playerRepository = new PlayerRepository();
        this.jwtAuth = PlayerService.getJwtAuth();
    }

    // --- Session-Verwaltung ---
    public void getOrCreateSession(Handler<AsyncResult<JsonObject>> resultHandler) {
        lobbyRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (rows.iterator().hasNext()) {
                        Row row = rows.iterator().next();
                        resultHandler.handle(Future.succeededFuture(mapSession(row)));
                        return;
                    }
                    lobbyRepository.createSession()
                            .onSuccess(res -> lobbyRepository.fetchLatestSession()
                                    .onSuccess(latest -> {
                                        if (!latest.iterator().hasNext()) {
                                            resultHandler.handle(
                                                    Future.failedFuture(new ApiException(500, "Internal server error")));
                                            return;
                                        }
                                        Row row = latest.iterator().next();
                                        resultHandler.handle(Future.succeededFuture(mapSession(row)));
                                    })
                                    .onFailure(err -> resultHandler.handle(
                                            Future.failedFuture(new ApiException(500, "Internal server error")))))
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    public void joinSession(String authHeader, String controllerId, String controllerType,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID is required")));
            return;
        }

        String controllerIdFinal = controllerId.trim();
        String controllerTypeFinal = (controllerType == null || controllerType.trim().isEmpty())
                ? "WEB"
                : controllerType.trim().toUpperCase();

        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();

            getOrCreateSession(ar -> {
                if (ar.failed()) {
                    resultHandler.handle(Future.failedFuture(ar.cause()));
                    return;
                }

                Long sessionId = ar.result().getLong("id");
                final Long userIdFinal = userId;

                // Nutzer darf nicht doppelt in derselben Session sein; zusaetzlich gilt das Platzlimit.
                lobbyRepository.isUserInSession(userIdFinal, sessionId)
                        .compose(userInSession -> {
                            if (userInSession.iterator().hasNext()) {
                                throw new ApiException(409, "Already in session");
                            }
                            // Maximale Spielerzahl pruefen.
                            return lobbyRepository.countSessionPlayers(sessionId)
                                    .map(countRows -> {
                                        long count = countRows.iterator().next().getLong("total");
                                        if (count >= 99) {
                                            throw new ApiException(409, "Session is full (max 99 players)");
                                        }
                                        return true;
                                    });
                        })
                        .onSuccess(canJoin -> {
                            lobbyRepository.assignHostIfMissing(sessionId, userIdFinal)
                                    .onSuccess(hostRes -> ensureController(controllerIdFinal, controllerTypeFinal, controllerResult -> {
                                        if (controllerResult.failed()) {
                                            resultHandler.handle(Future.failedFuture(controllerResult.cause()));
                                            return;
                                        }

                                        Row controllerRow = controllerResult.result();
                                        String status = controllerRow.getString("status");
                                        Long assignedUserId = controllerRow.getLong("assigned_user_id");

                                        if ("ASSIGNED".equalsIgnoreCase(status)
                                                && (assignedUserId == null || !assignedUserId.equals(userIdFinal))) {
                                            resultHandler.handle(
                                                    Future.failedFuture(new ApiException(409, "Controller already assigned")));
                                            return;
                                        }

                                        Long controllerDbId = controllerRow.getLong("id");

                                        lobbyRepository.assignController(controllerDbId, userIdFinal)
                                                .onSuccess(assignRes -> lobbyRepository.joinSession(sessionId, userIdFinal, controllerDbId)
                                                        .onSuccess(joinRes -> {
                                                            JsonObject response = new JsonObject()
                                                                    .put("message", "Joined session")
                                                                    .put("session_id", sessionId)
                                                                    .put("controller_id", controllerIdFinal)
                                                                    .put("controller_type", controllerTypeFinal);
                                                            resultHandler.handle(Future.succeededFuture(response));
                                                        })
                                                        .onFailure(err -> resultHandler.handle(
                                                                Future.failedFuture(new ApiException(500, "Internal server error")))))
                                                .onFailure(err -> resultHandler.handle(
                                                        Future.failedFuture(new ApiException(500, "Internal server error"))));
                                    }))
                                    .onFailure(err -> resultHandler.handle(
                                            Future.failedFuture(new ApiException(500, "Internal server error"))));
                        })
                        .onFailure(err -> resultHandler.handle(Future.failedFuture(err)));
            });
        });
    }

    public void leaveSession(String authHeader, Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();
            lobbyRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(
                                    Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }
                        Long sessionId = rows.iterator().next().getLong("id");
                        lobbyRepository.leaveSession(sessionId, userId)
                                .onSuccess(res -> lobbyRepository.releaseControllerByUser(userId)
                                        .onSuccess(rel -> resultHandler.handle(Future.succeededFuture(
                                                new JsonObject().put("message", "Left session"))))
                                        .onFailure(err -> resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error")))))
                                .onFailure(err -> resultHandler.handle(
                                        Future.failedFuture(new ApiException(500, "Internal server error"))));
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    public void setReady(String authHeader, Boolean ready, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (ready == null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Field 'ready' must be true or false")));
            return;
        }

        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();
            lobbyRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }

                        Long sessionId = rows.iterator().next().getLong("id");
                        lobbyRepository.setPlayerReady(sessionId, userId, ready)
                                .onSuccess(update -> {
                                    if (update.rowCount() == 0) {
                                        resultHandler.handle(Future.failedFuture(
                                                new ApiException(404, "User is not part of the active session")));
                                        return;
                                    }

                                    JsonObject response = new JsonObject()
                                            .put("message", ready ? "Player marked as ready" : "Player marked as not ready")
                                            .put("session_id", sessionId)
                                            .put("user_id", userId)
                                            .put("is_ready", ready);
                                    resultHandler.handle(Future.succeededFuture(response));
                                })
                                .onFailure(err -> resultHandler
                                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    public void setReadyByControllerId(String controllerId, Boolean ready,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID is required")));
            return;
        }
        if (ready == null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Field 'ready' must be true or false")));
            return;
        }

        String controllerIdFinal = controllerId.trim();

        lobbyRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                        return;
                    }

                    Long sessionId = rows.iterator().next().getLong("id");
                    lobbyRepository.fetchSessionPlayerByControllerExternalId(sessionId, controllerIdFinal)
                            .onSuccess(players -> {
                                if (!players.iterator().hasNext()) {
                                    resultHandler.handle(Future.failedFuture(
                                            new ApiException(404, "Controller is not part of the active session")));
                                    return;
                                }

                                Long userId = players.iterator().next().getLong("user_id");
                                lobbyRepository.setPlayerReady(sessionId, userId, ready)
                                        .onSuccess(update -> {
                                            JsonObject response = new JsonObject()
                                                    .put("message",
                                                            ready ? "Player marked as ready"
                                                                    : "Player marked as not ready")
                                                    .put("session_id", sessionId)
                                                    .put("controller_id", controllerIdFinal)
                                                    .put("user_id", userId)
                                                    .put("is_ready", ready);
                                            resultHandler.handle(Future.succeededFuture(response));
                                        })
                                        .onFailure(err -> resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error"))));
                            })
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    /**
     * Entfernt einen Spieler ueber die user_id aus der aktiven Session.
     * Es wird nur geprueft, ob der Aufrufer eingeloggt ist.
     */
    public void kickPlayer(String authHeader, Long targetUserId, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (targetUserId == null || targetUserId <= 0) {
            resultHandler.handle(Future.failedFuture(
                    new ApiException(400, "Field 'user_id' must be a positive number")));
            return;
        }

        // Es reicht hier, dass der Aufrufer authentifiziert ist.
        getUserIdFromAuthHeader(authHeader, authResult -> {
            if (authResult.failed()) {
                resultHandler.handle(Future.failedFuture(authResult.cause()));
                return;
            }

            lobbyRepository.fetchActiveSession()
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(Future.failedFuture(new ApiException(404, "No active session")));
                            return;
                        }
                        Long sessionId = rows.iterator().next().getLong("id");
                        lobbyRepository.leaveSession(sessionId, targetUserId)
                                .onSuccess(res -> lobbyRepository.releaseControllerByUser(targetUserId)
                                        .onSuccess(rel -> resultHandler.handle(Future.succeededFuture(
                                                new JsonObject()
                                                        .put("message", "Player removed from session")
                                                        .put("session_id", sessionId)
                                                        .put("user_id", targetUserId))))
                                        .onFailure(err -> resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error")))))
                                .onFailure(err -> resultHandler.handle(
                                        Future.failedFuture(new ApiException(500, "Internal server error"))));
                    })
                    .onFailure(err -> resultHandler
                            .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    // --- RFID-Login ---
    public void joinSessionByRfid(String controllerId, String rfidUid,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID is required")));
            return;
        }
        if (rfidUid == null || rfidUid.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "RFID UID is required")));
            return;
        }

        String controllerIdFinal = controllerId.trim();
        String rfidUidFinal = normalizeRfidUid(rfidUid);

        logger.info("RFID lookup: raw='{}', normalized='{}', length={}",
                    rfidUid, rfidUidFinal, rfidUidFinal != null ? rfidUidFinal.length() : -1);

        playerRepository.fetchUserByRfid(rfidUidFinal)
                .onSuccess(userRows -> {
                    logger.info("RFID lookup result: hasRows={}", userRows.iterator().hasNext());
                    if (!userRows.iterator().hasNext()) {
                        logger.warn("RFID not found in database: '{}'", rfidUidFinal);
                        resultHandler.handle(Future.failedFuture(new ApiException(404, "RFID card not recognized")));
                        return;
                    }
                    Row userRow = userRows.iterator().next();
                    Long userId = userRow.getLong("id");
                    logger.info("RFID found: userId={}, username={}", userId, userRow.getString("username"));

                    getOrCreateSession(ar -> {
                        if (ar.failed()) {
                            resultHandler.handle(Future.failedFuture(ar.cause()));
                            return;
                        }
                        Long sessionId = ar.result().getLong("id");

                        lobbyRepository.isUserInSession(userId, sessionId)
                                .onSuccess(inSessionRows -> {
                                    boolean alreadyInThisSession = inSessionRows.iterator().hasNext();
                                    if (alreadyInThisSession) {
                                        doRfidAssignAndJoin(controllerIdFinal, userId, sessionId, userRow, resultHandler);
                                        return;
                                    }
                                    lobbyRepository.isUserInActiveSession(userId)
                                            .onSuccess(activeRows -> {
                                                if (activeRows.iterator().hasNext()) {
                                                    logger.warn("User {} already in another active session", userId);
                                                    resultHandler.handle(Future.failedFuture(new ApiException(409,
                                                            "Dieser Account ist bereits in einer aktiven Spielsitzung angemeldet.")));
                                                    return;
                                                }
                                                logger.info("User {} not in any active session, continue join flow", userId);
                                                doRfidAssignAndJoin(controllerIdFinal, userId, sessionId, userRow, resultHandler);
                                            })
                                            .onFailure(err -> resultHandler.handle(
                                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                                })
                                .onFailure(err -> resultHandler.handle(
                                        Future.failedFuture(new ApiException(500, "Internal server error"))));
                    });
                })
                .onFailure(err -> {
                    logger.error("fetchUserByRfid DB error: {}", err.getMessage());
                    resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
                });
    }

    private void doRfidAssignAndJoin(String controllerIdFinal, Long userId, Long sessionId, Row userRow,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        logger.info("RFID assign and join: controllerId={}, userId={}, sessionId={}",
                    controllerIdFinal, userId, sessionId);
        
        // Nutzer darf nicht doppelt in derselben Session sein; zusaetzlich gilt das Platzlimit.
        lobbyRepository.isUserInSession(userId, sessionId)
                .compose(userInSession -> {
                    if (userInSession.iterator().hasNext()) {
                        throw new ApiException(409, "Already in session");
                    }
                    return lobbyRepository.countSessionPlayers(sessionId)
                            .map(countRows -> {
                                long count = countRows.iterator().next().getLong("total");
                                if (count >= 99) {
                                    throw new ApiException(409, "Session is full (max 99 players)");
                                }
                                return true;
                            });
                })
                .onFailure(err -> resultHandler.handle(Future.failedFuture(err)))
                .onSuccess(canJoin -> lobbyRepository.assignHostIfMissing(sessionId, userId)
                .onSuccess(hostRes -> ensureController(controllerIdFinal, "HARDWARE", controllerResult -> {
                    if (controllerResult.failed()) {
                        resultHandler.handle(Future.failedFuture(controllerResult.cause()));
                        return;
                    }
                    Row controllerRow = controllerResult.result();
                    String status = controllerRow.getString("status");
                    Long assignedUserId = controllerRow.getLong("assigned_user_id");

                    if ("ASSIGNED".equalsIgnoreCase(status)
                            && (assignedUserId == null || !assignedUserId.equals(userId))) {
                        logger.warn("Controller {} already assigned to user {}, rejecting user {}",
                                    controllerIdFinal, assignedUserId, userId);
                        resultHandler.handle(Future.failedFuture(
                                new ApiException(409, "Controller already assigned")));
                        return;
                    }

                    Long controllerDbId = controllerRow.getLong("id");
                    lobbyRepository.assignController(controllerDbId, userId)
                            .onSuccess(assignRes -> lobbyRepository
                                    .joinSession(sessionId, userId, controllerDbId)
                                    .onSuccess(joinRes -> {
                                        String username = userRow.getString("username");
                                        String displayName = userRow.getString("display_name");
                                        String playerName = displayName != null ? displayName : username;
                                        String token = PlayerService.generateJwtTokenStatic(userId, username);
                                        JsonObject response = new JsonObject()
                                                .put("message", "RFID login successful")
                                                .put("session_id", sessionId)
                                                .put("controller_id", controllerIdFinal)
                                                .put("controller_type", "HARDWARE")
                                                .put("user_id", userId)
                                                .put("username", username)
                                                .put("player_name", playerName)
                                                .put("token", token);
                                        logger.info("RFID join success: {}", response.encode());
                                        resultHandler.handle(Future.succeededFuture(response));
                                    })
                                    .onFailure(err -> {
                                        logger.error("joinSession failed: {}", err.getMessage());
                                        resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
                                    }))
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                }))
                .onFailure(err -> resultHandler.handle(
                        Future.failedFuture(new ApiException(500, "Internal server error")))));
    }

    public void getControllerRealtimeStatus(String controllerId, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (controllerId == null || controllerId.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Controller ID is required")));
            return;
        }

        String controllerIdFinal = controllerId.trim();
        lobbyRepository.fetchControllerRealtimeStatus(controllerIdFinal)
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.failedFuture(new ApiException(404, "Controller not found")));
                        return;
                    }

                    Row row = rows.iterator().next();
                    String username = row.getString("username");
                    String displayName = row.getString("display_name");
                    Boolean isReady = row.getBoolean("is_ready");
                    Long sessionId = row.getLong("session_id");
                    boolean hasPlayer = sessionId != null && username != null;

                    JsonObject response = new JsonObject()
                            .put("controller_id", row.getString("controller_id"))
                            .put("controller_status", row.getString("controller_status"))
                            .put("is_ready", hasPlayer && isReady != null ? isReady : false)
                            .put("player_name", hasPlayer ? (displayName != null ? displayName : username) : null)
                            .put("has_player", hasPlayer)
                            .put("points", row.getValue("points"));

                    resultHandler.handle(Future.succeededFuture(response));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    public void resetSession(Handler<AsyncResult<JsonObject>> resultHandler) {
        lobbyRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.succeededFuture(new JsonObject()
                                .put("message", "No active session")));
                        return;
                    }

                    Long sessionId = rows.iterator().next().getLong("id");

                    lobbyRepository.releaseControllersForSession(sessionId)
                            .onSuccess(rel -> lobbyRepository.clearSessionPlayers(sessionId)
                                    .onSuccess(clear -> lobbyRepository.endSession(sessionId)
                                            .onSuccess(end -> resultHandler.handle(Future.succeededFuture(
                                                    new JsonObject().put("message", "Session closed"))))
                                            .onFailure(err -> resultHandler.handle(
                                                    Future.failedFuture(new ApiException(500, "Internal server error")))))
                                    .onFailure(err -> resultHandler.handle(
                                            Future.failedFuture(new ApiException(500, "Internal server error")))))
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    public void getLobbyStatus(Handler<AsyncResult<JsonObject>> resultHandler) {
        lobbyRepository.fetchActiveSession()
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.succeededFuture(new JsonObject()
                                .put("session", new JsonObject())
                                .put("players", new JsonArray())));
                        return;
                    }

                    Row sessionRow = rows.iterator().next();
                    Long sessionId = sessionRow.getLong("id");

                    lobbyRepository.fetchLobbyPlayers(sessionId)
                            .onSuccess(players -> {
                                JsonArray list = new JsonArray();
                                for (Row row : players) {
                                    JsonObject player = new JsonObject()
                                            .put("user_id", row.getLong("user_id"))
                                            .put("username", row.getString("username"))
                                            .put("display_name",
                                                    row.getString("display_name") != null
                                                            ? row.getString("display_name")
                                                            : row.getString("username"))
                                            .put("profile_photo_url", row.getString("profile_photo_url"))
                                            .put("is_ready", row.getBoolean("is_ready"))
                                            .put("controller_id", row.getString("controller_id"))
                                            .put("controller_type", row.getString("controller_type"))
                                            .put("controller_status", row.getString("controller_status"));
                                    list.add(player);
                                }
                                JsonObject response = new JsonObject()
                                        .put("session", mapSession(sessionRow))
                                        .put("players", list);
                                resultHandler.handle(Future.succeededFuture(response));
                            })
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    private void ensureController(String controllerId, String controllerType,
            Handler<AsyncResult<Row>> resultHandler) {
        lobbyRepository.fetchControllerByExternalId(controllerId)
                .onSuccess(rows -> {
                    if (rows.iterator().hasNext()) {
                        resultHandler.handle(Future.succeededFuture(rows.iterator().next()));
                        return;
                    }

                    lobbyRepository.insertController(controllerId, controllerType)
                            .onSuccess(res -> lobbyRepository.fetchControllerByExternalId(controllerId)
                                    .onSuccess(fresh -> {
                                        if (!fresh.iterator().hasNext()) {
                                            resultHandler.handle(Future.failedFuture(
                                                    new ApiException(500, "Internal server error")));
                                            return;
                                        }
                                        resultHandler.handle(Future.succeededFuture(fresh.iterator().next()));
                                    })
                                    .onFailure(err -> resultHandler.handle(Future.failedFuture(
                                            new ApiException(500, "Internal server error")))))
                            .onFailure(err -> resultHandler.handle(
                                    Future.failedFuture(new ApiException(500, "Internal server error"))));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
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

        String token = header.substring(7).trim();
        // Tolerant gegen Token mit versehentlich kopierten Anfuehrungszeichen.
        token = token.replaceAll("^\"+|\"+$", "");
        if (token.isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required")));
            return;
        }

        JsonObject credentials = new JsonObject().put("token", token);

        jwtAuth.authenticate(credentials)
                .onSuccess(user -> {
                    JsonObject principal = user.principal();
                    String userIdStr = principal.getString("sub");
                    if (userIdStr == null) {
                        resultHandler.handle(
                                Future.failedFuture(new ApiException(401, "Unauthorized: invalid token")));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture(Long.parseLong(userIdStr)));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required"))));
    }

    private static String normalizeRfidUid(String rfidUid) {
        if (rfidUid == null) {
            return null;
        }
        return rfidUid.replaceAll("[: ]", "").toUpperCase().trim();
    }

    private JsonObject mapSession(Row row) {
        return new JsonObject()
                .put("id", row.getLong("id"))
                .put("state", row.getString("state"))
                .put("round_length", row.getString("round_length"))
                .put("host_user_id", row.getLong("host_user_id"));
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
