package org.codedefenders.assistant.services;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codedefenders.assistant.GPTObjects.GPTMessage;
import org.codedefenders.assistant.GPTObjects.GPTRole;
import org.codedefenders.assistant.entities.AssistantPromptEntity;
import org.codedefenders.assistant.entities.AssistantQuestionEntity;
import org.codedefenders.assistant.entities.AssistantType;
import org.codedefenders.assistant.entities.AssistantUserSettingsEntity;
import org.codedefenders.assistant.exceptions.GPTException;
import org.codedefenders.assistant.repositories.AssistantGeneralSettingsRepository;
import org.codedefenders.assistant.repositories.AssistantQuestionRepository;
import org.codedefenders.assistant.repositories.AssistantUserSettingsRepository;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;

/**
 * This class exposes methods to send and retrieve assistant data and to modify users and general settings related to
 * the assistant. In particular, it is possible to send new questions to the assistant and get answers, collect all
 * previously asked questions, collect and update the settings of one or more users, update the status of the assistant
 * and retrieve the status of the assistant for a specific user.
 */
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
    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);

    // ______USER_RELATED_METHODS______

    /**
     * Sends a question made by a player to GPT API. The question is sent together with the latest saved prompt in which
     * additional information is stored. This information includes code under test, mutants code or description and
     * tests code. Returns the answer received from the API.
     * @param question the question to be sent
     * @param game the game, containing the code under test, where the question was made
     * @param mutants a map of mutants that have been tagged in the question. For each mutant the map contains a flag
     *                which is {@code true} if the mutant code is visible to the player who made the question, and it is
     *                {@code false} if only the mutant description is visible to the player
     * @param tests a list of tests that have been tagged in the question and are visible to the player who made the
     *              question
     * @return an {@link AssistantQuestionEntity} containing the answer received from the API
     * @throws GPTException if an error occurs while sending the request
     * @throws RuntimeException if an error occurs while saving the question
     */
    public AssistantQuestionEntity sendQuestion(AssistantQuestionEntity question, AbstractGame game,
                                                Map<Mutant, Boolean> mutants, List<Test> tests) throws GPTException {
        AssistantPromptEntity prompt = assistantPromptService.getLastPrompt();
        question.setPromptId(prompt.getID());
        Optional<Integer> questionId = assistantQuestionRepository.storeQuestion(question);
        if(questionId.isPresent()) {
            question.setId(questionId.get());
        } else {
            logger.error("Unable to save player question in the database");
            throw new RuntimeException("Unable to save player question in the database");
        }
        String answer;
        if(prompt.getAsSeparateContext()) {
            List<GPTMessage> messages = new ArrayList<>();
            messages.add(new GPTMessage(GPTRole.SYSTEM, assistantPromptService.buildPromptText(prompt, game, mutants, tests)));
            messages.add(new GPTMessage(GPTRole.USER, question.getQuestion()));
            answer = dispatcher.sendChatCompletionRequestWithContext(messages).getFirstChoiceMessageContent();
        } else {
            GPTMessage message = new GPTMessage(GPTRole.USER,
                    assistantPromptService.buildPromptTextWithQuestion(prompt, question.getQuestion(), game, mutants, tests));
            answer = dispatcher.sendChatCompletionRequest(message).getFirstChoiceMessageContent();
        }
        question.setAnswer(answer);
        assistantQuestionRepository.updateAnswer(question);
        return getQuestionWithoutHiddenAnswerParts(question);
    }

    /**
     * Gets all the questions made by a given player with the relative answers, ordered by timestamp.
     * @param playerId the id of the player who made the questions
     * @return all the questions made by a given player with the relative answers, ordered by timestamp
     */
    public List<AssistantQuestionEntity> getQuestionsByPlayer(Integer playerId) {
        return assistantQuestionRepository.getQuestionsAndAnswersByPlayer(playerId)
                .stream().map(this::getQuestionWithoutHiddenAnswerParts).collect(Collectors.toList());
    }

    /**
     * Creates a copy of a given question, but with a filtered answer. The answer of the new copy does not contain the
     * field {@code code} if the given question had {@code showAnswerCode} flag equal to {@code true}. If the flag is
     * {@code false} or if the answer is not in JSON format the new answer will be equal to the old one.
     * @param question the question containing the answer to be filtered
     * @return a copy of the given question containing the filtered answer
     */
    private AssistantQuestionEntity getQuestionWithoutHiddenAnswerParts(AssistantQuestionEntity question) {
        String answer = question.getAnswer();
        if(answer != null && !question.getShowAnswerCode()) {
            Gson gson = new Gson();
            TypeAdapter<JsonObject> strictAdapter = gson.getAdapter(JsonObject.class);
            try {
                JsonObject jsonObj = strictAdapter.fromJson(answer);
                jsonObj.remove("code");
                answer = gson.toJson(jsonObj);
            } catch(Exception e) {
                answer = question.getAnswer();
            }
        }
        return new AssistantQuestionEntity(
                question.getId(),
                question.getQuestion(),
                answer,
                question.getShowAnswerCode(),
                question.getPlayerId(),
                question.getPromptId(),
                question.getUseful()
        );
    }

    /**
     * Updates the feedback of the latest question sent by the given player.
     * @param playerId the id of the player who made the question
     * @param usefulness {@code true} if the question has been useful; {@code false} otherwise
     * @return {@code true} if the update was applied successfully; {@code false} if no question to update was found for
     * the given player
     */
    public boolean updateQuestionFeedback(int playerId, boolean usefulness) {
        return assistantQuestionRepository.updateUsefulnessOfLastQuestionByPlayer(playerId, usefulness);
    }

    /**
     * Checks whether the assistant is enabled for the given user. The assistant is enabled for a user if it is
     * generally enabled and either the user has {@link AssistantType} different from {@link AssistantType#NONE} or the
     * assistant is enabled for all players in the given game.
     * @param userId the id of the given user
     * @param game a game joined by the given user
     * @return {@code true} if the assistant is enabled for the given user; {@code false} otherwise
     * @throws IllegalStateException if the method is unable to retrieve general or user settings
     */
    public boolean isAssistantEnabledForUser(int userId, AbstractGame game) {
        Optional<Boolean> enabled = assistantGeneralSettingsRepository.getAssistantEnabled();
        if(enabled.isEmpty()) {
            logger.error("Unable to retrieve the value of assistant enabled setting");
            throw new IllegalStateException("Unable to retrieve the value of assistant enabled setting");
        }
        if(!enabled.get()) {
            return false;
        }
        if(game.isAssistantEnabled()) {
            return true;
        }
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            logger.error("Unable to retrieve the assistant type for user " + userId);
            throw new IllegalStateException("Unable to retrieve the assistant type for user " + userId);
        }
        return entity.get().getAssistantType() != AssistantType.NONE;
    }

    /**
     * Gets the amount of questions remaining to the given user.
     * @param userId the id of the given user
     * @return the amount of questions remaining to the given user
     * @throws IllegalStateException if the method is unable to retrieve user settings
     */
    public int getRemainingQuestionsForUser(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            logger.error("Unable to retrieve remaining questions for user " + userId);
            throw new IllegalStateException("Unable to retrieve remaining questions for user " + userId);
        }
        return entity.get().getRemainingQuestions();
    }

    /**
     * Checks if a given user has a positive amount of remaining questions. If yes this amount is decremented by one.
     * @param userId the id of the given user
     * @return {@code true} if the given user had remaining questions before the decrement; {@code false} otherwise
     * @throws IllegalStateException if the method is unable to retrieve user settings
     */
    @Transactional
    public boolean checkAndDecrementRemainingQuestions(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            logger.error("Unable to retrieve remaining questions for user " + userId);
            throw new IllegalStateException("Unable to retrieve remaining questions for user " + userId);
        }
        AssistantUserSettingsEntity settings = entity.get();
        if(settings.getRemainingQuestions() > 0) {
            settings.setRemainingQuestionsDelta(-1);
            assistantUserSettingsRepository.updateAssistantUserSettings(settings);
            return true;
        }
        return false;
    }

    /**
     * Increments the amount of remaining questions of a given user by one.
     * @param userId the id of the given user
     * @throws IllegalStateException if the method is unable to retrieve user settings
     */
    @Transactional
    public void incrementRemainingQuestions(int userId) {
        Optional<AssistantUserSettingsEntity> entity = assistantUserSettingsRepository.getAssistantUserSettingsByUserId(userId);
        if(entity.isEmpty()) {
            logger.error("Unable to retrieve remaining questions for user " + userId);
            throw new IllegalStateException("Unable to retrieve remaining questions for user " + userId);
        }
        AssistantUserSettingsEntity settings = entity.get();
        settings.setRemainingQuestionsDelta(1);
        assistantUserSettingsRepository.updateAssistantUserSettings(settings);
    }

    // ______ADMIN_RELATED_METHODS______

    /**
     * Gets the settings of each active user.
     * @return a list containing the settings of each active user
     */
    public List<AssistantUserSettingsEntity> getAllAssistantUserSettings() {
        return assistantUserSettingsRepository.getAllAssistantUserSettings();
    }

    /**
     * Updates the settings of each given user.
     * @param usersSettings a list of the settings to be updated for each user
     */
    public void updateAssistantUserSettings(List<AssistantUserSettingsEntity> usersSettings) {
        for(AssistantUserSettingsEntity userSettings : usersSettings) {
            assistantUserSettingsRepository.updateAssistantUserSettings(userSettings);
        }
    }

    /**
     * For each day in the given range, gets the amount of questions made in that day. The range always starts from
     * today and goes back of the amount of days specified by the parameter.
     * @param numberOfDays the number of days included in the range. For example: if {@code numberOdDays} is 1 the
     *                     range will only contain the current day; if {@code numberOdDays} is 2 the range will contain
     *                     both the current day and the day before
     * @return a map containing for each day in the range the amount of questions made in that day
     */
    public Map<LocalDate, Integer> getAmountOfQuestionsInTheLastDays(int numberOfDays) {
        Map<LocalDate, Integer> dbMap = assistantQuestionRepository.getAmountOfQuestionsInTheLastNonEmptyDays(numberOfDays);
        Map<LocalDate, Integer> resultMap = new HashMap<>();
        LocalDate date = LocalDate.now();
        for(int i = 0; i < numberOfDays; i++) {
            resultMap.put(date, dbMap.getOrDefault(date, 0));
            date = date.minusDays(1);
        }
        return resultMap;
    }

    /**
     * Gets the total amount of questions sent to the assistant.
     * @return the total amount of questions sent to the assistant
     * @throws RuntimeException if the method is unable to retrieve the total amount of questions
     */
    public Integer getTotalQuestionsAmount() {
        Optional<Integer> amount = assistantQuestionRepository.getTotalQuestionsAmount();
        if(amount.isEmpty()) {
            logger.error("Unable to retrieve total amount of questions for the assistant");
            throw new RuntimeException("Unable to retrieve total amount of questions for the assistant");
        }
        return amount.get();
    }

    /**
     * Returns weather the assistant in enabled or not.
     * @return {@code true} if the assistant is enabled and {@code false} if the assistant is disabled
     * @throws IllegalStateException if the method is unable to retrieve general settings
     */
    public Boolean getAssistantEnabled() {
        Optional<Boolean> enabled = assistantGeneralSettingsRepository.getAssistantEnabled();
        if(enabled.isEmpty()) {
            logger.error("Unable to retrieve the value of assistant enabled setting");
            throw new IllegalStateException("Unable to retrieve the value of assistant enabled setting");
        }
        return enabled.get();
    }

    /**
     * Enables or disables the assistant.
     * @param enabled {@code true} if the assistant should be enabled and {@code false} if the assistant should be
     *                disabled
     */
    public void updateAssistantEnabled(boolean enabled) {
        assistantGeneralSettingsRepository.updateAssistantEnabled(enabled);
    }

}
