package org.codedefenders.assistant.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.assistant.entities.AssistantQuestionEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

/**
 * This repository provides methods for querying and updating the {@code assistant_questions} table in the database.
 */
@Transactional
public class AssistantQuestionRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantQuestionRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantQuestionRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Maps a result set from the {@code assistant_questions} table to an {@link AssistantQuestionEntity} objet.
     * @param rs the result set to map
     * @return the {@link AssistantQuestionEntity} corresponding to the result set
     * @throws SQLException if a {@link SQLException} occurs while accessing the result set
     */
    private AssistantQuestionEntity assistantQuestionEntityFromRS(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("ID");
        String question = rs.getString("Question");
        String answer = rs.getString("Answer");
        Integer promptId = rs.getInt("Prompt_ID");
        Integer playerId = rs.getInt("Player_ID");
        Boolean useful = rs.getBoolean("Useful");
        if(rs.wasNull()) {
            useful = null;
        }
        return new AssistantQuestionEntity(id, question, answer, playerId, promptId, useful);
    }

    /**
     * Maps a result set containing dates and integers to a Java {@code map}.
     * @param rs the result set to map
     * @return the Java {@code map} corresponding to the result set
     * @throws SQLException if a {@link SQLException} occurs while accessing the result set
     */
    private Map<LocalDate, Integer> questionsAmountMapFromRS(ResultSet rs) throws SQLException {
        Map<LocalDate, Integer> map = new HashMap<>();
        while(rs.next()) {
            LocalDate date = rs.getDate(1).toLocalDate();
            int amount = rs.getInt(2);
            map.put(date, amount);
        }
        return map;
    }

    /**
     * Stores a new {@link AssistantQuestionEntity} containing the question text, the question prompt and the id of the
     * player asking the question.
     * @param assistantQuestionEntity the new {@link AssistantQuestionEntity} to be stored
     * @return the id of the newly stored question
     */
    public Optional<Integer> storeQuestion(@Nonnull AssistantQuestionEntity assistantQuestionEntity) {
        @Language("SQL") String query = "INSERT INTO assistant_questions "
                + "(Question, Player_ID, Prompt_ID) "
                + "VALUES (?, ?, ?);";
        try {
            return queryRunner.insert(query, resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                            assistantQuestionEntity.getQuestion(),
                            assistantQuestionEntity.getPlayerId(),
                            assistantQuestionEntity.getPromptId()
                    );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Updates a question by storing an answer for it.
     * @param assistantQuestionEntity the {@link AssistantQuestionEntity} containing the answer to be stored
     */
    public void updateAnswer(@Nonnull AssistantQuestionEntity assistantQuestionEntity) {
        @Language("SQL") String query = "UPDATE assistant_questions " +
                "SET answer = ? WHERE ID = ?";
        try {
            int updatedRows = queryRunner.update(query,
                    assistantQuestionEntity.getAnswer(),
                    assistantQuestionEntity.getId()
            );
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update assistant answer");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Gets all {@link AssistantQuestionEntity} of the given player containing valid answers, ordered by timestamp.
     * @param playerId the id of the player who made the questions to retrieve
     * @return a list of the questions with valid answers made by the player and ordered by timestamp
     */
    public List<AssistantQuestionEntity> getQuestionsAndAnswersByPlayer(int playerId) {
        @Language("SQL") String query = "SELECT * FROM assistant_questions " +
                "WHERE player_ID = ? AND answer IS NOT NULL ORDER BY timestamp;";
        try {
            return queryRunner
                    .query(query, listFromRS(this::assistantQuestionEntityFromRS), playerId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Starting from the current day and going backwards, this method retrieves the number of questions made in each day
     * until it finds an amount of days with at least one questions equal to {@code numberOfDays}. The method returns
     * all the days with at least one questions found in this way.
     * @param numberOfDays the number of days with at least one question to be returned
     * @return a map containing each day with at least one question and the amount of questions made in that day
     */
    public Map<LocalDate, Integer> getAmountOfQuestionsInTheLastNonEmptyDays(int numberOfDays) {
        @Language("SQL") String query = "SELECT DATE(Timestamp), count(*) FROM assistant_questions " +
                "GROUP BY DATE(Timestamp) ORDER BY DATE(Timestamp) DESC LIMIT ?;";
        try {
            return queryRunner.query(query, this::questionsAmountMapFromRS, numberOfDays);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Gets the total amount of questions sent to the assistant.
     * @return the total amount of questions sent to the assistant
     */
    public Optional<Integer> getTotalQuestionsAmount() {
        @Language("SQL") String query = "SELECT COUNT(*) FROM assistant_questions;";
        try {
            return queryRunner.query(query, oneFromRS(rs -> rs.getInt(1)));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Updates the latest question sent by the given player storing a feedback for it.
     * @param playerId the id of the player who made the question
     * @param usefulness {@code true} if the question has been useful; {@code false} otherwise
     * @return {@code true} if the update was applied successfully; {@code false} if no question to update was found for
     * the given player
     */
    public boolean updateUsefulnessOfLastQuestionByPlayer(int playerId, boolean usefulness) {
        @Language("SQL") String query = "UPDATE assistant_questions, " +
                "(SELECT id FROM assistant_questions WHERE player_id = ? ORDER BY timestamp DESC LIMIT 1) " +
                "AS last_player_question " +
                "SET assistant_questions.useful = ? " +
                "WHERE assistant_questions.id = last_player_question.id;";
        try {
            int updatedRows = queryRunner.update(query, playerId, usefulness);
            if (updatedRows != 1) {
                return false;
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
        return true;
    }

}
