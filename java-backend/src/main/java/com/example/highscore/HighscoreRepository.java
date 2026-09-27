package com.example.highscore;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class HighscoreRepository {

    private final JDBCPool jdbcPool;

    public HighscoreRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    // --- Highscore-Query ---
    /**
     * Liefert Highscore-Eintraege nach Punkten absteigend und Zeit aufsteigend.
     * Pro Modus werden maximal 20 Eintraege zurueckgegeben.
     */
    public Future<RowSet<Row>> fetchHighscoresByRoundLength(String roundLength) {
        String query = "SELECT h.user_id, u.username, u.display_name, h.total_points, " +
                "h.total_response_time_ms, h.created_at " +
                "FROM highscores h " +
                "JOIN users u ON u.id = h.user_id " +
                "WHERE h.round_length = ? " +
                "ORDER BY h.total_points DESC, h.total_response_time_ms ASC, h.created_at ASC " +
                "LIMIT 20";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(roundLength));
    }
}
