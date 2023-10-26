package org.codedefenders.smartassistant.GPTObjects;

public class GPTChoice {
    private Integer index;
    private GPTMessage message;
    private String finishReason;

    public String getMessageContent() {
        return message.getContent();
    }
}
