package com.example.player;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class PlayerRepository {

    private final JDBCPool jdbcPool;

    public PlayerRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    // --- Nutzer anlegen / laden ---
    public Future<RowSet<Row>> insertUser(String username, String passwordHash, String rfidUid) {
        String query = "INSERT INTO users (username, password_hash, rfid_uid) VALUES (?, ?, ?)";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(username, passwordHash, rfidUid));
    }

    public Future<RowSet<Row>> fetchUserByUsername(String username) {
        String query = "SELECT id, username, password_hash, display_name, profile_photo_url, rfid_uid, created_at FROM users WHERE username = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(username));
    }

    public Future<RowSet<Row>> fetchUserById(Long userId) {
        String query = "SELECT id, rfid_uid FROM users WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId));
    }

    // --- RFID-Queries ---
    public Future<RowSet<Row>> fetchUserByRfidExcludingId(String rfidUid, Long userId) {
        String query = "SELECT id FROM users WHERE rfid_uid = ? AND id != ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(rfidUid, userId));
    }

    public Future<RowSet<Row>> fetchUserByRfid(String rfidUid) {
        String query = "SELECT id, username, display_name, profile_photo_url FROM users WHERE rfid_uid = ? LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(rfidUid));
    }

    public Future<RowSet<Row>> updateUserRfid(String rfidUid, Long userId) {
        String query = "UPDATE users SET rfid_uid = ? WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(rfidUid, userId));
    }

    public Future<RowSet<Row>> clearUserRfid(Long userId) {
        String query = "UPDATE users SET rfid_uid = NULL WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId));
    }

    // --- Profil-Updates ---
    public Future<RowSet<Row>> updateDisplayName(Long userId, String displayName) {
        String query = "UPDATE users SET display_name = ? WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(displayName, userId));
    }

    public Future<RowSet<Row>> updateProfilePhotoUrl(Long userId, String profilePhotoUrl) {
        String query = "UPDATE users SET profile_photo_url = ? WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(profilePhotoUrl, userId));
    }
}
