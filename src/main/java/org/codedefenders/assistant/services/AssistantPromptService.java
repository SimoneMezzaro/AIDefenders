package org.codedefenders.assistant.services;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.codedefenders.analysis.gameclass.MethodDescription;
import org.codedefenders.assistant.entities.AssistantPromptEntity;
import org.codedefenders.assistant.repositories.AssistantPromptRepository;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.dto.MutantDTO;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameAccordionMapping;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.service.game.GameService;
import org.codedefenders.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods to save and retrieve prompts, together with some utility methods to parse prompts.
 */
public class AssistantPromptService {

    @Inject
    private AssistantPromptRepository assistantPromptRepository;
    @Inject
    private CodeDefendersAuth login;
    @Inject
    GameService gameService;
    private static final Logger logger = LoggerFactory.getLogger(AssistantPromptService.class);

    /**
     * Parses a given prompt and builds an enriched prompt by substituting each placeholder in the given prompt with
     * actual information from a game. This information includes the code under test, mutants code or modified method,
     * tests code and the text of a question made by a player for the assistant.
     * @param prompt the prompt to be parsed ad enriched
     * @param question the assistant question to be embedded in the prompt
     * @param game the game, containing the code under test, where the question was made
     * @param mutants a map containing mutants to be added to the prompt. For each mutant the map contains a flag which
     *                is {@code true} if the code of the mutant should be included in the prompt and it is {@code false}
     *                if the name of the method modified by the mutant should be included instead
     * @param tests a list containing tests whose code should be added to the prompt
     * @return the newly built enriched prompt
     */
    public String buildPromptTextWithQuestion(AssistantPromptEntity prompt, String question, AbstractGame game,
                                              Map<Mutant, Boolean> mutants, List<Test> tests) {
        String regex = "<mutants\\s*\\(.*\\)>|<tests\\s*\\(.*\\)>|<class_under_test>|<user_question>"; // matches all the possible
                                                                                                       // placeholders in a prompt
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(prompt.getPrompt());
        String extendedPrompt = "_" + prompt.getPrompt() + "_"; // padding to correctly split the prompt
        String[] promptBlocks = extendedPrompt.split(regex); // splits the prompt in blocks separated by placeholders
        List<String> placeholders = new ArrayList<>();
        while(matcher.find()) { // makes a list of all placeholders in the prompt
            placeholders.add(matcher.group());
        }

        // Populates a map containing for each mutant the name of the method modified by the mutant
        List<MutantDTO> mutantList = mutants.keySet()
                .stream()
                .map(m -> gameService.getMutant(login.getUserId(), m))
                .collect(Collectors.toList());
        List<MethodDescription> methodDescriptions = game.getCUT().getMethodDescriptions();
        GameAccordionMapping mapping = GameAccordionMapping.computeForMutants(methodDescriptions, mutantList);
        Map<Integer, String> mutantMethodMap = new HashMap<>();
        for(Integer mutantId : mapping.elementsOutsideMethods) {
            mutantMethodMap.put(mutantId, null); // null is used to indicate mutants outside methods
        }
        for (MethodDescription description : methodDescriptions) {
            for(Integer mutantId : mapping.elementsPerMethod.get(description)) {
                mutantMethodMap.put(mutantId, description.getDescription());
            }
        }

        // builds a string containing all mutants information to be added to the prompt
        StringBuilder mutantsString = new StringBuilder();
        for(Mutant m : mutants.keySet()) {
            mutantsString.append("@mutant").append(m.getId()).append(" ");
            if(mutants.get(m)) {
                mutantsString.append("has code:\n").append(m.getAsString());
            } else {
                String method = mutantMethodMap.get(m.getId());
                if(method == null) {
                    mutantsString.append("modifies code outside methods.");
                } else {
                    mutantsString.append("modifies the method ").append(method).append(".");
                }
            }
            mutantsString.append("\n\n");
        }

        // builds a string containing all tests information to be added to the prompt
        StringBuilder testsString = new StringBuilder();
        for(Test t : tests) {
            testsString.append("@test").append(t.getId()).append(" has code:\n").append(t.getAsString()).append("\n\n");
        }

        // builds the final prompt by appending the prompt blocks and substituting each placeholder between blocks with
        // the additional content
        StringBuilder promptBuilder = new StringBuilder(promptBlocks[0].substring(1)); // appends first block without padding
        for(int i = 0; i < placeholders.size(); i++) {
            String placeholder = placeholders.get(i);
            if(placeholder.startsWith("mutants", 1) && !mutants.isEmpty()) {
                promptBuilder.append(placeholder, "mutants".length() + 2, placeholder.length() - 2)
                        .append("\n\n")
                        .append(mutantsString);
            } else if(placeholder.startsWith("tests", 1) && !tests.isEmpty()) {
                promptBuilder.append(placeholder, "tests".length() + 2, placeholder.length() - 2)
                        .append("\n\n")
                        .append(testsString);
            } else if(placeholder.equals("<class_under_test>")) {
                promptBuilder.append(game.getCUT().getSourceCode());
            } else if(placeholder.equals("<user_question>")) {
                promptBuilder.append(question);
            }
            promptBuilder.append(promptBlocks[i + 1]);
        }
        promptBuilder.deleteCharAt(promptBuilder.length() - 1); // removes padding at the end
        return promptBuilder.toString();
    }

    /**
     * Parses a given prompt and builds an enriched prompt by substituting each placeholder in the given prompt with
     * actual information from a game. This information includes the code under test, mutants code or modified method
     * and tests code.
     * @param prompt the prompt to be parsed ad enriched
     * @param game the game containing the code under test
     * @param mutants a map containing mutants to be added to the prompt. For each mutant the map contains a flag which
     *                is {@code true} if the code of the mutant should be included in the prompt and it is {@code false}
     *                if the name of the method modified by the mutant should be included instead
     * @param tests a list containing tests whose code should be added to the prompt
     * @return the newly built enriched prompt
     */
    public String buildPromptText(AssistantPromptEntity prompt, AbstractGame game, Map<Mutant, Boolean> mutants, List<Test> tests) {
        return buildPromptTextWithQuestion(prompt, "", game, mutants, tests);
    }

    /**
     * Gets the latest prompt saved.
     * @return the latest prompt saved
     * @throws IllegalStateException if no prompt is found
     */
    public AssistantPromptEntity getLastPrompt() {
        Optional<AssistantPromptEntity> promptEntity = assistantPromptRepository.getLastPrompt();
        if(promptEntity.isPresent()) {
            return promptEntity.get();
        } else {
            logger.error("There is no prompt stored in the database");
            throw new IllegalStateException("There is no prompt stored in the database");
        }
    }

    /**
     * Gets all the saved prompts ordered by timestamp, starting with the most recent one.
     * @return all the saved prompts ordered by timestamp, starting with the most recent one
     */
    public List<AssistantPromptEntity> getAllPrompts() {
        return assistantPromptRepository.getAllPrompts();
    }

    /**
     * Saves a new prompt.
     * @param prompt the text of the prompt to be saved
     * @param defaultFlag weather the new prompt should be saved as default or not. Default prompts can be restored
     *                    using the {@link #restoreDefaultPrompt} method
     */
    public void storeNewPrompt(String prompt, boolean defaultFlag) {
        AssistantPromptEntity promptEntity;
        if(prompt.contains("<user_question>")) {
            promptEntity = new AssistantPromptEntity(prompt, false, defaultFlag);
        } else {
            promptEntity = new AssistantPromptEntity(prompt, true, defaultFlag);
        }
        assistantPromptRepository.storePrompt(promptEntity);
    }

    /**
     * Retrieves the latest saved default prompt and saves it again with a new timestamp. The restored prompt will be
     * the one returned by {@link #getLastPrompt()} until a new prompt is saved.
     * @throws IllegalStateException if no default prompt is found
     */
    @Transactional
    public void restoreDefaultPrompt() {
        Optional<AssistantPromptEntity> promptEntity = assistantPromptRepository.getLastDefaultPrompt();
        if(promptEntity.isPresent()) {
            assistantPromptRepository.storePrompt(promptEntity.get());
        } else {
            logger.error("There is no default prompt stored in the database");
            throw new IllegalStateException("There is no default prompt stored in the database");
        }
    }

}
