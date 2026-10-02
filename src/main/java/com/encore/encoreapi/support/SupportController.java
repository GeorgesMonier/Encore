package com.encore.encoreapi.support;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody AskRequest request) {
        String answer = supportService.answer(request.getQuestion());
        return ResponseEntity.ok(new AskResponse(answer));
    }

    public static class AskRequest {
        private String question;
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
    }

    public static class AskResponse {
        private String answer;
        public AskResponse(String answer) { this.answer = answer; }
        public String getAnswer() { return answer; }
    }
}