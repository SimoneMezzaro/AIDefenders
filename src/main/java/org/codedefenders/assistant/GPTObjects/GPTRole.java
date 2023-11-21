package org.codedefenders.assistant.GPTObjects;

import com.google.gson.annotations.SerializedName;

/**
 * This enum represents the role property of a message object for the GPT API. Specifications about the possible roles
 * are available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/create">
 *     https://platform.openai.com/docs/api-reference/chat/create
 * </a>
 */
public enum GPTRole {
    @SerializedName("system") SYSTEM,
    @SerializedName("user") USER,
    @SerializedName("assistant") ASSISTANT
}
