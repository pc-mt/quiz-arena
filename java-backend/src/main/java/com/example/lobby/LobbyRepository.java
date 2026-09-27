package com.example.lobby;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class LobbyRepository {

    private final JDBCPool jdbcPool;

    public LobbyRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<RowSet<Row>> fetchActiveSession() {
        String query = "SELECT id, state, round_length, host_user_id, created_at " +
                "FROM game_sessions " +
                "WHERE state IN ('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS') " +
                "ORDER BY id DESC LIMIT 1";
        return jdbcPool.query(query).execute();
    }

    public Future<RowSet<Row>> fetchSessionById(Long sessionId) {
        String query = "SELECT id, state, round_length, host_user_id, created_at " +
                "FROM game_sessions WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> createSession() {
        String query = "INSERT INTO game_sessions (round_length, allow_easy, allow_medium, allow_hard, state) " +
                "VALUES ('Q5', 1, 1, 1, 'LOBBY')";
        return jdbcPool.query(query).execute();
    }

    public Future<RowSet<Row>> fetchLatestSession() {
        String query = "SELECT id, state, round_length, host_user_id, created_at " +
                "FROM game_sessions ORDER BY id DESC LIMIT 1";
        return jdbcPool.query(query).execute();
    }

    public Future<RowSet<Row>> fetchControllerByExternalId(String controllerId) {
        String query = "SELECT id, controller_id, controller_type, status, assigned_user_id " +
                "FROM controllers WHERE controller_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId));
    }

    public Future<RowSet<Row>> insertController(String controllerId, String controllerType) {
        String query = "INSERT INTO controllers (controller_id, controller_type, status) " +
                "VALUES (?, ?, 'FREE')";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId, controllerType));
    }

    public Future<RowSet<Row>> assignController(Long controllerDbId, Long userId) {
        String query = "UPDATE controllers SET status = 'ASSIGNED', assigned_user_id = ? WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId, controllerDbId));
    }

    public Future<RowSet<Row>> releaseControllerByUser(Long userId) {
        String query = "UPDATE controllers SET status = 'FREE', assigned_user_id = NULL WHERE assigned_user_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId));
    }

    public Future<RowSet<Row>> releaseControllersForSession(Long sessionId) {
        String query = "UPDATE controllers c " +
                "JOIN game_session_players gsp ON c.id = gsp.controller_id " +
                "SET c.status = 'FREE', c.assigned_user_id = NULL " +
                "WHERE gsp.game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> joinSession(Long sessionId, Long userId, Long controllerDbId) {
        String query = "INSERT INTO game_session_players (game_session_id, user_id, controller_id, is_ready) " +
                "VALUES (?, ?, ?, 0) " +
                "ON DUPLICATE KEY UPDATE controller_id = VALUES(controller_id), is_ready = VALUES(is_ready), " +
                "joined_at = CURRENT_TIMESTAMP";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, userId, controllerDbId));
    }

    public Future<RowSet<Row>> countSessionPlayers(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_players WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> assignHostIfMissing(Long sessionId, Long userId) {
        String query = "UPDATE game_sessions SET host_user_id = ? WHERE id = ? AND host_user_id IS NULL";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId, sessionId));
    }

    public Future<RowSet<Row>> leaveSession(Long sessionId, Long userId) {
        String query = "DELETE FROM game_session_players WHERE game_session_id = ? AND user_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, userId));
    }

    public Future<RowSet<Row>> setPlayerReady(Long sessionId, Long userId, boolean ready) {
        String query = "UPDATE game_session_players SET is_ready = ? WHERE game_session_id = ? AND user_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(ready ? 1 : 0, sessionId, userId));
    }

    public Future<RowSet<Row>> fetchSessionPlayerByControllerExternalId(Long sessionId, String controllerId) {
        String query = "SELECT gsp.user_id, gsp.controller_id " +
                "FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gsp.game_session_id = ? AND c.controller_id = ? " +
                "LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, controllerId));
    }

    public Future<RowSet<Row>> fetchControllerRealtimeStatus(String controllerId) {
        String query = "SELECT c.controller_id, c.status AS controller_status, gsp.is_ready, " +
                "u.username, u.display_name, gs.id AS session_id, " +
                "COALESCE((SELECT SUM(ga.points_awarded) FROM game_answers ga " +
                "WHERE ga.game_session_id = gs.id AND ga.user_id = u.id), 0) AS points " +
                "FROM controllers c " +
                "LEFT JOIN game_session_players gsp ON gsp.controller_id = c.id " +
                "LEFT JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "LEFT JOIN users u ON u.id = gsp.user_id " +
                "WHERE c.controller_id = ? " +
                "AND (gs.id IS NULL OR gs.state IN ('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS')) " +
                "ORDER BY gs.id DESC, gsp.joined_at DESC " +
                "LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId));
    }

    public Future<RowSet<Row>> clearSessionPlayers(Long sessionId) {
        String query = "DELETE FROM game_session_players WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> endSession(Long sessionId) {
        String query = "UPDATE game_sessions SET state = 'ENDED', ended_at = CURRENT_TIMESTAMP WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchLobbyPlayers(Long sessionId) {
        String query = "SELECT u.id AS user_id, u.username, u.display_name, u.profile_photo_url, gsp.is_ready, " +
                "c.controller_id, c.controller_type, c.status AS controller_status " +
                "FROM game_session_players gsp " +
                "JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gsp.game_session_id = ? " +
                "ORDER BY gsp.joined_at ASC";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    /**
     * Prueft, ob der Nutzer bereits in einer aktiven Session ist.
     */
    public Future<RowSet<Row>> isUserInActiveSession(Long userId) {
        String query = "SELECT 1 FROM game_session_players gsp " +
                "JOIN game_sessions gs ON gs.id = gsp.game_session_id " +
                "WHERE gsp.user_id = ? AND gs.state IN ('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS') " +
                "LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId));
    }

    /**
     * Prueft, ob der Nutzer bereits in der angegebenen Session ist.
     */
    public Future<RowSet<Row>> isUserInSession(Long userId, Long sessionId) {
        String query = "SELECT 1 FROM game_session_players WHERE user_id = ? AND game_session_id = ? LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId, sessionId));
    }

    /** Externe Controller-IDs aller Spieler in der aktiven Session (Heartbeat). */
    public Future<RowSet<Row>> fetchControllerIdsInActiveSession() {
        String query = "SELECT c.controller_id FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gsp.game_session_id = (SELECT id FROM game_sessions " +
                "WHERE state IN ('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS') ORDER BY id DESC LIMIT 1) " +
                "AND c.status = 'ASSIGNED'";
        return jdbcPool.query(query).execute();
    }

    /** Externe Controller-IDs aller Spieler in der angegebenen Session (Pre-Question-Ping). */
    public Future<RowSet<Row>> fetchControllerIdsBySessionId(Long sessionId) {
        String query = "SELECT c.controller_id FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gsp.game_session_id = ? AND c.status = 'ASSIGNED'";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }
}
