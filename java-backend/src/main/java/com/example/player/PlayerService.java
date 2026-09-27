package com.example.player;

import org.mindrot.jbcrypt.BCrypt;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.sqlclient.Row;

public class PlayerService {

    private static JWTAuth jwtAuth;
    private final PlayerRepository playerRepository;
    private final com.example.lobby.LobbyRepository lobbyRepository;

    public static JWTAuth getJwtAuth() {
        return jwtAuth;
    }

    public PlayerService() {
        this.playerRepository = new PlayerRepository();
        this.lobbyRepository = new com.example.lobby.LobbyRepository();
    }

    // Initialisiert die JWT-Konfiguration einmal beim Start.
    public static void initializeJwtAuth(Vertx vertx) {
        String secretKey = System.getenv("JWT_SECRET_KEY") != null
                ? System.getenv("JWT_SECRET_KEY")
                : "jwt-secret-key-not-configured";

        jwtAuth = JWTAuth.create(vertx, new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setBuffer(secretKey)));
    }

    // --- Auth-Logik ---
    // Neuen Nutzer anlegen (optional mit RFID-Verknuepfung).
    public void register(String username, String password, String rfidUid,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (username == null || username.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Username is required")));
            return;
        }
        if (password == null || password.isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Password is required")));
            return;
        }

        String usernameTrimmed = username.trim();
        String usernameError = validateUsernameDetailed(usernameTrimmed);
        if (usernameError != null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, usernameError)));
            return;
        }

        String passwordError = validatePasswordDetailed(password);
        if (passwordError != null) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, passwordError)));
            return;
        }

        if (rfidUid != null && !isValidRfidUid(rfidUid)) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid RFID UID format")));
            return;
        }

        String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
        String rfidUidFinal = rfidUid != null ? normalizeRfidUid(rfidUid) : null;

        playerRepository.insertUser(usernameTrimmed, passwordHash, rfidUidFinal)
                .onSuccess(res -> playerRepository.fetchUserByUsername(usernameTrimmed)
                        .onSuccess(rows -> {
                            if (!rows.iterator().hasNext()) {
                                resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
                                return;
                            }
                            Row r = rows.iterator().next();
                            JsonObject response = new JsonObject()
                                    .put("id", r.getLong("id"))
                                    .put("username", r.getString("username"))
                                    .put("display_name", r.getString("display_name") != null
                                            ? r.getString("display_name")
                                            : r.getString("username"))
                                    .put("profile_photo_url", r.getString("profile_photo_url"))
                                    .put("created_at", r.getLocalDateTime("created_at").toString());

                            resultHandler.handle(Future.succeededFuture(response));
                        })
                        .onFailure(err -> resultHandler
                                .handle(Future.failedFuture(new ApiException(500, "Internal server error")))))
                .onFailure(err -> {
                    String errorMessage = getUniqueViolationMessage(err);
                    if (errorMessage != null) {
                        resultHandler.handle(Future.failedFuture(new ApiException(409, errorMessage)));
                        return;
                    }
                    resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
                });
    }

    // Login pruefen und JWT + Profildaten zurueckgeben.
    public void login(String username, String password, Handler<AsyncResult<JsonObject>> resultHandler) {
        if (username == null || username.trim().isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Username is required")));
            return;
        }
        if (password == null || password.isEmpty()) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Password is required")));
            return;
        }

        String usernameFinal = username.trim();

        playerRepository.fetchUserByUsername(usernameFinal)
                .onSuccess(rows -> {
                    if (!rows.iterator().hasNext()) {
                        resultHandler.handle(Future.failedFuture(new ApiException(401, "User not found")));
                        return;
                    }

                    Row r = rows.iterator().next();
                    String storedHash = r.getString("password_hash");
                    if (!BCrypt.checkpw(password, storedHash)) {
                        resultHandler.handle(Future.failedFuture(new ApiException(401, "Invalid password")));
                        return;
                    }

                    Long userId = r.getLong("id");
                    lobbyRepository.isUserInActiveSession(userId)
                            .onSuccess(sessionRows -> {
                                if (sessionRows.iterator().hasNext()) {
                                    resultHandler.handle(Future.failedFuture(
                                            new ApiException(409,
                                                    "Dieser Account ist bereits in einer aktiven Spielsitzung angemeldet. Bitte die laufende Session zuerst verlassen, bevor du dich erneut einloggst.")));
                                    return;
                                }
                                String token = generateJwtToken(userId, usernameFinal);
                                JsonObject response = new JsonObject()
                                        .put("id", r.getLong("id"))
                                        .put("username", r.getString("username"))
                                        .put("display_name",
                                                r.getString("display_name") != null ? r.getString("display_name")
                                                        : r.getString("username"))
                                        .put("profile_photo_url", r.getString("profile_photo_url"))
                                        .put("rfid_uid", r.getString("rfid_uid"))
                                        .put("token", token);
                                resultHandler.handle(Future.succeededFuture(response));
                            })
                            .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
                    return;
                })
                .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    // RFID-Karte fuer den eingeloggten Nutzer setzen oder aktualisieren.
    public void addOrUpdateRfid(String authHeader, String rfidUid,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();
            if (rfidUid == null || rfidUid.trim().isEmpty()) {
                resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid RFID UID format")));
                return;
            }

            if (!isValidRfidUid(rfidUid)) {
                resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid RFID UID format")));
                return;
            }

            String rfidUidFinal = normalizeRfidUid(rfidUid);

            playerRepository.fetchUserByRfidExcludingId(rfidUidFinal, userId)
                    .onSuccess(rows -> {
                        if (rows.iterator().hasNext()) {
                            resultHandler.handle(
                                    Future.failedFuture(new ApiException(409, "RFID already assigned to another account")));
                            return;
                        }
                        playerRepository.updateUserRfid(rfidUidFinal, userId)
                                .onSuccess(updateRes -> resultHandler.handle(Future.succeededFuture(new JsonObject()
                                        .put("message", "RFID successfully linked to account")
                                        .put("rfid_uid", rfidUidFinal))))
                                .onFailure(err -> {
                                    String errorMessage = getUniqueViolationMessage(err);
                                    if (errorMessage != null) {
                                        resultHandler.handle(Future.failedFuture(new ApiException(409, errorMessage)));
                                    } else {
                                        resultHandler.handle(
                                                Future.failedFuture(new ApiException(500, "Internal server error")));
                                    }
                                });
                    })
                    .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    // RFID-Karte beim eingeloggten Nutzer entfernen.
    public void deleteRfid(String authHeader, Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }

            Long userId = userIdResult.result();
            playerRepository.fetchUserById(userId)
                    .onSuccess(rows -> {
                        if (!rows.iterator().hasNext()) {
                            resultHandler.handle(
                                    Future.failedFuture(new ApiException(404, "No RFID linked to this account")));
                            return;
                        }

                        Row row = rows.iterator().next();
                        String currentRfid = row.getString("rfid_uid");
                        if (currentRfid == null || currentRfid.trim().isEmpty()) {
                            resultHandler.handle(
                                    Future.failedFuture(new ApiException(404, "No RFID linked to this account")));
                            return;
                        }

                        playerRepository.clearUserRfid(row.getLong("id"))
                                .onSuccess(updateRes -> resultHandler.handle(Future.succeededFuture(new JsonObject()
                                        .put("message", "RFID successfully unlinked from account"))))
                                .onFailure(err -> resultHandler
                                        .handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
                    })
                    .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    /** Aktualisiert display_name und/oder profile_photo_url des eingeloggten Nutzers. */
    public void updateProfile(String authHeader, String displayName, String profilePhotoUrl,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        getUserIdFromAuthHeader(authHeader, userIdResult -> {
            if (userIdResult.failed()) {
                resultHandler.handle(Future.failedFuture(userIdResult.cause()));
                return;
            }
            Long userId = userIdResult.result();
            String nameValue = displayName != null ? displayName.trim() : "";
            if (nameValue.length() > 80) {
                resultHandler.handle(Future.failedFuture(new ApiException(400, "Display name must be at most 80 characters")));
                return;
            }
            String nameOrNull = nameValue.isEmpty() ? null : nameValue;
            String photoValue = profilePhotoUrl != null ? profilePhotoUrl.trim() : "";
            if (photoValue.length() > 255) {
                resultHandler.handle(Future.failedFuture(new ApiException(400, "Profile photo URL must be at most 255 characters")));
                return;
            }
            String photoOrNull = photoValue.isEmpty() ? null : photoValue;
            playerRepository.updateDisplayName(userId, nameOrNull)
                    .compose(v -> playerRepository.updateProfilePhotoUrl(userId, photoOrNull))
                    .onSuccess(v -> resultHandler.handle(Future.succeededFuture(
                            new JsonObject()
                                    .put("display_name", nameOrNull != null ? nameOrNull : "")
                                    .put("profile_photo_url", photoOrNull != null ? photoOrNull : ""))))
                    .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
        });
    }

    /** Aktualisiert display_name und/oder profile_photo_url eines Nutzers per userId (ohne Token). */
    public void updateProfileByUserId(Long userId, String displayName, String profilePhotoUrl,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        if (userId == null || userId <= 0) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Invalid user_id")));
            return;
        }

        String nameValue = displayName != null ? displayName.trim() : "";
        if (nameValue.length() > 80) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Display name must be at most 80 characters")));
            return;
        }
        String nameOrNull = nameValue.isEmpty() ? null : nameValue;

        String photoValue = profilePhotoUrl != null ? profilePhotoUrl.trim() : "";
        if (photoValue.length() > 255) {
            resultHandler.handle(Future.failedFuture(new ApiException(400, "Profile photo URL must be at most 255 characters")));
            return;
        }
        String photoOrNull = photoValue.isEmpty() ? null : photoValue;

        playerRepository.updateDisplayName(userId, nameOrNull)
                .compose(v -> playerRepository.updateProfilePhotoUrl(userId, photoOrNull))
                .onSuccess(v -> resultHandler.handle(Future.succeededFuture(
                        new JsonObject()
                                .put("user_id", userId)
                                .put("display_name", nameOrNull != null ? nameOrNull : "")
                                .put("profile_photo_url", photoOrNull != null ? photoOrNull : ""))))
                .onFailure(err -> resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error"))));
    }

    // --- Interne Helper ---
    // Liest die user_id aus dem Bearer-Token.
    private void getUserIdFromAuthHeader(String authHeader, Handler<AsyncResult<Long>> resultHandler) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required")));
            return;
        }

        String token = authHeader.substring(7);
        JsonObject credentials = new JsonObject().put("token", token);

        if (jwtAuth == null) {
            resultHandler.handle(Future.failedFuture(new ApiException(500, "Internal server error")));
            return;
        }

        jwtAuth.authenticate(credentials)
                .onSuccess(user -> {
                    JsonObject principal = user.principal();
                    String userIdStr = principal.getString("sub");
                    if (userIdStr == null) {
                        resultHandler.handle(Future.failedFuture(new ApiException(401, "Unauthorized: invalid token")));
                        return;
                    }
                    resultHandler.handle(Future.succeededFuture(Long.parseLong(userIdStr)));
                })
                .onFailure(err -> resultHandler
                        .handle(Future.failedFuture(new ApiException(401, "Unauthorized: authentication required"))));
    }

    private static String generateJwtToken(Long userId, String username) {
        return generateJwtTokenStatic(userId, username);
    }

    public static String generateJwtTokenStatic(Long userId, String username) {
        if (jwtAuth == null) {
            throw new IllegalStateException("JWT Auth not initialized. Call initializeJwtAuth() first.");
        }

        JsonObject claims = new JsonObject()
                .put("sub", userId.toString())
                .put("username", username);

        return jwtAuth.generateToken(claims, new JWTOptions()
                .setAlgorithm("HS256")
                .setExpiresInSeconds(24 * 60 * 60));
    }

    private static String validateUsernameDetailed(String username) {
        if (username == null) {
            return "Username is required";
        }

        String u = username.trim();
        if (u.isEmpty()) {
            return "Username cannot be empty";
        }

        if (u.length() < 3) {
            return "Username too short (minimum 3 characters)";
        }
        if (u.length() > 64) {
            return "Username too long (maximum 64 characters)";
        }

        for (char c : u.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-') {
                return "Username contains invalid characters (only letters, numbers, '_', and '-' are allowed)";
            }
        }

        return null;
    }

    private static String validatePasswordDetailed(String password) {
        if (password == null) {
            return "Password is required";
        }

        if (password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < 6) {
            return "Password too short (minimum 6 characters)";
        }
        if (password.length() > 256) {
            return "Password too long (maximum 256 characters)";
        }

        return null;
    }

    private static boolean isValidRfidUid(String rfidUid) {
        if (rfidUid == null) {
            return false;
        }
        String r = rfidUid.trim();
        if (r.isEmpty()) {
            return false;
        }

        if (r.length() > 64) {
            return false;
        }

        return r.matches("^[0-9A-Fa-f: ]+$");
    }

    private static String normalizeRfidUid(String rfidUid) {
        if (rfidUid == null) {
            return null;
        }
        return rfidUid.replaceAll("[: ]", "").toUpperCase().trim();
    }

    private static String getUniqueViolationMessage(Throwable err) {
        if (err == null || err.getMessage() == null) {
            return null;
        }

        String msg = err.getMessage();
        String msgLower = msg.toLowerCase();

        if (!msgLower.contains("duplicate") && !msgLower.contains("unique")) {
            return null;
        }

        if (msg.contains("uq_users_username")) {
            return "Username already exists";
        }
        if (msg.contains("uq_users_rfid_uid")) {
            return "RFID already assigned to another account";
        }

        if (msgLower.contains("users.rfid_uid") || msgLower.contains("rfid_uid")) {
            return "RFID already assigned to another account";
        }
        if (msgLower.contains("users.username")) {
            return "Username already exists";
        }

        if (msgLower.contains("rfid") && !msgLower.contains("username")) {
            return "RFID already assigned to another account";
        }
        if (msgLower.contains("username") && !msgLower.contains("rfid")) {
            return "Username already exists";
        }

        return "Username or RFID already exists";
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
