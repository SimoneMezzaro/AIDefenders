package org.codedefenders.smartassistant;

import java.time.LocalDate;
import java.util.*;

import javax.inject.Inject;

import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.model.AssistantUserSettingsEntity;
import org.codedefenders.model.SmartAssistantType;
import org.codedefenders.persistence.database.AssistantGeneralSettingsRepository;
import org.codedefenders.persistence.database.AssistantQuestionRepository;
import org.codedefenders.persistence.database.AssistantUserSettingsRepository;
import org.codedefenders.smartassistant.exceptions.ChatGPTException;
import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.codedefenders.smartassistant.response.objects.ChatGPTRole;

public class AssistantService {
    @Inject
    private ChatGPTRequestDispatcher dispatcher;
    @Inject
    private AssistantQuestionRepository assistantQuestionRepository;
    @Inject
    private AssistantUserSettingsRepository assistantUserSettingsRepository;
    @Inject
    private AssistantGeneralSettingsRepository assistantGeneralSettingsRepository;

    public AssistantQuestionEntity sendQuestionWithNoContext(AssistantQuestionEntity question, String prompt) throws ChatGPTException {
        Optional<Integer> id = assistantQuestionRepository.storeQuestion(question);
        if(id.isPresent()) {
            question.setId(id.get());
        }
        else {
            //TODO: handle error
        }
        ChatGPTMessage message = new ChatGPTMessage(ChatGPTRole.USER, prompt + "\n\n" + question.getQuestion());
        //TODO: set a size limit to the response (and also to the question?)
        String answer = dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
        question.setAnswer(answer);
        assistantQuestionRepository.updateAnswer(question);
        return question;
    }

    public List<AssistantQuestionEntity> getQuestionsByPlayer(Integer playerId) {
        return assistantQuestionRepository.getQuestionsAndAnswersByPlayer(playerId);
    }

    public void updateAssistantUserSettings(Map<Integer, SmartAssistantType> assistantTypes) {
        assistantTypes.forEach((id, type) -> assistantUserSettingsRepository.updateAssistantUserSettings(id, type));
    }

    public List<AssistantUserSettingsEntity> getAllAssistantUserSettings() {
        return assistantUserSettingsRepository.getAllAssistantUserSettings();
    }

    public Map<LocalDate, Integer> getAmountOfQuestionsInTheLastDays(int daysBack) {
        Map<LocalDate, Integer> dbMap = assistantQuestionRepository.getLastXAmountOfQuestions(daysBack);
        Map<LocalDate, Integer> resultMap = new HashMap<>();
        LocalDate date = LocalDate.now();
        for(int i = 0; i < daysBack; i++) {
            resultMap.put(date, dbMap.getOrDefault(date, 0));
            date = date.minusDays(1);
        }
        return resultMap;
    }

    public Integer getTotalQuestionsAmount() {
        Optional<Integer> amount = assistantQuestionRepository.getTotalQuestionsAmount();
        //TODO: handle error
        return amount.orElse(null);
    }

    public Boolean getAssistantEnabled() {
        Optional<Boolean> enabled = assistantGeneralSettingsRepository.getAssistantEnabled();
        //TODO: handle error
        return enabled.orElse(null);
    }

    public void updateAssistantEnabled(boolean enabled) {
        assistantGeneralSettingsRepository.updateAssistantEnabled(enabled);
    }

    public boolean isAssistantEnabledForUser(int userId) {
        Optional<Boolean> enabled = assistantGeneralSettingsRepository.getAssistantEnabled();
        if(enabled.isEmpty()) {
            //TODO: handle error
            return false;
        }
        if(!enabled.get()) {
            return false;
        }
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            //TODO: handle error
            return false;
        }
        return entity.get().getAssistantType() != SmartAssistantType.NONE;
    }

}
