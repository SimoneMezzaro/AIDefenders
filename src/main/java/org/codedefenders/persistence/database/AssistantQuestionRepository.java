package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;

@Transactional
public class AssistantQuestionRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantQuestionRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    private AssistantQuestionEntity assistantQuestionEntityFromRS(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("ID");
        String question = rs.getString("Question");
        String answer = rs.getString("Answer");
        Integer playerId = rs.getInt("Player_ID");
        Boolean useful = rs.getBoolean("Useful");
        if(rs.wasNull()) {
            useful = null;
        }
        return new AssistantQuestionEntity(id, question, answer, playerId, useful);
    }

    // add new question
    public Optional<Integer> storeQuestion(@Nonnull AssistantQuestionEntity assistantQuestionEntity) {
        @Language("SQL") String query = "INSERT INTO assistant_questions "
                + "(Question, Player_ID) "
                + "VALUES (?, ?);";
        try {
            return queryRunner.insert(query, resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                            assistantQuestionEntity.getQuestion(),
                            assistantQuestionEntity.getPlayerId()
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

    // update question with usefulness
    public void updateUsefulness(@Nonnull AssistantQuestionEntity assistantQuestionEntity) {
        @Language("SQL") String query = "UPDATE assistant_questions " +
                "SET useful = ? WHERE ID = ?";
        try {
            int updatedRows = queryRunner.update(query,
                    assistantQuestionEntity.getUseful(),
                    assistantQuestionEntity.getId()
            );
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update assistant answer usefulness");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
