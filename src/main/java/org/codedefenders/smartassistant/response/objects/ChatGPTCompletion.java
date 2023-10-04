package org.codedefenders.smartassistant.response.objects;

import java.util.List;

public class ChatGPTCompletion {
        private String id;
        private String object;
        private Integer created;
        private String model;
        private List<ChatGPTChoice> choices;
        private ChatGPTCompletionUsage usage;
}
