package org.codedefenders.smartassistant.GPTObjects;

public class GPTMessage {
    private GPTRole role;
    private String content;

    public GPTMessage() { }

    public GPTMessage(GPTRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
