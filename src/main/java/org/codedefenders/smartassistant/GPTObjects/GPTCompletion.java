package org.codedefenders.smartassistant.GPTObjects;

import java.util.List;

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
