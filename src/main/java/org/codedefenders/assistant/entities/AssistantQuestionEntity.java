package org.codedefenders.assistant.entities;

/**
 * This class represents a question entity in the database. A question entity stores a question sent by a player, its
 * answer, the id of the {@link AssistantPromptEntity} used with the question, the id of the player asking and whether
 * the player found the answer useful.
 */
public class AssistantQuestionEntity {

    private transient Integer id;
    private String question;
    private String answer;
    private transient Integer playerId;
    private transient Integer promptId;
    private transient Boolean useful;

    public AssistantQuestionEntity(String question, Integer playerId) {
        this.question = question;
        this.playerId = playerId;
    }

    public AssistantQuestionEntity(Integer id, String question, String answer, Integer playerId, Integer promptId, Boolean useful) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.playerId = playerId;
        this.promptId = promptId;
        this.useful = useful;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Integer getPromptId() {
        return promptId;
    }

    public void setPromptId(Integer promptId) {
        this.promptId = promptId;
    }

    public Boolean getUseful() {
        return useful;
    }

    public void setUseful(Boolean useful) {
        this.useful = useful;
    }

}
