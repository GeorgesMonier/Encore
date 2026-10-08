package com.encore.encoreapi.support;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public ResponseEntity<?> ask(@Valid @RequestBody AskRequest request) {
        String answer = supportService.answer(request.getQuestion());
        return ResponseEntity.ok(new AskResponse(answer));
    }

    public static class AskRequest {
        @NotBlank(message = "Escribe una consulta.")
        @Size(max = 1000, message = "La consulta no puede superar los 1000 caracteres.")
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