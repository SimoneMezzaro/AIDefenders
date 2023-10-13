package org.codedefenders.smartassistant;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.persistence.database.AssistantQuestionRepository;
import org.codedefenders.smartassistant.exceptions.ChatGPTException;
import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.codedefenders.smartassistant.response.objects.ChatGPTRole;

public class AssistantService {
    @Inject
    private ChatGPTRequestDispatcher dispatcher;
    @Inject
    private AssistantQuestionRepository assistantQuestionRepository;

    public AssistantQuestionEntity sendQuestionWithNoContext(AssistantQuestionEntity question) throws ChatGPTException {
        Optional<Integer> id = assistantQuestionRepository.storeQuestion(question);
        if(id.isPresent()) {
            question.setId(id.get());
        }
        else {
            //TODO: handle error
        }
        ChatGPTMessage message = new ChatGPTMessage(ChatGPTRole.USER, question.getQuestion());
        //TODO: set a size limit to the response (and also to the question?)
        String answer = dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
        question.setAnswer(answer);
        assistantQuestionRepository.updateAnswer(question);
        return question;
    }

    public List<AssistantQuestionEntity> getQuestionsByPlayer(Integer playerId) {
        return assistantQuestionRepository.getQuestionsAndAnswersByPlayer(playerId);
    }

}
