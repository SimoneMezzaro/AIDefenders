package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.AssistantUserSettingsEntity;
import org.codedefenders.model.SmartAssistantType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.*;

@Transactional
public class AssistantUserSettingsRepository {

    private static final Logger logger = LoggerFactory.getLogger(AssistantUserSettingsRepository.class);
    private final QueryRunner queryRunner;

    @Inject
    public AssistantUserSettingsRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

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
                SmartAssistantType.valueOf(assistantType));
    }

    // get all users settings
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

    // get settings of given user
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

    // insert or update settings of given user
    public Optional<Integer> updateAssistantUserSettings(AssistantUserSettingsEntity settings) {
        int userId = settings.getUserId();
        int remainingQuestionsDelta = settings.getRemainingQuestionsDelta();
        SmartAssistantType assistantType = settings.getAssistantType();
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
