package org.codedefenders.assistant.repositories;

import java.sql.SQLException;
import java.util.Optional;

import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;

/**
 * This repository provides methods for querying and updating the {@code assistant_general_settings} table in the
 * database.
 */
@Transactional
public class AssistantGeneralSettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantGeneralSettingsRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantGeneralSettingsRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Gets the value of the {@code ASSISTANT_ENABLED} setting.
     * @return the value of the {@code ASSISTANT_ENABLED} setting
     */
    public Optional<Boolean> getAssistantEnabled() {
        @Language("SQL") String query = "SELECT Value FROM assistant_general_settings WHERE Name = 'ASSISTANT_ENABLED';";
        try {
            return queryRunner.query(query, resultSet -> nextFromRS(resultSet, rs -> rs.getBoolean(1)));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Updates the value of the {@code ASSISTANT_ENABLED} setting.
     * @param enabled the value to be assigned to the {@code ASSISTANT_ENABLED} setting
     */
    public void updateAssistantEnabled(boolean enabled) {
        @Language("SQL") String query = "UPDATE assistant_general_settings SET Value = ? WHERE Name = 'ASSISTANT_ENABLED';";
        try {
            int updatedRows = queryRunner.update(query, enabled);
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update ASSISTANT_ENABLED setting");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
