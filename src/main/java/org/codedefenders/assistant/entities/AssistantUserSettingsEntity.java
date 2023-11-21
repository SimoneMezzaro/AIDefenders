package org.codedefenders.assistant.entities;

/**
 * This class represents a user settings entity in the database. A user settings entity stores user's basic information,
 * the total number of questions asked by a user, the remaining amount of questions that a user can ask and the type of
 * assistant assigned to the user.
 */
public class AssistantUserSettingsEntity {

    private transient Integer id;
    private Integer userId;
    private final String username;
    private final String email;
    private final Integer questionsNumber;
    private final Integer remainingQuestions;
    private final AssistantType assistantType;

    /**
     * This field stores the amount of questions to be added (if positive) or removed (if negative) from the remaining
     * questions of the user. It is not stored in the database.
     */
    private Integer remainingQuestionsDelta;

    public AssistantUserSettingsEntity(Integer id, Integer userId, String username, String email, Integer questionsNumber,
                                       Integer remainingQuestions, AssistantType assistantType) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.questionsNumber = questionsNumber;
        this.remainingQuestions = remainingQuestions;
        this.assistantType = assistantType;
        this.remainingQuestionsDelta = 0;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public Integer getQuestionNumber() {
        return questionsNumber;
    }

    public Integer getRemainingQuestions() {
        return remainingQuestions;
    }

    public AssistantType getAssistantType() {
        return assistantType;
    }

    public Integer getRemainingQuestionsDelta() {
        return remainingQuestionsDelta;
    }

    public void setRemainingQuestionsDelta(Integer remainingQuestionsDelta) {
        this.remainingQuestionsDelta = remainingQuestionsDelta;
    }
}
