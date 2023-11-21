package org.codedefenders.assistant.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.codedefenders.assistant.services.AssistantPromptService;
import org.codedefenders.assistant.services.AssistantService;
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.assistant.entities.AssistantPromptEntity;
import org.codedefenders.assistant.entities.AssistantUserSettingsEntity;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This {@link HttpServlet} handles all admin requests related to the GPT smart assistant.
 * <p>
 * The default {@code GET} request returns the admin page related to the smart assistant. Multiple types of other
 * {@code GET} requests are handled by {@link AdminAssistantSettings}. The {@code GET} request type is
 * specified in the URL parameter {@code action}. Available types are:
 * <p>- {@code allSettings}: retrieves all the current settings related to the assistant
 * <p>- {@code downloadPrompts}: retrieves all the prompts from the database
 * <p>
 * Multiple types of {@code POST} requests are handled by {@link AdminAssistantSettings}. These {@code POST} requests
 * expect the body to be in JSON format. The {@code POST} request type is specified in the body parameter {@code action}.
 * Available types are:
 * <p>- {@code usersSettingsUpdate}: updates the assistant settings of all the given users
 * <p>- {@code newPrompt}: stores a new prompt
 * <p>- {@code restorePrompt}: restores the last default prompt saved in the database
 * <p>- {@code assistantEnabledUpdate}: enables or disables the assistant
 * <p>
 * All the responses sent by {@link AdminAssistantSettings} are in JSON format, including most of the redirects.
 */
@WebServlet(Paths.ADMIN_ASSISTANT)
public class AdminAssistantSettings extends HttpServlet {

    @Inject
    private AssistantService assistantService;
    @Inject
    AssistantPromptService assistantPromptService;
    @Inject
    private UserService userService;
    @Inject
    private MessagesBean messages;
    @Inject
    private URLUtils url;
    private static final Logger logger = LoggerFactory.getLogger(AdminAssistantSettings.class);

    /**
     * This class represents the body of the {@code POST} requests handled by {@link AdminAssistantSettings}. It is used
     * to convert the body content from JSON to Java objects.
     */
    private static class PostBody {
        private String action;
        private List<AssistantUserSettingsEntity> usersSettings;
        private String prompt;
        private Boolean defaultFlag;
        private Boolean assistantEnabled;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");
        if(action == null) {
            request.getRequestDispatcher(Constants.ADMIN_ASSISTANT_JSP).forward(request, response);
            return;
        }
        switch(action) {
            case "allSettings" -> getAllSettings(response);
            case "downloadPrompts" -> getDownloadPrompts(request, response);
            default -> request.getRequestDispatcher(Constants.ADMIN_ASSISTANT_JSP).forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, String> responseBody = new HashMap<>();
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        Gson gson = new Gson();
        PostBody body;
        try {
            body = gson.fromJson(sb.toString(), PostBody.class);
        } catch(JsonSyntaxException e) {
            messages.add("Operation failed due to malformed request");
            logger.info("Failed to process post request because the request was malformed");
            responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
            sendJson(response, responseBody);
            return;
        }
        String action = body.action;
        if(action == null) {
            messages.add("Operation failed due to malformed request");
            logger.info("Failed to process post request because the request action is missing");
            responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
            sendJson(response, responseBody);
            return;
        }
        switch(action) {
            case "usersSettingsUpdate" -> postUsersSettingsUpdate(response, body.usersSettings);
            case "newPrompt" -> postNewPrompt(response, body.prompt, body.defaultFlag);
            case "restorePrompt" -> postRestorePrompt(response);
            case "assistantEnabledUpdate" -> postAssistantEnabledUpdate(response, body.assistantEnabled);
            default -> {
                messages.add("Operation failed due to malformed request");
                logger.info("Failed to process post request because the request was malformed");
                responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
                sendJson(response, responseBody);
            }
        }
    }

    /**
     * Writes in the response all the assistant settings. The settings include the settings of each user, the number of
     * questions sent in the last seven days, the total number of questions sent, the current prompt and the status of
     * the assistant (enabled or disabled).
     * @param response the {@link HttpServletResponse}
     * @throws IOException when the writing of the body fails
     */
    private void getAllSettings(HttpServletResponse response) throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("usersSettings", assistantService.getAllAssistantUserSettings());
        responseBody.put("questionsPerDay", assistantService.getAmountOfQuestionsInTheLastDays(7));
        responseBody.put("totalQuestions", assistantService.getTotalQuestionsAmount());
        responseBody.put("prompt", assistantPromptService.getLastPrompt().getPrompt());
        responseBody.put("assistantEnabled", assistantService.getAssistantEnabled());
        sendJson(response, responseBody);
    }

    /**
     * Writes in the response a csv or JSON file containing all the prompts stored in the database.
     * @param request the {@link HttpServletRequest} containing the requested format of the file
     * @param response the {@link HttpServletResponse} which will contain the file
     * @throws IOException when the writing of the body fails
     */
    private void getDownloadPrompts(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String type = request.getParameter("fileType");
        if(type == null) {
            Map<String, Object> responseBody = new HashMap<>();
            logger.info("Unable to download prompts because the request was malformed");
            responseBody.put("error", "Download failed due to malformed request");
            sendJson(response, responseBody);
            return;
        }
        List<AssistantPromptEntity> prompts;
        switch(type) {
            case "csv" -> {
                prompts = assistantPromptService.getAllPrompts();
                String[] columns = new String[]{
                        "timestamp",
                        "prompt",
                        "asSeparateContext",
                        "defaultFlag"
                };
                PrintWriter out = response.getWriter();
                CSVPrinter csvPrinter = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(columns).build());
                for(AssistantPromptEntity prompt : prompts) {
                    csvPrinter.print(prompt);
                    csvPrinter.println();
                }
                csvPrinter.flush();
            }
            case "json" -> {
                prompts = assistantPromptService.getAllPrompts();
                String json = new Gson().toJson(prompts);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().print(json);
            }
            default -> {
                Map<String, Object> responseBody = new HashMap<>();
                logger.info("Unable to download prompts because the request was malformed");
                responseBody.put("error", "Download failed due to malformed request");
                sendJson(response, responseBody);
            }
        }
    }

    /**
     * Updates the settings of each given user. The settings of each user include both the type of their assistant and
     * their amount of remaining questions. The update is performed only if all users and settings are valid. A redirect
     * to the game page containing an error message is sent otherwise.
     * @param response the {@link HttpServletResponse}
     * @param usersSettings the list of users settings to be updated
     * @throws IOException when the writing of the body fails
     */
    private void postUsersSettingsUpdate(HttpServletResponse response, List<AssistantUserSettingsEntity> usersSettings)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        boolean isUpdateValid = true;
        for(AssistantUserSettingsEntity userSettings : usersSettings) {
            Integer userId = userSettings.getUserId();
            if(userId == null || userService.getSimpleUserById(userId).isEmpty()) {
                messages.add("Operation failed because user " + userId + " does not exist");
                isUpdateValid = false;
            } else if(userSettings.getRemainingQuestionsDelta() == null) {
                messages.add("Operation failed because amount of remaining questions is missing for user " + userId);
                isUpdateValid = false;
            } else if(userSettings.getAssistantType() == null) {
                messages.add("Operation failed because assistant type is missing for user " + userId);
                isUpdateValid = false;
            }
        }
        if(isUpdateValid) {
            assistantService.updateAssistantUserSettings(usersSettings);
            messages.add("All settings of selected users have been updated");
            logger.info("Users settings for smart assistant updated");
        } else {
            logger.info("Failed to update users settings for smart assistant because some of the selected users " +
                    "do not exist");
        }
        responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
        sendJson(response, responseBody);
    }

    /**
     * Stores a new prompt. This prompt becomes the prompt used for future questions.
     * @param response the {@link HttpServletResponse}
     * @param prompt the new prompt to be stored
     * @param defaultFlag {@code true} if the prompt should become the new default prompt; {@code false} otherwise
     * @throws IOException when the writing of the body fails
     */
    private void postNewPrompt(HttpServletResponse response, String prompt, Boolean defaultFlag) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if(prompt == null || defaultFlag == null) {
            messages.add("Unable to update the prompt because the request was malformed");
            logger.info("Prompt update failed because the prompt or the default flag were missing");
        } else {
            assistantPromptService.storeNewPrompt(prompt, defaultFlag);
            logger.info("Prompt updated");
            messages.add("Prompt updated");
        }
        responseBody.put("redirect", url.forPath("/admin/assistant"));
        sendJson(response, responseBody);
    }

    /**
     * Restores the last saved default prompt from the database. This prompt becomes the prompt used for future
     * questions.
     * @param response the {@link HttpServletResponse}
     * @throws IOException when the writing of the body fails
     */
    private void postRestorePrompt(HttpServletResponse response) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        assistantPromptService.restoreDefaultPrompt();
        messages.add("The most recent default prompt has been restored");
        logger.info("Default prompt restored");
        responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
        sendJson(response, responseBody);
    }

    /**
     * Updates the status of the assistant (enabled or disabled).
     * @param response the {@link HttpServletResponse}
     * @param enabled {@code true} if the assistant should be enabled; {@code false} if the assistant should be disabled
     * @throws IOException when the writing of the body fails
     */
    private void postAssistantEnabledUpdate(HttpServletResponse response, Boolean enabled) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if(enabled == null) {
            messages.add("Unable to update assistant settings because the request was malformed");
            logger.info("Assistant enabled setting update failed because the enabled flag was missing");
        } else {
            assistantService.updateAssistantEnabled(enabled);
            messages.add("Assistant " + (enabled ? "enabled" : "disabled"));
            logger.info("Assistant " + (enabled ? "enabled" : "disabled"));
        }
        responseBody.put("redirect", url.forPath("/admin/assistant"));
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
