package org.codedefenders.smartassistant;

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
import org.codedefenders.beans.message.MessagesBean;
import org.codedefenders.model.AssistantPromptEntity;
import org.codedefenders.model.SmartAssistantType;
import org.codedefenders.service.UserService;
import org.codedefenders.util.Constants;
import org.codedefenders.util.Paths;
import org.codedefenders.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

@WebServlet(Paths.ADMIN_ASSISTANT)
public class AdminAssistantSettings extends HttpServlet {

    @Inject
    private AssistantService assistantService;
    @Inject AssistantPromptService assistantPromptService;
    @Inject
    private UserService userService;
    @Inject
    private MessagesBean messages;
    @Inject
    private URLUtils url;
    private static final Logger logger = LoggerFactory.getLogger(ChatGPTRequestDispatcher.class);

    private static class PostBody {
        private String action;
        private Map<Integer, SmartAssistantType> usersSettings;
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
        switch (action) {
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
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        Gson gson = new Gson();
        PostBody body;
        try {
            body = gson.fromJson(sb.toString(), PostBody.class);
        } catch (JsonSyntaxException e) {
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
        switch (action) {
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

    private void getAllSettings(HttpServletResponse response) throws IOException {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("usersSettings", assistantService.getAllAssistantUserSettings());
        //TODO: let the user select the amount of days
        responseBody.put("questionsPerDay", assistantService.getAmountOfQuestionsInTheLastDays(7));
        responseBody.put("totalQuestions", assistantService.getTotalQuestionsAmount());
        responseBody.put("prompt", assistantPromptService.getLastPrompt());
        responseBody.put("assistantEnabled", assistantService.getAssistantEnabled());
        sendJson(response, responseBody);
    }

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
        switch (type) {
            case "csv" -> {
                prompts = assistantPromptService.getAllPrompts();
                String[] columns = new String[]{
                        "timestamp",
                        "prompt",
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

    private void postUsersSettingsUpdate(HttpServletResponse response, Map<Integer, SmartAssistantType> usersSettings)
            throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        boolean isUpdateValid = true;
        for(Integer userId : usersSettings.keySet()) {
            if(userService.getSimpleUserById(userId).isEmpty()) {
                messages.add("Operation failed because user " + userId + " does not exist");
                isUpdateValid = false;
            }
        }
        if(isUpdateValid) {
            assistantService.updateAssistantUserSettings(usersSettings);
            messages.add("All settings of selected users have been updated");
            logger.info("Users settings for smart assistant updated");
        }
        else {
            logger.info("Failed to update users settings for smart assistant because some of the selected users " +
                    "do not exist");
        }
        responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
        sendJson(response, responseBody);
    }

    private void postNewPrompt(HttpServletResponse response, String prompt, Boolean defaultFlag) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if(prompt == null || defaultFlag == null) {
            messages.add("Unable to update the prompt because the request was malformed");
            logger.info("Prompt update failed because the prompt or the default flag were missing");
        }
        else {
            assistantPromptService.storeNewPrompt(prompt, defaultFlag);
            logger.info("Prompt updated");
            messages.add("Prompt updated");
        }
        responseBody.put("redirect", url.forPath("/admin/assistant"));
        sendJson(response, responseBody);
    }

    private void postRestorePrompt(HttpServletResponse response) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        assistantPromptService.restoreDefaultPrompt();
        messages.add("The most recent default prompt has been restored");
        logger.info("Default prompt restored");
        responseBody.put("redirect", url.forPath(Paths.ADMIN_ASSISTANT));
        sendJson(response, responseBody);
    }

    private void postAssistantEnabledUpdate(HttpServletResponse response, Boolean enabled) throws IOException {
        Map<String, String> responseBody = new HashMap<>();
        if(enabled == null) {
            messages.add("Unable to update assistant settings because the request was malformed");
            logger.info("Assistant enabled setting update failed because the enabled flag was missing");
        }
        else {
            assistantService.updateAssistantEnabled(enabled);
            messages.add("Assistant " + (enabled ? "enabled" : "disabled"));
            logger.info("Assistant " + (enabled ? "enabled" : "disabled"));
        }
        responseBody.put("redirect", url.forPath("/admin/assistant"));
        sendJson(response, responseBody);
    }

    private void sendJson(HttpServletResponse response, Object responseBody) throws IOException {
        String json = new Gson().toJson(responseBody);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

}
