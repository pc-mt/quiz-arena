package com.example.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;

public class DatabaseClient {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseClient.class);
    private static JDBCPool jdbcPool;

    private DatabaseClient() { // Singleton
    }

    public static void initialize(Vertx vertx, JsonObject config) {
        if (jdbcPool != null) {
            logger.warn("DatabaseClient is already initialized!");
            return;
        }

        try {

            logger.info("Pruefe Datenbankkonfiguration: {}", config.encodePrettily());

            // Konfiguration auf Pflichtfelder pruefen.
            validateConfig(config);

            // JDBC-Pool initialisieren.
            jdbcPool = JDBCPool.pool(vertx,
                    new io.vertx.jdbcclient.JDBCConnectOptions()
                            .setJdbcUrl("jdbc:mariadb://" + config.getString("DB_HOST") + ":"
                                    + config.getInteger("DB_PORT") + "/" + config.getString("DB_NAME"))
                            .setUser(config.getString("DB_USER"))
                            .setPassword(config.getString("DB_PASSWORD")),
                    new PoolOptions().setMaxSize(5)
            );

            logger.info("Datenbank-Pool wurde erfolgreich initialisiert.");

            // Verbindung mit einem kurzen Test-Query pruefen.
            testDatabaseConnection();
        } catch (Exception e) {
            logger.error("Initialisierung von DatabaseClient fehlgeschlagen: {}", e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static JDBCPool getInstance() {
        if (jdbcPool == null) {
            throw new IllegalStateException("DatabaseClient ist nicht initialisiert. Bitte zuerst initialize() aufrufen.");
        }
        return jdbcPool;
    }

    private static void validateConfig(JsonObject config) {
        logger.info("Pruefe Datenbankkonfiguration: {}", config.encodePrettily());

        if (!config.containsKey("DB_HOST") || !config.containsKey("DB_PORT")
                || !config.containsKey("DB_NAME") || !config.containsKey("DB_USER")
                || !config.containsKey("DB_PASSWORD")) {
            throw new IllegalArgumentException("Pflichtfelder fuer die Datenbankkonfiguration fehlen.");
        }
    }

    private static void testDatabaseConnection() {
        jdbcPool.query("SELECT 1").execute()
                .onSuccess(rows -> logger.info("Datenbank-Verbindungstest erfolgreich."))
                .onFailure(err -> logger.error("Datenbank-Verbindungstest fehlgeschlagen: {}", err.getMessage()));
    }
}
