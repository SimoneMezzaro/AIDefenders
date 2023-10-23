package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.AssistantPromptEntity;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

@Transactional
public class AssistantPromptRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantPromptRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    private AssistantPromptEntity assistantPromptEntityFromRS(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("ID");
        String prompt = rs.getString("Prompt");
        long timestamp = rs.getTimestamp("Timestamp").toInstant().getEpochSecond();
        Boolean defaultFlag = rs.getBoolean("Default_flag");
        return new AssistantPromptEntity(id, prompt, timestamp, defaultFlag);
    }

    public Optional<AssistantPromptEntity> getLastPrompt() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, Default_flag FROM assistant_prompts " +
                "ORDER BY Timestamp DESC LIMIT 1;";
        try {
            return queryRunner.query(query, oneFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<AssistantPromptEntity> getLastDefaultPrompt() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, Default_flag FROM assistant_prompts WHERE Default_flag = 1 " +
                "ORDER BY Timestamp DESC LIMIT 1;";
        try {
            return queryRunner.query(query, oneFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<AssistantPromptEntity> getAllPrompts() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, Default_flag FROM assistant_prompts " +
                "ORDER BY Timestamp DESC;";
        try {
            return queryRunner.query(query, listFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<Integer> storePrompt(@Nonnull AssistantPromptEntity assistantPromptEntity) {
        @Language("SQL") String query = "INSERT INTO assistant_prompts "
                + "(Prompt, Default_flag) "
                + "VALUES (?, ?);";
        try {
            return queryRunner.insert(query, resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                    assistantPromptEntity.getPrompt(),
                    assistantPromptEntity.getDefaultFlag()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
