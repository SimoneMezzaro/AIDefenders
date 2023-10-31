package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

@Transactional
public class AssistantQuestionRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantQuestionRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantQuestionRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

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

    private Map<LocalDate, Integer> questionsAmountMapFromRS(ResultSet rs) throws SQLException {
        Map<LocalDate, Integer> map = new HashMap<>();
        while(rs.next()) {
            LocalDate date = rs.getDate(1).toLocalDate();
            int amount = rs.getInt(2);
            map.put(date, amount);
        }
        return map;
    }

    // add new question
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

    // update question with answer
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

    // get all questions with answer of given player
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

    // get amount of questions in the last X days with at least one question
    public Map<LocalDate, Integer> getLastXAmountOfQuestions(int X) {
        @Language("SQL") String query = "SELECT DATE(Timestamp), count(*) FROM assistant_questions " +
                "GROUP BY DATE(Timestamp) ORDER BY DATE(Timestamp) DESC LIMIT ?;";
        try {
            return queryRunner.query(query, this::questionsAmountMapFromRS, X);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    // get total amount of questions (all players)
    public Optional<Integer> getTotalQuestionsAmount() {
        @Language("SQL") String query = "SELECT COUNT(*) FROM assistant_questions;";
        try {
            return queryRunner.query(query, oneFromRS(rs -> rs.getInt(1)));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    // update usefulness of last the last question of given player
    public void updateUsefulnessOfLastQuestionByPlayer(int playerId, boolean usefulness) {
        @Language("SQL") String query = "UPDATE assistant_questions, " +
                "(SELECT id FROM assistant_questions WHERE player_id = ? ORDER BY timestamp DESC LIMIT 1) " +
                "AS last_player_question " +
                "SET assistant_questions.useful = ? " +
                "WHERE assistant_questions.id = last_player_question.id;";
        try {
            int updatedRows = queryRunner.update(query, playerId, usefulness);
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update assistant answer usefulness");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
