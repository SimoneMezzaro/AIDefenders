package org.codedefenders.model;

public class AssistantUserSettingsEntity {

    private transient Integer id;
    private Integer userId;
    private String username;
    private String email;
    private Integer questionsNumber;
    private SmartAssistantType assistantType;

    public AssistantUserSettingsEntity(Integer id, Integer userId, String username, String email, Integer questionsNumber, SmartAssistantType assistantType) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.questionsNumber = questionsNumber;
        this.assistantType = assistantType;
    }


    public SmartAssistantType getAssistantType() {
        return assistantType;
    }

    public void setAssistantType(SmartAssistantType assistantType) {
        this.assistantType = assistantType;
    }

}
