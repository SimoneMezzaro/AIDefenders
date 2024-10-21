package org.codedefenders.assistant.entities;

/**
 * This class represents a prompt entity in the database. A prompt is a template used to build a message to be sent to
 * GPT together with the player question. The template allows to include addition information to the question such as
 * the class under test or the code of some mutants and tests.
 * <p>
 * A prompt entity stores the text of the template, the timestamp of its creation, weather it will embed the question
 * or send it in a separate message and weather it is a default prompt.
 */
public class AssistantPromptEntity {

    private transient Integer ID;
    private long timestamp;
    private final String prompt;
    private final Boolean asSeparateContext;
    private final Boolean defaultFlag;

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
