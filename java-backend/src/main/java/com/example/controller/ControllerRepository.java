package com.example.controller;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ControllerRepository {

    private final JDBCPool jdbcPool;

    public ControllerRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    // --- Controller laden / anlegen ---
    public Future<RowSet<Row>> fetchControllerByExternalId(String controllerId) {
        String query = "SELECT id, controller_id, controller_type, status, assigned_user_id " +
                "FROM controllers WHERE controller_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId));
    }

    public Future<RowSet<Row>> insertController(String controllerId, String controllerType) {
        String query = "INSERT INTO controllers (controller_id, controller_type, status, last_seen_at) " +
                "VALUES (?, ?, 'FREE', CURRENT_TIMESTAMP)";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId, controllerType));
    }

    /** Aktualisiert last_seen_at, damit der Controller als aktiv gilt. */
    public Future<RowSet<Row>> touchLastSeenByControllerId(String controllerId) {
        String query = "UPDATE controllers SET last_seen_at = CURRENT_TIMESTAMP WHERE controller_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId));
    }

    /** Liefert nur freie Controller, die kuerzlich gesehen wurden. */
    public Future<RowSet<Row>> fetchAvailableControllers() {
        String query = "SELECT id, controller_id, controller_type, status, assigned_user_id " +
                "FROM controllers WHERE status = 'FREE' AND last_seen_at >= (CURRENT_TIMESTAMP - INTERVAL 45 SECOND) " +
                "ORDER BY id ASC";
        return jdbcPool.query(query).execute();
    }

    /** Markiert den Controller als OFFLINE (kein Ping/Pong). */
    public Future<RowSet<Row>> setControllerOffline(String controllerId) {
        String query = "UPDATE controllers SET status = 'OFFLINE' WHERE controller_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(controllerId));
    }
}
