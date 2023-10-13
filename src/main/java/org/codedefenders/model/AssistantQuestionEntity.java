package org.codedefenders.model;

public class AssistantQuestionEntity {

    private transient Integer id;
    private String question;
    private String answer;
    private transient Integer playerId;
    private transient Boolean useful;

    public AssistantQuestionEntity(String question, Integer playerId) {
        this.question = question;
        this.playerId = playerId;
    }

    public AssistantQuestionEntity(Integer id, String question, String answer, Integer playerId, Boolean useful) {
        this.id = id;
        this.question = question;
        this.answer = answer;
        this.playerId = playerId;
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

    public Boolean getUseful() {
        return useful;
    }

    public void setUseful(Boolean useful) {
        this.useful = useful;
    }

}
