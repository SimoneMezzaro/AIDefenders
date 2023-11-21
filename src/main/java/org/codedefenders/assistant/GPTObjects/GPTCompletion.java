package org.codedefenders.assistant.GPTObjects;

import java.util.List;

/**
 * This class represents a chat completion object for the GPT API. Specifications about chat completion properties are
 * available at
 * <a href="https://platform.openai.com/docs/api-reference/chat/object">
 *     https://platform.openai.com/docs/api-reference/chat/object
 * </a>
 */
public class GPTCompletion {
        private String id;
        private String object;
        private Integer created;
        private String model;
        private List<GPTChoice> choices;
        private GPTCompletionUsage usage;

        public GPTChoice getFirstChoice() {
            return choices.get(0);
        }

        public String getFirstChoiceMessageContent() {
            return getFirstChoice().getMessageContent();
        }
}
