package org.codedefenders.smartassistant;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.smartassistant.GPTObjects.GPTCompletion;
import org.codedefenders.smartassistant.GPTObjects.GPTMessage;
import org.codedefenders.smartassistant.GPTObjects.GPTRequest;
import org.codedefenders.smartassistant.exceptions.GPTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GPTRequestDispatcher {
    private final String openaiAPIKey;
    private final String model;
    private final Double temperature;
    private static final Logger logger = LoggerFactory.getLogger(GPTRequestDispatcher.class);

    @Inject
    public GPTRequestDispatcher(Configuration config) {
        openaiAPIKey = config.getOpenaiApiKey();
        model = config.getOpenaiChatgptModel();
        temperature = 0.0;
    }

    public GPTCompletion sendChatCompletionRequestWithContext(List<GPTMessage> messages) throws GPTException {
        if(openaiAPIKey == null || openaiAPIKey.equals("")) {
            logger.warn("OpenAI API key is missing in configuration file");
            throw new GPTException();
        }
        if(model == null || model.equals("")) {
            logger.warn("ChatGPT model is missing in configuration file");
            throw new GPTException();
        }
        Gson gson = new Gson();
        HttpClient client = HttpClient.newBuilder().build();
        GPTCompletion completion;
        try {
            GPTRequest body = new GPTRequest(model, messages, temperature);
            URI uri = new URI("https://api.openai.com/v1/chat/completions");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + openaiAPIKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() != 200) {
                logger.warn("ChatGPT replied with an error.\n" +
                        "Response status code: " + response.statusCode() +
                        "Response body:\n" + response.body() + "\n\n" +
                        "Check https://platform.openai.com/docs/guides/error-codes/api-errors for further info");
                throw new GPTException();
            }
            completion = gson.fromJson(response.body(), GPTCompletion.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            logger.warn("Sending of request to ChatGPT failed");
            throw new GPTException();
        } catch (JsonSyntaxException e) {
            logger.warn("Gson failed to convert ChatGPT response into a valid Java object");
            throw new GPTException();
        }
        return completion;
    }

    public GPTCompletion sendChatCompletionRequest(GPTMessage message) throws GPTException {
        List<GPTMessage> messages = new ArrayList<>();
        messages.add(message);
        return sendChatCompletionRequestWithContext(messages);
    }

}
