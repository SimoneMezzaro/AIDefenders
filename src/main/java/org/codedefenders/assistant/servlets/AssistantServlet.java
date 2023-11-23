package org.codedefenders.assistant.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codedefenders.assistant.entities.AssistantQuestionEntity;
import org.codedefenders.assistant.exceptions.GPTException;
import org.codedefenders.assistant.services.AssistantService;
import org.codedefenders.auth.CodeDefendersAuth;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.database.GameDAO;
import org.codedefenders.database.PlayerDAO;
import org.codedefenders.game.*;
import org.codedefenders.game.multiplayer.MeleeGame;
import org.codedefenders.service.game.AbstractGameService;
import org.codedefenders.service.game.MeleeGameService;
import org.codedefenders.service.game.MultiplayerGameService;
import org.codedefenders.servlets.games.GameProducer;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * This {@link HttpServlet} handles all user requests related to the GPT smart assistant.
 * <p>
 * Multiple types of {@code GET} requests are handled by {@link AssistantServlet}. The {@code GET} request type is
 * specified in the URL parameter {@code action}. Available types are:
 * <p>- {@code previousQuestions}: retrieves all the questions asked by the user in the current game
 * <p>- {@code remainingQuestions}: retrieves the number of questions that the user is still able to ask
 * <p>
 * Multiple types of {@code POST} requests are handled by {@link AssistantServlet}. These {@code POST} requests expect
 * the body {@code Content-type} to be {@code application/x-www-form-urlencoded}. The {@code POST} request type is
 * specified in the body parameter {@code action}. Available types are:
 * <p>- {@code question}: submits a new question for the assistant
 * <p>- {@code feedback}: submits a feedback for the last submitted question
 * <p>
 * All the responses sent by {@link AssistantServlet} are in JSON format, including most of the redirects.
 */
@WebServlet(Paths.SMART_ASSISTANT)
public class AssistantServlet extends HttpServlet {
    @Inject
    private AssistantService assistantService;
    @Inject
    private MultiplayerGameService multiplayerGameService;
    @Inject
    private MeleeGameService meleeGameService;
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
        Map<String, String> responseBody = new HashMap<>();
        AbstractGame game = gameProducer.getGame();
        String redirectUrl = computeRedirectUrlIfGameIsValid(request, response, game);
        if(redirectUrl == null){
            return;
        }
        int userId = login.getUserId();
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(userId, game.getId());
        if(!assistantService.isAssistantEnabledForUser(userId)) {
            sendRedirectWithMessage(response, "Your smart assistant is currently disabled", redirectUrl);
            return;
        }
        String action = request.getParameter("action");
        if(action == null) {
            logger.info("Failed to process get request because the request action is missing");
            sendRedirectWithMessage(response, "Unable to get previous questions", redirectUrl);
            return;
        }
        switch(action) {
            case "previousQuestions" -> {
                List<AssistantQuestionEntity> questionsList = assistantService.getQuestionsByPlayer(playerId);
                sendJson(response, questionsList);
            }
            case "remainingQuestions" -> {
                int remainingQuestions = assistantService.getRemainingQuestionsForUser(userId);
                responseBody.put("remainingQuestions", Integer.toString(remainingQuestions));
                sendJson(response, responseBody);
            }
            default -> {
                logger.info("Failed to process get request because the request action is malformed");
                sendRedirectWithMessage(response, "Unable to get previous questions", redirectUrl);
            }
        }
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
        switch(action) {
            case "question" -> postQuestion(request, response, redirectUrl, game);
            case "feedback" -> postFeedback(request, response, redirectUrl, game);
            default -> {
                logger.info("Failed to process post request because the request was malformed");
                sendRedirectWithMessage(response, "Operation failed due to malformed request", redirectUrl);
            }
        }
    }

    /**
     * Submits a new question to GPT and writes the answer in the response body. This method also checks that the
     * question has the correct length and does not contain invalid mutants or tests tags.
     * @param request the {@link HttpServletRequest} containing the new question
     * @param response the {@link HttpServletResponse} which will contain the answer
     * @param redirectUrl the URL of the game page where the user is redirected if the question is not valid
     * @param game the game where the question was sent
     * @throws IOException when the writing of the body fails
     */
    private void postQuestion(HttpServletRequest request, HttpServletResponse response, String redirectUrl, AbstractGame game)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        String questionText = request.getParameter("question");
        Boolean answerCode = Boolean.valueOf(request.getParameter("answerCode"));
        if(questionText == null || questionText.isEmpty()) {
            sendRedirectWithMessage(response, "You can't submit empty questions to the smart assistant", redirectUrl);
            return;
        }
        questionText = questionText.trim();
        if(questionText.split("\\s+").length > 1500) {
            sendRedirectWithMessage(response, "Your question is too long. Use at most 1500 words", redirectUrl);
            return;
        }
        int userId = login.getUserId();
        AbstractGameService abstractGameService;
        if(game.getMode() == GameMode.MELEE) {
            abstractGameService = meleeGameService;
        } else {
            abstractGameService = multiplayerGameService;
        }

        // retrieves all the mutants tagged in the question
        Map<Mutant, Boolean> mutantsMap = new HashMap<>();
        Pattern pattern = Pattern.compile("@mutant[0-9]*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(questionText);
        while(matcher.find()) {
            String mutantString = matcher.group();
            try {
                int mutantId = Integer.parseInt(mutantString.substring("@mutant".length()));
                Mutant mutant = game.getMutantByID(mutantId);
                if(mutant != null) {
                    mutantsMap.put(mutant, abstractGameService.getMutant(userId, mutant).isCanView());
                } else {
                    sendRedirectWithMessage(response, mutantString + " does not exist in the current game", redirectUrl);
                    return;
                }
            } catch(NumberFormatException e) {
                sendRedirectWithMessage(response, mutantString + " does not exist in the current game", redirectUrl);
                return;
            }
        }

        // retrieves all the tests tagged in the question
        List<Test> allTests = game.getTests();
        List<Test> testsList = new ArrayList<>();
        pattern = Pattern.compile("@test[0-9]*", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(questionText);
        while(matcher.find()) {
            String testString = matcher.group();
            try {
                int testId = Integer.parseInt(testString.substring("@test".length()));
                Test test = null;
                for(Test t : allTests) {
                    if(t.getId() == testId) {
                        test = t;
                    }
                }
                if(test != null && abstractGameService.getTest(userId, test).isCanView()) {
                    testsList.add(test);
                } else {
                    sendRedirectWithMessage(response, testString + " does not exist in the current game or is not visible",
                            redirectUrl);
                    return;
                }
            } catch(NumberFormatException e) {
                sendRedirectWithMessage(response, testString + " does not exist in the current game or is not visible",
                        redirectUrl);
                return;
            }
        }

        if(!assistantService.checkAndDecrementRemainingQuestions(userId)) {
            sendRedirectWithMessage(response, "Question refused. You have reached your questions quota!", redirectUrl);
            return;
        }
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(userId, game.getId());
        AssistantQuestionEntity question = new AssistantQuestionEntity(questionText, playerId, answerCode);
        try {
            question = assistantService.sendQuestion(question, game, mutantsMap, testsList);
        } catch(GPTException e) {
            assistantService.incrementRemainingQuestions(userId);
            sendRedirectWithMessage(response, "The smart assistant encountered an error!\n" +
                    "Please try again and contact your administrator if this keeps happening", redirectUrl);
            return;
        }
        responseBody.put("question", question.getQuestion());
        responseBody.put("answer", question.getAnswer());
        sendJson(response, responseBody);
    }

    /**
     * Updates the feedback of the last question sent by the user in the given game
     * @param request the {@link HttpServletRequest} containing the feedback attribute
     * @param response the {@link HttpServletResponse}
     * @param redirectUrl the URL of the game page where the user is redirected if the post request is not valid
     * @param game the game where the question was sent
     * @throws IOException when the writing of the body fails
     */
    private void postFeedback(HttpServletRequest request, HttpServletResponse response, String redirectUrl, AbstractGame game)
            throws IOException {
        boolean feedback = Boolean.parseBoolean(request.getParameter("feedback"));
        int playerId = PlayerDAO.getPlayerIdForUserAndGame(login.getUserId(), game.getId());
        if(!assistantService.updateQuestionFeedback(playerId, feedback)) {
            logger.info("Invalid feedback update by player " + playerId + ". No question was found for this player.");
            sendRedirectWithMessage(response, "You can't submit a question feedback because you have not sent any " +
                    "question yet", redirectUrl);
        }
    }

    /**
     * Performs several checks related to the game and returns the URL of the game page if all the checks are successful.
     * In particular this method checks that the game exists, that it is neither finished nor expired and that the user
     * is part of the game. If one of the checks fails, then an appropriate redirect is written in the response body.
     * @param request the {@link HttpServletRequest}
     * @param response the {@link HttpServletResponse}
     * @param game the game on which the checks must be performed
     * @return the game page URL of the given game if all the checks are successful; {@code null} otherwise
     * @throws IOException when the writing of the body fails
     */
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

    /**
     * Writes a response containing the given redirect URL in its JSON body and adds a given message to the
     * {@link MessagesBean}.
     * @param response the {@link HttpServletResponse}
     * @param message the message to be added to the {@link MessagesBean}
     * @param redirectUrl the URL where the client should be redirected
     * @throws IOException when the writing of the body fails
     */
    private void sendRedirectWithMessage(HttpServletResponse response, String message, String redirectUrl)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        messages.add(message);
        responseBody.put("redirect", redirectUrl);
        sendJson(response, responseBody);
    }

    /**
     * Writes a given object in the response body in JSON format.
     * @param response the {@link HttpServletResponse}
     * @param responseBody the object to be written in the response body
     * @throws IOException when the writing of the body fails
     */
    private void sendJson(HttpServletResponse response, Object responseBody) throws IOException {
        String json = new Gson().toJson(responseBody);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

}
