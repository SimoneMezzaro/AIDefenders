package org.codedefenders.smartassistant;

import javax.inject.Inject;

import org.codedefenders.smartassistant.exceptions.ChatGPTException;
import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.codedefenders.smartassistant.response.objects.ChatGPTRole;

public class AssistantService {
    @Inject
    private ChatGPTRequestDispatcher dispatcher;

    public String sendQuestionWithNoContext(String question) throws ChatGPTException {
        ChatGPTMessage message = new ChatGPTMessage(ChatGPTRole.USER, question);
        //TODO: set a size limit to the response
        return dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
    }

}
