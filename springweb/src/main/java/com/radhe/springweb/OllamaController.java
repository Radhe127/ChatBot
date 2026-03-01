package com.radhe.springweb;

import org.springframework.web.bind.annotation.*;
import org.springframework.ai.chat.model.ChatModel;

@RestController
class OllamaController{

    private final ChatModel chatModel;

    public OllamaController(ChatModel chatModel){
        this.chatModel = chatModel;
    }

    @GetMapping("/api/{message}")
    public String getAnswer(@PathVariable String message){

        String response = chatModel.call(message);
        return response;

    }
}