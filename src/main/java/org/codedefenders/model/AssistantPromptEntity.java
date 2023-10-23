package org.codedefenders.model;

public class AssistantPromptEntity {

    private transient Integer ID;
    private long timestamp;
    private String prompt;
    private Boolean defaultFlag;

    public AssistantPromptEntity(String prompt, Boolean defaultFlag) {
        this.prompt = prompt;
        this.defaultFlag = defaultFlag;
    }

    public AssistantPromptEntity(Integer ID, String prompt, long timestamp, Boolean defaultFlag) {
        this.ID = ID;
        this.prompt = prompt;
        this.timestamp = timestamp;
        this.defaultFlag = defaultFlag;
    }

    public String getPrompt() {
        return prompt;
    }

    public Boolean getDefaultFlag() {
        return defaultFlag;
    }

    @Override
    public String toString() {
        return timestamp + "," + prompt + "," + defaultFlag;
    }

}
