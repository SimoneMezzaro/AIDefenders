package org.codedefenders.assistant.GPTObjects;

/**
 * This class represents a message object for the GPT API. Specifications about message properties are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/create">
 *     https://platform.openai.com/docs/api-reference/chat/create
 * </a>
 */
public class GPTMessage {
    private GPTRole role;
    private String content;

    public GPTMessage() { }

    public GPTMessage(GPTRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public GPTRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }
}
