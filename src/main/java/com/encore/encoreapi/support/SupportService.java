package com.encore.encoreapi.support;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public SupportService(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    public String answer(String question) {
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(3)
                        .build()
        );

        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
                .system("Eres el asistente de soporte de Encore, una plataforma de venta de tickets para conciertos. " +
                        "Responde la pregunta del usuario basándote ÚNICAMENTE en este contexto:\n\n" + context +
                        "\n\nSi la pregunta no se puede responder con este contexto, di que no tienes esa información.")
                .user(question)
                .call()
                .content();
    }
}