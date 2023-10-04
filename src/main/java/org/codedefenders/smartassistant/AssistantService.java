package org.codedefenders.smartassistant;

import javax.inject.Inject;

import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.codedefenders.smartassistant.response.objects.ChatGPTRole;

public class AssistantService {
    @Inject
    private ChatGPTRequestDispatcher dispatcher;

    public String sendQuestionWithNoContext(String question) {
        ChatGPTMessage message = new ChatGPTMessage(ChatGPTRole.USER, question);
        return dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
    }

}
