package org.codedefenders.smartassistant;

import java.time.LocalDate;
import java.util.*;

import javax.inject.Inject;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.model.AssistantPromptEntity;
import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.model.AssistantUserSettingsEntity;
import org.codedefenders.model.SmartAssistantType;
import org.codedefenders.persistence.database.AssistantGeneralSettingsRepository;
import org.codedefenders.persistence.database.AssistantQuestionRepository;
import org.codedefenders.persistence.database.AssistantUserSettingsRepository;
import org.codedefenders.smartassistant.GPTObjects.GPTMessage;
import org.codedefenders.smartassistant.GPTObjects.GPTRole;
import org.codedefenders.smartassistant.exceptions.GPTException;
import org.codedefenders.transaction.Transactional;

public class AssistantService {
    @Inject
    private GPTRequestDispatcher dispatcher;
    @Inject
    private AssistantPromptService assistantPromptService;
    @Inject
    private AssistantQuestionRepository assistantQuestionRepository;
    @Inject
    private AssistantUserSettingsRepository assistantUserSettingsRepository;
    @Inject
    private AssistantGeneralSettingsRepository assistantGeneralSettingsRepository;

    // ______USER______

    public AssistantQuestionEntity sendQuestion(AssistantQuestionEntity question, AbstractGame game) throws GPTException {
        AssistantPromptEntity prompt = assistantPromptService.getLastPrompt();
        question.setPromptId(prompt.getID());
        Optional<Integer> questionId = assistantQuestionRepository.storeQuestion(question);
        if(questionId.isPresent()) {
            question.setId(questionId.get());
        } else {
            //TODO: handle error
        }
        String answer;
        if(prompt.getAsSeparateContext()) {
            List<GPTMessage> messages = new ArrayList<>();
            messages.add(new GPTMessage(GPTRole.SYSTEM, assistantPromptService.buildPromptText(prompt, game)));
            messages.add(new GPTMessage(GPTRole.USER, question.getQuestion()));
            answer = dispatcher.sendChatCompletionRequestWithContext(messages).getFirstChoiceMessageContent();
        } else {
            GPTMessage message = new GPTMessage(GPTRole.USER,
                    assistantPromptService.buildPromptTextWithQuestion(prompt, question.getQuestion(), game));
            answer = dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
        }
        question.setAnswer(answer);
        assistantQuestionRepository.updateAnswer(question);
        return question;
    }

    public List<AssistantQuestionEntity> getQuestionsByPlayer(Integer playerId) {
        return assistantQuestionRepository.getQuestionsAndAnswersByPlayer(playerId);
    }

    public void updateQuestionFeedback(int playerId, boolean usefulness) {
        assistantQuestionRepository.updateUsefulnessOfLastQuestionByPlayer(playerId, usefulness);
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

    public int getRemainingQuestionsForUser(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            //TODO: handle error
            return 0;
        }
        return entity.get().getRemainingQuestions();
    }

    @Transactional
    public boolean checkAndDecrementRemainingQuestions(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            //TODO: handle error
            return false;
        }
        AssistantUserSettingsEntity settings = entity.get();
        if(settings.getRemainingQuestions() > 0) {
            settings.setRemainingQuestionsDelta(-1);
            assistantUserSettingsRepository.updateAssistantUserSettings(settings);
            return true;
        }
        return false;
    }

    @Transactional
    public void incrementRemainingQuestions(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            //TODO: handle error
            return;
        }
        AssistantUserSettingsEntity settings = entity.get();
        settings.setRemainingQuestionsDelta(1);
        assistantUserSettingsRepository.updateAssistantUserSettings(settings);
    }

    // ______ADMIN______

    public List<AssistantUserSettingsEntity> getAllAssistantUserSettings() {
        return assistantUserSettingsRepository.getAllAssistantUserSettings();
    }

    public void updateAssistantUserSettings(List<AssistantUserSettingsEntity> usersSettings) {
        for(AssistantUserSettingsEntity userSettings : usersSettings) {
            assistantUserSettingsRepository.updateAssistantUserSettings(userSettings);
        }
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

}
