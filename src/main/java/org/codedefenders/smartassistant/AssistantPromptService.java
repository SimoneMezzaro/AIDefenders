package org.codedefenders.smartassistant;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.AssistantPromptEntity;
import org.codedefenders.persistence.database.AssistantPromptRepository;

public class AssistantPromptService {

    @Inject
    private AssistantPromptRepository assistantPromptRepository;

    public String buildPrompt(String prompt, MultiplayerGame game) {
        String CUTSourceCode= game.getCUT().getSourceCode();
        return prompt.replace("<class_under_test>", CUTSourceCode);
    }

    public String getLastPrompt() {
        Optional<AssistantPromptEntity> promptEntity = assistantPromptRepository.getLastPrompt();
        if(promptEntity.isPresent()) {
            return promptEntity.get().getPrompt();
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
        AssistantPromptEntity promptEntity = new AssistantPromptEntity(prompt, defaultFlag);
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
