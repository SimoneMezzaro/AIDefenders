package org.codedefenders.smartassistant;

import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.codedefenders.smartassistant.response.objects.ChatGPTRole;

import javax.inject.Inject;

public class AssistantService {
    @Inject
    private ChatGPTRequestDispatcher dispatcher;

    public String sendQuestionWithNoContext(String question) {
        ChatGPTMessage message = new ChatGPTMessage(ChatGPTRole.USER, question);
        return dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
    }

}
