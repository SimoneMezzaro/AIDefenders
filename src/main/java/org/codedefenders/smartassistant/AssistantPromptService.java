package org.codedefenders.smartassistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.AssistantPromptEntity;
import org.codedefenders.persistence.database.AssistantPromptRepository;

public class AssistantPromptService {

    @Inject
    private AssistantPromptRepository assistantPromptRepository;

    public String buildPromptTextWithQuestion(AssistantPromptEntity prompt, String question, AbstractGame game,
                                              Map<Mutant, Boolean> mutants, List<Test> tests) {
        String regex = "<mutants\\s*\\(.*\\)>|<tests\\s*\\(.*\\)>|<class_under_test>|<user_question>";
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(prompt.getPrompt());
        List<String> placeholders = new ArrayList<>();
        String extendedPrompt = "_" + prompt.getPrompt() + "_";
        String[] promptBlocks = extendedPrompt.split(regex);
        while(matcher.find()) {
            placeholders.add(matcher.group());
        }
        StringBuilder mutantsString = new StringBuilder();
        for(Mutant m : mutants.keySet()) {
            mutantsString.append("@mutant").append(m.getId()).append(" ");
            if(mutants.get(m)) {
                mutantsString.append("has code:\n").append(m.getAsString());
            } else {
                mutantsString.append(String.join(", ", m.getHTMLReadout()).toLowerCase()).append(".");
            }
            mutantsString.append("\n\n");
        }
        StringBuilder testsString = new StringBuilder();
        for(Test t : tests) {
            testsString.append("@test").append(t.getId()).append(" has code:\n").append(t.getAsString()).append("\n\n");
        }
        StringBuilder promptBuilder = new StringBuilder(promptBlocks[0].substring(1));
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
        promptBuilder.deleteCharAt(promptBuilder.length() - 1);
        return promptBuilder.toString();
    }

    public String buildPromptText(AssistantPromptEntity prompt, AbstractGame game, Map<Mutant, Boolean> mutants, List<Test> tests) {
        return buildPromptTextWithQuestion(prompt, "", game, mutants, tests);
    }

    public AssistantPromptEntity getLastPrompt() {
        Optional<AssistantPromptEntity> promptEntity = assistantPromptRepository.getLastPrompt();
        if(promptEntity.isPresent()) {
            return promptEntity.get();
        }
        else {
            //TODO: handle error
            return null;
        }
    }

    public List<AssistantPromptEntity> getAllPrompts() {
        return assistantPromptRepository.getAllPrompts();
    }

    public void storeNewPrompt(String prompt, boolean defaultFlag) {
        AssistantPromptEntity promptEntity;
        if(prompt.contains("<user_question>")) {
            promptEntity = new AssistantPromptEntity(prompt, false, defaultFlag);
        } else {
            promptEntity = new AssistantPromptEntity(prompt, true, defaultFlag);
        }
        assistantPromptRepository.storePrompt(promptEntity);
    }

    public void storeNewPrompt(String prompt) {
        storeNewPrompt(prompt, false);
    }

    public void restoreDefaultPrompt() {
        Optional<AssistantPromptEntity> promptEntity = assistantPromptRepository.getLastDefaultPrompt();
        if(promptEntity.isPresent()) {
            assistantPromptRepository.storePrompt(promptEntity.get());
        }
        else {
            //TODO: handle error
        }
    }

}
