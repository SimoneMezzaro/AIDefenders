package org.codedefenders.smartassistant.response.objects;

public class ChatGPTMessage {
    private ChatGPTRole role;
    private String content;

    public ChatGPTMessage() { }

    public ChatGPTMessage(ChatGPTRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
