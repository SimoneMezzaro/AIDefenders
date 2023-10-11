package org.codedefenders.smartassistant.exceptions;

public class ChatGPTException extends Exception{

    public ChatGPTException(){ }

    public ChatGPTException(String message) {
        super(message);
    }

}
