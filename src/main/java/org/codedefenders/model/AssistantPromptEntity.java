package org.codedefenders.model;

public class AssistantPromptEntity {

    private transient Integer ID;
    private long timestamp;
    private String prompt;
    private Boolean asSeparateContext;
    private Boolean defaultFlag;

    public AssistantPromptEntity(String prompt, Boolean asSeparateContext, Boolean defaultFlag) {
        this.prompt = prompt;
        this.asSeparateContext = asSeparateContext;
        this.defaultFlag = defaultFlag;
    }

    public AssistantPromptEntity(Integer ID, String prompt, long timestamp, Boolean asSeparateContext, Boolean defaultFlag) {
        this.ID = ID;
        this.prompt = prompt;
        this.timestamp = timestamp;
        this.asSeparateContext = asSeparateContext;
        this.defaultFlag = defaultFlag;
    }

    public Integer getID() {
        return ID;
    }

    public String getPrompt() {
        return prompt;
    }

    public Boolean getAsSeparateContext() {
        return asSeparateContext;
    }

    public Boolean getDefaultFlag() {
        return defaultFlag;
    }

    @Override
    public String toString() {
        return timestamp + "," + prompt + "," + asSeparateContext + "," + defaultFlag;
    }

}
