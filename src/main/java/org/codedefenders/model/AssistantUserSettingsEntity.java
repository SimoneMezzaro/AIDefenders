package org.codedefenders.model;

public class AssistantUserSettingsEntity {

    private transient Integer id;
    private Integer userId;
    private String username;
    private String email;
    private Integer questionsNumber;
    private Integer remainingQuestions;
    private SmartAssistantType assistantType;
    private Integer remainingQuestionsDelta;

    public AssistantUserSettingsEntity(Integer id, Integer userId, String username, String email, Integer questionsNumber,
                                       Integer remainingQuestions, SmartAssistantType assistantType) {
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

    public SmartAssistantType getAssistantType() {
        return assistantType;
    }

    public void setAssistantType(SmartAssistantType assistantType) {
        this.assistantType = assistantType;
    }

    public Integer getRemainingQuestions() {
        return remainingQuestions;
    }

    public void setRemainingQuestions(Integer remainingQuestions) {
        this.remainingQuestions = remainingQuestions;
    }

    public Integer getRemainingQuestionsDelta() {
        return remainingQuestionsDelta;
    }

    public void setRemainingQuestionsDelta(Integer remainingQuestionsDelta) {
        this.remainingQuestionsDelta = remainingQuestionsDelta;
    }
}
