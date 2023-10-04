package org.codedefenders.smartassistant.response.objects;

public class ChatGPTChoice {
    private Integer index;
    private ChatGPTMessage message;
    private String finishReason;

    public String getMessageContent() {
        return message.getContent();
    }
}
