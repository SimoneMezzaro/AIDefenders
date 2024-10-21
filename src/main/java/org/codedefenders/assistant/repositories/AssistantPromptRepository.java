package org.codedefenders.assistant.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.codedefenders.assistant.entities.AssistantPromptEntity;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

/**
 * This repository provides methods for querying and updating the {@code assistant_prompts} table in the database.
 */
@Transactional
public class AssistantPromptRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantPromptRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantPromptRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Maps a result set from the {@code assistant_prompts} table to an {@link AssistantPromptEntity} objet.
     * @param rs the result set to map
     * @return the {@link AssistantPromptEntity} corresponding to the result set
     * @throws SQLException if a {@link SQLException} occurs while accessing the result set
     */
    private AssistantPromptEntity assistantPromptEntityFromRS(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("ID");
        String prompt = rs.getString("Prompt");
        long timestamp = rs.getTimestamp("Timestamp").toInstant().getEpochSecond();
        Boolean asSeparateContext = rs.getBoolean("As_separate_context");
        Boolean defaultFlag = rs.getBoolean("Default_flag");
        return new AssistantPromptEntity(id, prompt, timestamp, asSeparateContext, defaultFlag);
    }

    /**
     * Gets the latest {@link AssistantPromptEntity}.
     * @return the latest {@link AssistantPromptEntity}
     */
    public Optional<AssistantPromptEntity> getLastPrompt() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, As_separate_context, Default_flag " +
                "FROM assistant_prompts ORDER BY Timestamp DESC LIMIT 1;";
        try {
            return queryRunner.query(query, oneFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Gets the latest {@link AssistantPromptEntity} marked as default.
     * @return the latest {@link AssistantPromptEntity} marked as default
     */
    public Optional<AssistantPromptEntity> getLastDefaultPrompt() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, As_separate_context, Default_flag " +
                "FROM assistant_prompts WHERE Default_flag = 1 ORDER BY Timestamp DESC LIMIT 1;";
        try {
            return queryRunner.query(query, oneFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Gets all {@link AssistantPromptEntity} ordered by timestamp, starting with the latest one.
     * @return all {@link AssistantPromptEntity} ordered by timestamp, starting with the latest one
     */
    public List<AssistantPromptEntity> getAllPrompts() {
        @Language("SQL") String query = "SELECT ID, Prompt, Timestamp, As_separate_context, Default_flag " +
                "FROM assistant_prompts ORDER BY Timestamp DESC;";
        try {
            return queryRunner.query(query, listFromRS(this::assistantPromptEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Stores a new {@link AssistantPromptEntity}.
     * @param assistantPromptEntity the new {@link AssistantPromptEntity} to be stored
     */
    public void storePrompt(@Nonnull AssistantPromptEntity assistantPromptEntity) {
        @Language("SQL") String query = "INSERT INTO assistant_prompts "
                + "(Prompt, As_separate_context, Default_flag) "
                + "VALUES (?, ?, ?);";
        try {
            queryRunner.insert(query, resultSet -> nextFromRS(resultSet, rs -> rs.getInt(1)),
                    assistantPromptEntity.getPrompt(),
                    assistantPromptEntity.getAsSeparateContext(),
                    assistantPromptEntity.getDefaultFlag()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
