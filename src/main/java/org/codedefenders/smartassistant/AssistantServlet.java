package org.codedefenders.smartassistant;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.GameMode;
import org.codedefenders.game.GameState;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.model.AssistantQuestionEntity;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.smartassistant.exceptions.GPTException;
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
    private static final Logger logger = LoggerFactory.getLogger(AssistantServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AbstractGame game = gameProducer.getGame();
        String redirectUrl = computeRedirectUrlIfGameIsValid(request, response, game);
        if(redirectUrl == null){
            return;
        }
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), game.getId());
        if(!assistantService.isAssistantEnabledForUser(login.getUserId())) {
            sendRedirectWithMessage(response, "Your smart assistant is currently disabled", redirectUrl);
            return;
        }
        List<AssistantQuestionEntity> questionsList = assistantService.getQuestionsByPlayer(playerId);
        sendJson(response, questionsList);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AbstractGame game = gameProducer.getGame();
        String redirectUrl = computeRedirectUrlIfGameIsValid(request, response, game);
        if(redirectUrl == null){
            return;
        }
        if(!assistantService.isAssistantEnabledForUser(login.getUserId())) {
            sendRedirectWithMessage(response, "Your smart assistant is currently disabled", redirectUrl);
            return;
        }
        String action = request.getParameter("action");
        if(action == null) {
            logger.info("Failed to process post request because the request action is missing");
            sendRedirectWithMessage(response, "Operation failed due to malformed request", redirectUrl);
            return;
        }
        switch (action) {
            case "question" -> postQuestion(request, response, redirectUrl, game);
            case "feedback" -> postFeedback(request, game);
            default -> {
                logger.info("Failed to process post request because the request was malformed");
                sendRedirectWithMessage(response, "Operation failed due to malformed request", redirectUrl);
            }
        }
    }

    private void postQuestion(HttpServletRequest request, HttpServletResponse response, String redirectUrl, AbstractGame game)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        String questionText = request.getParameter("question");
        if(questionText == null || questionText.isEmpty()) {
            sendRedirectWithMessage(response, "You can't submit empty questions to the smart assistant", redirectUrl);
            return;
        }
        questionText = questionText.trim();
        if(questionText.split("\\s+").length > 1500) {
            sendRedirectWithMessage(response, "Your question is too long. Use at most 1500 words", redirectUrl);
            return;
        }
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), game.getId());
        AssistantQuestionEntity question = new AssistantQuestionEntity(questionText, playerId);
        try {
            question = assistantService.sendQuestion(question, game);
        } catch (GPTException e) {
            sendRedirectWithMessage(response, "The smart assistant encountered an error!\n" +
                    "Please try again and contact your administrator if this keeps happening", redirectUrl);
            return;
        }
        responseBody.put("question", question.getQuestion());
        responseBody.put("answer", question.getAnswer());
        sendJson(response, responseBody);
    }

    private void postFeedback(HttpServletRequest request, AbstractGame game) {
        boolean feedback = Boolean.parseBoolean(request.getParameter("feedback"));
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), game.getId());
        assistantService.updateQuestionFeedback(playerId, feedback);
    }

    private String computeRedirectUrlIfGameIsValid(HttpServletRequest request, HttpServletResponse response, AbstractGame game)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if (game == null) {
            logger.error("No game found. Aborting request.");
            String referer = request.getHeader("referer");
            responseBody.put("redirect", referer != null ? referer : "/");
            sendJson(response, responseBody);
            return null;
        }
        GameMode mode = game.getMode();
        if(mode == null) {
            logger.error("Game mode not set. Aborting request.");
            String referer = request.getHeader("referer");
            responseBody.put("redirect", referer != null ? referer : "/");
            sendJson(response, responseBody);
            return null;
        }
        int gameId = game.getId();
        int userId = login.getUserId();
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), gameId);
        String redirectUrl;
        if(mode == GameMode.MELEE) {
            if(!((MeleeGame) game).hasUserJoined(userId) && game.getCreatorId() != userId) {
                logger.info("User {} not part of game {}. Aborting request.", userId, gameId);
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return null;
            }
            if (game.getCreatorId() != userId && playerId == -1) {
                logger.warn("Wrong registration with the User {} in Melee Game {}", userId, gameId);
                response.sendRedirect(url.forPath(Paths.GAMES_OVERVIEW));
                return null;
            }
            redirectUrl = url.forPath(Paths.MELEE_GAME) + "?gameId=" + game.getId();
        } else if(mode == GameMode.PARTY) {
            if (playerId == -1 && game.getCreatorId() != login.getUserId()) {
                logger.info("User {} not part of game {}. Aborting request.", login.getUserId(), gameId);
                responseBody.put("redirect", url.forPath(Paths.GAMES_OVERVIEW));
                sendJson(response, responseBody);
                return null;
            }
            redirectUrl = url.forPath(Paths.BATTLEGROUND_GAME) + "?gameId=" + game.getId();
        } else {
            logger.error("Invalid game mode. Aborting request.");
            String referer = request.getHeader("referer");
            responseBody.put("redirect", referer != null ? referer : "/");
            sendJson(response, responseBody);
            return null;
        }
        if (game.getState() == GameState.FINISHED || GameDAO.isGameExpired(gameId)) {
            logger.info("Game {} is finished or expired. Aborting request.", gameId);
            responseBody.put("redirect", redirectUrl);
            sendJson(response, responseBody);
            return null;
        }
        return redirectUrl;
    }

    private void sendRedirectWithMessage(HttpServletResponse response, String message, String redirectUrl)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        messages.add(message);
        responseBody.put("redirect", redirectUrl);
        sendJson(response, responseBody);
    }

    private void sendJson(HttpServletResponse response, Object responseBody) throws IOException {
        String json = new Gson().toJson(responseBody);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

}
