package org.codedefenders.smartassistant;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.smartassistant.exceptions.ChatGPTException;
import org.codedefenders.smartassistant.response.objects.ChatGPTCompletion;
import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ChatGPTRequestDispatcher {
    private final String openaiAPIKey;
    private final String model;
    private static final Logger logger = LoggerFactory.getLogger(ChatGPTRequestDispatcher.class);

    @Inject
    public ChatGPTRequestDispatcher(Configuration config) {
        openaiAPIKey = config.getOpenaiApiKey();
        model = config.getOpenaiChatgptModel();
    }

    public ChatGPTCompletion sendChatCompletionRequest(ChatGPTMessage message) throws ChatGPTException {
        if(openaiAPIKey == null || openaiAPIKey.equals("")) {
            logger.warn("OpenAI API key is missing in configuration file");
            throw new ChatGPTException();
        }
        if(model == null || model.equals("")) {
            logger.warn("ChatGPT model is missing in configuration file");
            throw new ChatGPTException();
        }
        Gson gson = new Gson();
        HttpClient client = HttpClient.newBuilder().build();
        ChatGPTCompletion completion;
        try {
            URI uri = new URI("https://api.openai.com/v1/chat/completions");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + openaiAPIKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildJsonBodyString(gson, message)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                logger.warn("ChatGPT replied with an error.\n" +
                        "Response status code: " + response.statusCode() +
                        "Response body:\n" + response.body() + "\n\n" +
                        "Check https://platform.openai.com/docs/guides/error-codes/api-errors for further info");
                throw new ChatGPTException();
            }
            completion = gson.fromJson(response.body(), ChatGPTCompletion.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.warn("Sending of request to ChatGPT failed");
            throw new ChatGPTException();
        } catch (JsonSyntaxException e) {
            logger.warn("Gson failed to convert ChatGPT response into a valid Java object");
            throw new ChatGPTException();
        }
        return completion;
    }

    private String buildJsonBodyString(Gson gson, ChatGPTMessage message) {
        return "{\"model\":\"" + model + "\",\"messages\":[" + gson.toJson(message) + "]}";
    }

}
