package org.codedefenders.smartassistant;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.smartassistant.response.objects.ChatGPTCompletion;
import org.codedefenders.smartassistant.response.objects.ChatGPTMessage;

import com.google.gson.Gson;

public class ChatGPTRequestDispatcher {
    private final String openaiAPIKey;
    private final String model;

    @Inject
    public ChatGPTRequestDispatcher(Configuration config) {
        openaiAPIKey = config.getOpenaiApiKey();
        model = config.getOpenaiChatgptModel();
    }

    public ChatGPTCompletion sendChatCompletionRequest(ChatGPTMessage message) {
        Gson gson = new Gson();
        HttpClient client = HttpClient.newBuilder().build();
        ChatGPTCompletion completion = null;
        try {
            URI uri = new URI("https://api.openai.com/v1/chat/completions");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Authorization", "Bearer " + openaiAPIKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildJsonBodyString(gson, message)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            //TODO: check http response status code
            completion = gson.fromJson(response.body(), ChatGPTCompletion.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            //TODO: manage exceptions
            System.out.println("EXCEPTION: " + e.getMessage() + "\nCaused by: " + e.getCause());
            e.printStackTrace();
        }
        return completion;
    }

    private String buildJsonBodyString(Gson gson, ChatGPTMessage message) {
        return "{\"model\":\"" + model + "\",\"messages\":[" + gson.toJson(message) + "]}";
    }

}
