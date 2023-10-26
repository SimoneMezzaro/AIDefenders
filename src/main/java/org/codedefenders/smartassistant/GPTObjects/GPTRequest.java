package org.codedefenders.smartassistant.GPTObjects;

import java.util.List;

public class GPTRequest {
    private String model;
    private List<GPTMessage> messages;
    private Double temperature;

    public GPTRequest(String model, List<GPTMessage> messages, Double temperature) {
        this.model = model;
        this.messages = messages;
        if(temperature < 0.0 || temperature > 2.0) {
            this.temperature = 0.0;
        } else {
            this.temperature = temperature;
        }
    }
}
