package org.codedefenders.assistant.services;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codedefenders.assistant.GPTObjects.GPTCompletion;
import org.codedefenders.assistant.GPTObjects.GPTMessage;
import org.codedefenders.assistant.GPTObjects.GPTRequest;
import org.codedefenders.assistant.exceptions.GPTException;
import org.codedefenders.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * This class provides utility methods to send GPT requests to OpenAI API. In order to be able to send API request the
 * {@link GPTRequestDispatcher} requires an OpenAI API key and a GPT model to be specified in a {@link Configuration}
 * object.
 */
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

    /**
     * Sends a chat completion request containing a list of messages to GPT API.
     * @param messages the list of messages to be included in the request
     * @return a chat completion object representing the body of the response from GPT
     * @throws GPTException if an error occurs while sending the request. This exception may be thrown if API key or GPT
     * model are missing, if the list of messages is badly formatted, if the {@link GPTRequestDispatcher} is unable to
     * contact OpenAI API or receives an error in the response
     */
    public GPTCompletion sendChatCompletionRequestWithContext(List<GPTMessage> messages) throws GPTException {
        if(openaiAPIKey == null || openaiAPIKey.isEmpty()) {
            logger.warn("OpenAI API key is missing in configuration file");
            throw new GPTException();
        }
        if(model == null || model.isEmpty()) {
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
        } catch(URISyntaxException | IOException | InterruptedException e) {
            logger.warn("Sending of request to ChatGPT failed");
            throw new GPTException();
        } catch(JsonSyntaxException e) {
            logger.warn("Gson failed to convert ChatGPT response into a valid Java object");
            throw new GPTException();
        }
        return completion;
    }

    /**
     * Sends a chat completion request containing a message to GPT API.
     * @param message the message to be included in the request
     * @return a chat completion object representing the body of the response from GPT
     * @throws GPTException if an error occurs while sending the request. This exception may be thrown if API key or GPT
     * model are missing, if the message is badly formatted, if the {@link GPTRequestDispatcher} is unable to contact
     * OpenAI API or receives an error in the response
     */
    public GPTCompletion sendChatCompletionRequest(GPTMessage message) throws GPTException {
        List<GPTMessage> messages = new ArrayList<>();
        messages.add(message);
        return sendChatCompletionRequestWithContext(messages);
    }

}
