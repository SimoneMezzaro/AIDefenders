package org.codedefenders.smartassistant;

import java.io.IOException;
import java.util.*;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.game.GameState;
import org.codedefenders.game.multiplayer.MultiplayerGame;
import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.smartassistant.exceptions.ChatGPTException;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

@WebServlet(Paths.SMART_ASSISTANT)
public class AssistantServlet extends HttpServlet {
    @Inject
    private AssistantService assistantService;
    @Inject
    private GameProducer gameProducer;
    @Inject
    private CodeDefendersAuth login;
    @Inject
    private MessagesBean messages;
    @Inject
    private URLUtils url;
    private static final Logger logger = LoggerFactory.getLogger(ChatGPTRequestDispatcher.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        MultiplayerGame game = gameProducer.getMultiplayerGame();
        Integer playerId = getPlayerIdIfGameIsValid(request, response, game);
        if(playerId == null){
            return;
        }
        List<AssistantQuestionEntity> questionsList = assistantService.getQuestionsByPlayer(playerId);
        sendJson(response, questionsList);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String> responseBody = new HashMap<>();
        MultiplayerGame game = gameProducer.getMultiplayerGame();
        Integer playerId = getPlayerIdIfGameIsValid(request, response, game);
        if(playerId == null){
            return;
        }
        int gameId = game.getId();
        String questionText = request.getParameter("question");
        if(questionText == null || questionText.isEmpty()) {
            messages.add("You can't submit empty questions to the smart assistant");
            responseBody.put("redirect", url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
            sendJson(response, responseBody);
            return;
        }
        questionText = questionText.trim();
        AssistantQuestionEntity question = new AssistantQuestionEntity(questionText, playerId);
        try {
            question = assistantService.sendQuestionWithNoContext(question);
        } catch (ChatGPTException e) {
            messages.add("The smart assistant encountered an error!\n" +
                    "Please try again and contact your administrator if this keeps happening");
            responseBody.put("redirect", url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
            sendJson(response, responseBody);
            return;
        }
        responseBody.put("question", question.getQuestion());
        responseBody.put("answer", question.getAnswer());
        sendJson(response, responseBody);
    }

    private Integer getPlayerIdIfGameIsValid(HttpServletRequest request, HttpServletResponse response, MultiplayerGame game)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if (game == null) {
            logger.error("No game found. Aborting request.");
            String referer = request.getHeader("referer");
            responseBody.put("redirect", referer != null ? referer : "/");
            sendJson(response, responseBody);
            return null;
        }
        int gameId = game.getId();
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
        if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
            logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), gameId);
            responseBody.put("redirect", url.forPath(Paths.GAMES_OVERVIEW));
            sendJson(response, responseBody);
            return null;
        }
        if (game.getState() == GameState.FINISHED || GameDAO.isGameExpired(gameId)) {
            logger.info("Game {} is finished or expired. Aborting request.", gameId);
            responseBody.put("redirect", url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + gameId);
            sendJson(response, responseBody);
            return null;
        }
        return playerId;
    }

    private void sendJson(HttpServletResponse response, Object responseBody) throws IOException {
        String json = new Gson().toJson(responseBody);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

}
