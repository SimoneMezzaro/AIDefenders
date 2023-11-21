package org.codedefenders.assistant.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.assistant.entities.AssistantUserSettingsEntity;
import org.codedefenders.assistant.entities.AssistantType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

/**
 * This repository provides methods for querying and updating the {@code assistant_user_settings} table in the database.
 */
@Transactional
public class AssistantUserSettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantUserSettingsRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantUserSettingsRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    /**
     * Maps a result set from the {@code assistant_user_settings} table to an {@link AssistantUserSettingsEntity} objet.
     * @param rs rs the result set to map
     * @return the {@link AssistantUserSettingsEntity} corresponding to the result set
     * @throws SQLException if a {@link SQLException} occurs while accessing the result set
     */
    private AssistantUserSettingsEntity assistantUserSettingsEntityFromRS(ResultSet rs) throws SQLException {
        Integer id = rs.getInt("ID");
        if(rs.wasNull()) {
            id = null;
        }
        Integer userId = rs.getInt("User_ID");
        String username = rs.getString("Username");
        String email = rs.getString("Email");
        Integer questionsNumber = rs.getInt("Questions_number");
        Integer remainingQuestions = rs.getInt("Remaining_questions");
        String assistantType = rs.getString("Assistant_type");
        if(rs.wasNull()) {
            assistantType = "NONE";
        }
        return new AssistantUserSettingsEntity(id, userId, username, email, questionsNumber, remainingQuestions,
                AssistantType.valueOf(assistantType));
    }

    /**
     * Gets the {@link AssistantUserSettingsEntity} for each active user.
     * @return a list containing the {@link AssistantUserSettingsEntity} for each active user
     */
    public List<AssistantUserSettingsEntity> getAllAssistantUserSettings() {
        @Language("SQL") String query = "SELECT a.ID, u.User_ID, u.Username, u.Email, " +
                "a.Questions_number, a.Remaining_questions, a.Assistant_type " +
                "FROM view_valid_users AS u LEFT JOIN assistant_user_settings AS a ON u.User_ID = a.User_ID " +
                "WHERE u.Active = 1 " +
                "ORDER BY u.Username";
        try {
            return queryRunner
                    .query(query, listFromRS(this::assistantUserSettingsEntityFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Gets the {@link AssistantUserSettingsEntity} of a given user.
     * @param userId the id of the given user
     * @return the {@link AssistantUserSettingsEntity} of the given user
     */
    public Optional<AssistantUserSettingsEntity> getAssistantUserSettingsByUserId(int userId) {
        @Language("SQL") String query = "SELECT a.ID, u.User_ID, u.Username, u.Email, " +
                "a.Questions_number, a.Remaining_questions, a.Assistant_type " +
                "FROM view_valid_users AS u LEFT JOIN assistant_user_settings AS a ON u.User_ID = a.User_ID " +
                "WHERE u.User_ID = ? ";
        try {
            return queryRunner
                    .query(query, oneFromRS(this::assistantUserSettingsEntityFromRS), userId);
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    /**
     * Updates the {@link AssistantUserSettingsEntity} of a specific user. If no settings are present in the database
     * for that user, then a new row is inserted for the user and is updated with the given settings.
     * @param settings the {@link AssistantUserSettingsEntity} to be updated
     * @return the id of the updated {@link AssistantUserSettingsEntity}
     */
    public Optional<Integer> updateAssistantUserSettings(AssistantUserSettingsEntity settings) {
        int userId = settings.getUserId();
        int remainingQuestionsDelta = settings.getRemainingQuestionsDelta();
        AssistantType assistantType = settings.getAssistantType();
        @Language("SQL") String query = "INSERT INTO assistant_user_settings (User_ID, Remaining_questions, Assistant_type) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE Remaining_questions = Remaining_questions + ?, Assistant_type = ?;";
        try {
            return queryRunner.insert(query, nextFromRS(rs -> rs.getInt(1)),
                    userId, remainingQuestionsDelta, assistantType.toString(),
                    remainingQuestionsDelta, assistantType.toString());
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

}
