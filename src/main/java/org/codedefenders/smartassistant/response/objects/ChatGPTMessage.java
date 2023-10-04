package org.codedefenders.smartassistant.response.objects;

public class ChatGPTMessage {
    private final ChatGPTRole role;
    private final String content;

    public ChatGPTMessage(ChatGPTRole role, String content) {
        this.role = role;
        this.content = content;
    }

}
