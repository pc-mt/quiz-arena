package com.example.game;

import java.util.Collections;
import java.util.List;

import com.example.database.DatabaseClient;

import io.vertx.core.Future;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class GameRepository {

    private final JDBCPool jdbcPool;

    public GameRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    public Future<RowSet<Row>> fetchActiveSession() {
        String query = "SELECT id, state, host_user_id, round_length, allow_easy, allow_medium, allow_hard FROM game_sessions " +
                "WHERE state IN ('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS') " +
                "ORDER BY id DESC LIMIT 1";
        return jdbcPool.query(query).execute();
    }

    public Future<RowSet<Row>> assignHostIfMissing(Long sessionId, Long userId) {
        String query = "UPDATE game_sessions SET host_user_id = ? WHERE id = ? AND host_user_id IS NULL";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(userId, sessionId));
    }

    public Future<RowSet<Row>> countExistingCategories(List<Long> categoryIds) {
        String placeholders = String.join(",", Collections.nCopies(categoryIds.size(), "?"));
        String query = "SELECT COUNT(*) AS total FROM categories WHERE id IN (" + placeholders + ")";
        Tuple params = Tuple.tuple();
        categoryIds.forEach(params::addValue);
        return jdbcPool.preparedQuery(query).execute(params);
    }

    public Future<RowSet<Row>> countQuestionsByFilters(List<Long> categoryIds, List<String> difficulties) {
        if (categoryIds.isEmpty() || difficulties.isEmpty()) {
            return jdbcPool.preparedQuery("SELECT 0 AS total").execute(Tuple.tuple());
        }
        String categoryPlaceholders = String.join(",", Collections.nCopies(categoryIds.size(), "?"));
        String difficultyPlaceholders = String.join(",", Collections.nCopies(difficulties.size(), "?"));
        String query = "SELECT COUNT(*) AS total FROM questions " +
                "WHERE is_active = 1 " +
                "AND category_id IN (" + categoryPlaceholders + ") " +
                "AND difficulty IN (" + difficultyPlaceholders + ")";
        Tuple params = Tuple.tuple();
        categoryIds.forEach(params::addValue);
        difficulties.forEach(params::addValue);
        return jdbcPool.preparedQuery(query).execute(params);
    }

    /** Liefert je Kategorie/Schwierigkeit die Anzahl aktiver Fragen fuer die Lobby-Anzeige. */
    public Future<RowSet<Row>> fetchCategoryDifficultyCounts() {
        String query = "SELECT category_id, difficulty, COUNT(*) AS cnt " +
                "FROM questions WHERE is_active = 1 GROUP BY category_id, difficulty";
        return jdbcPool.query(query).execute();
    }

    public Future<RowSet<Row>> updateSessionConfig(Long sessionId, String roundLength,
            boolean allowEasy, boolean allowMedium, boolean allowHard) {
        String query = "UPDATE game_sessions " +
                "SET round_length = ?, allow_easy = ?, allow_medium = ?, allow_hard = ? " +
                "WHERE id = ?";
        return jdbcPool.preparedQuery(query)
                .execute(Tuple.of(roundLength, allowEasy ? 1 : 0, allowMedium ? 1 : 0, allowHard ? 1 : 0, sessionId));
    }

    public Future<RowSet<Row>> clearSessionCategories(Long sessionId) {
        String query = "DELETE FROM game_session_categories WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> insertSessionCategory(Long sessionId, Long categoryId) {
        String query = "INSERT INTO game_session_categories (game_session_id, category_id) VALUES (?, ?)";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, categoryId));
    }

    public Future<RowSet<Row>> countSessionCategories(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_categories WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> countSessionPlayers(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_players WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> countNotReadyPlayers(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_players WHERE game_session_id = ? AND is_ready = 0";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    /** Zaehlt nur verbundene Spieler (Controller-Status ASSIGNED). */
    public Future<RowSet<Row>> countConnectedPlayers(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id AND c.status = 'ASSIGNED' " +
                "WHERE gsp.game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    /** Zaehlt verbundene Spieler, die noch nicht bereit sind. */
    public Future<RowSet<Row>> countNotReadyConnectedPlayers(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id AND c.status = 'ASSIGNED' " +
                "WHERE gsp.game_session_id = ? AND gsp.is_ready = 0";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> startSessionCountdown(Long sessionId) {
        String query = "UPDATE game_sessions SET state = 'COUNTDOWN', started_at = CURRENT_TIMESTAMP " +
                "WHERE id = ? AND state = 'LOBBY'";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> setSessionState(Long sessionId, String newState) {
        String query = "UPDATE game_sessions SET state = ? WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(newState, sessionId));
    }

    public Future<RowSet<Row>> setSessionStateIfCurrent(Long sessionId, String currentState, String newState) {
        String query = "UPDATE game_sessions SET state = ? WHERE id = ? AND state = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(newState, sessionId, currentState));
    }

    public Future<RowSet<Row>> fetchFirstQuestionForSession(Long sessionId) {
        String query = "SELECT q.id, q.question_text, q.correct_option, q.difficulty, c.name AS category_name " +
                "FROM game_sessions gs " +
                "JOIN game_session_categories gsc ON gsc.game_session_id = gs.id " +
                "JOIN questions q ON q.category_id = gsc.category_id " +
                "JOIN categories c ON c.id = q.category_id " +
                "WHERE gs.id = ? " +
                "AND q.is_active = 1 " +
                "AND ((gs.allow_easy = 1 AND q.difficulty = 'EASY') " +
                "OR (gs.allow_medium = 1 AND q.difficulty = 'MEDIUM') " +
                "OR (gs.allow_hard = 1 AND q.difficulty = 'HARD')) " +
                "ORDER BY q.id ASC LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchQuestionOptions(Long questionId) {
        String query = "SELECT option_letter, option_text FROM question_options " +
                "WHERE question_id = ? " +
                "ORDER BY FIELD(option_letter, 'A', 'B', 'C', 'D')";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(questionId));
    }

    public Future<RowSet<Row>> fetchCorrectOptionForQuestion(Long questionId) {
        String query = "SELECT correct_option FROM questions WHERE id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(questionId));
    }

    public Future<RowSet<Row>> insertSessionQuestion(Long sessionId, int questionIndex, Long questionId) {
        String query = "INSERT INTO game_session_questions (game_session_id, question_index, question_id, asked_at) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, questionIndex, questionId));
    }

    public Future<RowSet<Row>> fetchCurrentSessionQuestion(Long sessionId) {
        String query = "SELECT gsq.question_index, gsq.question_id, q.correct_option " +
                "FROM game_session_questions gsq " +
                "JOIN questions q ON q.id = gsq.question_id " +
                "WHERE gsq.game_session_id = ? " +
                "ORDER BY gsq.question_index DESC LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchSessionPlayerByControllerExternalId(Long sessionId, String controllerExternalId) {
        String query = "SELECT gsp.user_id " +
                "FROM game_session_players gsp " +
                "JOIN controllers c ON c.id = gsp.controller_id " +
                "WHERE gsp.game_session_id = ? AND c.controller_id = ? " +
                "LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, controllerExternalId));
    }

    public Future<RowSet<Row>> hasAnswerForQuestion(Long sessionId, Long userId, Long questionId) {
        String query = "SELECT COUNT(*) AS total FROM game_answers " +
                "WHERE game_session_id = ? AND user_id = ? AND question_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, userId, questionId));
    }

    public Future<RowSet<Row>> insertAnswer(Long sessionId, Long userId, Long questionId, String answeredOption) {
        return insertAnswer(sessionId, userId, questionId, answeredOption, null);
    }

    public Future<RowSet<Row>> insertAnswer(Long sessionId, Long userId, Long questionId, String answeredOption, Long botResponseTimeMs) {
        if (botResponseTimeMs != null && botResponseTimeMs > 0) {
            String query = "INSERT INTO game_answers " +
                    "(game_session_id, user_id, question_id, answered_option, response_time_ms, time_bucket, is_correct, points_awarded, created_at) " +
                    "SELECT " +
                    "  ?, ?, ?, ?, " +
                    "  ?, " +
                    "  b.bucket_code, " +
                    "  IF(q.correct_option = ?, 1, 0), " +
                    "  IF(q.correct_option = ?, " +
                    "     (CASE q.difficulty WHEN 'EASY' THEN 1.00 WHEN 'MEDIUM' THEN 2.00 ELSE 3.00 END) * b.factor, " +
                    "     0.00), " +
                    "  CURRENT_TIMESTAMP " +
                    "FROM questions q " +
                    "JOIN game_session_questions gsq ON gsq.game_session_id = ? AND gsq.question_id = q.id " +
                    "JOIN scoring_time_buckets b ON " +
                    "  ? >= b.min_ms " +
                    "  AND (b.max_ms IS NULL OR ? < b.max_ms) " +
                    "WHERE q.id = ? " +
                    "LIMIT 1";
            return jdbcPool.preparedQuery(query)
                    .execute(Tuple.of(
                            sessionId,
                            userId,
                            questionId,
                            answeredOption,
                            botResponseTimeMs,
                            answeredOption,
                            answeredOption,
                            sessionId,
                            botResponseTimeMs,
                            botResponseTimeMs,
                            questionId));
        }

        String query = "INSERT INTO game_answers " +
                "(game_session_id, user_id, question_id, answered_option, response_time_ms, time_bucket, is_correct, points_awarded, created_at) " +
                "SELECT " +
                "  ?, ?, ?, ?, " +
                "  GREATEST(0, CAST(TIMESTAMPDIFF(MICROSECOND, gsq.asked_at, CURRENT_TIMESTAMP) / 1000 AS UNSIGNED)), " +
                "  b.bucket_code, " +
                "  IF(q.correct_option = ?, 1, 0), " +
                "  IF(q.correct_option = ?, " +
                "     (CASE q.difficulty WHEN 'EASY' THEN 1.00 WHEN 'MEDIUM' THEN 2.00 ELSE 3.00 END) * b.factor, " +
                "     0.00), " +
                "  CURRENT_TIMESTAMP " +
                "FROM questions q " +
                "JOIN game_session_questions gsq ON gsq.game_session_id = ? AND gsq.question_id = q.id " +
                "JOIN scoring_time_buckets b ON " +
                "  GREATEST(0, CAST(TIMESTAMPDIFF(MICROSECOND, gsq.asked_at, CURRENT_TIMESTAMP) / 1000 AS UNSIGNED)) >= b.min_ms " +
                "  AND (b.max_ms IS NULL OR GREATEST(0, CAST(TIMESTAMPDIFF(MICROSECOND, gsq.asked_at, CURRENT_TIMESTAMP) / 1000 AS UNSIGNED)) < b.max_ms) " +
                "WHERE q.id = ? " +
                "LIMIT 1";
        return jdbcPool.preparedQuery(query)
                .execute(Tuple.of(
                        sessionId,
                        userId,
                        questionId,
                        answeredOption,
                        answeredOption,
                        answeredOption,
                        sessionId,
                        questionId));
    }

    public Future<RowSet<Row>> countAnswersForQuestion(Long sessionId, Long questionId) {
        String query = "SELECT COUNT(*) AS total FROM game_answers WHERE game_session_id = ? AND question_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId, questionId));
    }

    public Future<RowSet<Row>> fetchEvaluationRows(Long sessionId, Long questionId) {
        String query = "SELECT " +
                "  gsp.user_id, " +
                "  COALESCE(NULLIF(u.display_name, ''), u.username) AS player_name, " +
                "  ga.answered_option, " +
                "  COALESCE(ga.is_correct, 0) AS is_correct, " +
                "  COALESCE(ga.points_awarded, 0.00) AS points_awarded, " +
                "  ga.response_time_ms, " +
                "  ga.time_bucket, " +
                "  stb.factor AS time_factor, " +
                "  (SELECT CASE q.difficulty WHEN 'EASY' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END FROM questions q WHERE q.id = ?) AS base_points " +
                "FROM game_session_players gsp " +
                "JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN game_answers ga ON ga.game_session_id = gsp.game_session_id " +
                "  AND ga.user_id = gsp.user_id " +
                "  AND ga.question_id = ? " +
                "LEFT JOIN scoring_time_buckets stb ON stb.bucket_code = ga.time_bucket " +
                "WHERE gsp.game_session_id = ? " +
                "ORDER BY gsp.user_id ASC";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(questionId, questionId, sessionId));
    }

    public Future<RowSet<Row>> countSessionQuestions(Long sessionId) {
        String query = "SELECT COUNT(*) AS total FROM game_session_questions WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchSessionRoundLength(Long sessionId) {
        String query = "SELECT round_length FROM game_sessions WHERE id = ? LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchNextQuestionForSession(Long sessionId) {
        String query = "SELECT q.id, q.question_text, q.correct_option, q.difficulty, c.name AS category_name " +
                "FROM game_sessions gs " +
                "JOIN game_session_categories gsc ON gsc.game_session_id = gs.id " +
                "JOIN questions q ON q.category_id = gsc.category_id " +
                "JOIN categories c ON c.id = q.category_id " +
                "LEFT JOIN game_session_questions gsq ON gsq.game_session_id = gs.id AND gsq.question_id = q.id " +
                "WHERE gs.id = ? " +
                "AND q.is_active = 1 " +
                "AND gsq.question_id IS NULL " +
                "AND ((gs.allow_easy = 1 AND q.difficulty = 'EASY') " +
                "OR (gs.allow_medium = 1 AND q.difficulty = 'MEDIUM') " +
                "OR (gs.allow_hard = 1 AND q.difficulty = 'HARD')) " +
                "ORDER BY q.id ASC LIMIT 1";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> fetchFinalResults(Long sessionId) {
        String query = "SELECT gsp.user_id, " +
                "COALESCE(NULLIF(u.display_name, ''), u.username) AS player_name, " +
                "COALESCE(SUM(ga.points_awarded), 0) AS total_points, " +
                "COALESCE(SUM(ga.response_time_ms), 0) AS total_response_time_ms, " +
                "COALESCE(SUM(CASE WHEN ga.is_correct = 1 THEN 1 ELSE 0 END), 0) AS correct_count, " +
                "COUNT(ga.id) AS answered_count " +
                "FROM game_session_players gsp " +
                "JOIN users u ON u.id = gsp.user_id " +
                "LEFT JOIN game_answers ga ON ga.game_session_id = gsp.game_session_id AND ga.user_id = gsp.user_id " +
                "WHERE gsp.game_session_id = ? " +
                "GROUP BY gsp.user_id, u.display_name, u.username " +
                "ORDER BY total_points DESC, total_response_time_ms ASC";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    public Future<RowSet<Row>> insertSessionResult(Long sessionId, Long userId, double totalPoints,
            long totalTimeMs, int correctCount, int answeredCount) {
        String query = "INSERT IGNORE INTO game_session_results " +
                "(game_session_id, user_id, total_points, total_response_time_ms, correct_count, answered_count) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        return jdbcPool.preparedQuery(query).execute(
                Tuple.of(sessionId, userId, totalPoints, totalTimeMs, correctCount, answeredCount));
    }

    public Future<RowSet<Row>> insertHighscore(String roundLength, Long userId, Long sessionId,
            double totalPoints, long totalTimeMs) {
        String query = "INSERT INTO highscores " +
                "(round_length, user_id, game_session_id, total_points, total_response_time_ms) " +
                "VALUES (?, ?, ?, ?, ?)";
        return jdbcPool.preparedQuery(query).execute(
                Tuple.of(roundLength, userId, sessionId, totalPoints, totalTimeMs));
    }

    public Future<RowSet<Row>> resetAllPlayersReady(Long sessionId) {
        String query = "UPDATE game_session_players SET is_ready = 0 WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    /** Entfernt alle Antworten der Session, z. B. beim Neustart zur Lobby. */
    public Future<RowSet<Row>> deleteGameAnswersForSession(Long sessionId) {
        String query = "DELETE FROM game_answers WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }

    /** Entfernt alle bereits gezogenen Fragen der Session, damit neu gestartet werden kann. */
    public Future<RowSet<Row>> deleteGameSessionQuestionsForSession(Long sessionId) {
        String query = "DELETE FROM game_session_questions WHERE game_session_id = ?";
        return jdbcPool.preparedQuery(query).execute(Tuple.of(sessionId));
    }
}
