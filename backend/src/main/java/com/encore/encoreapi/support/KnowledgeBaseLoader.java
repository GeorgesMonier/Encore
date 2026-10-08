package com.encore.encoreapi.support;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeBaseLoader implements CommandLineRunner {

    private final VectorStore vectorStore;

    public KnowledgeBaseLoader(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        List<Document> documents = List.of(
                new Document("Para comprar entradas en Encore, primero debes crear una cuenta y verificar tu correo electrónico. Después puedes navegar los eventos disponibles y seleccionar el tipo de entrada que quieras."),
                new Document("La política de reembolsos de Encore permite cancelar una compra dentro de las 24 horas siguientes a la compra, siempre que el evento no haya ocurrido todavía. Después de ese plazo, no se garantizan reembolsos."),
                new Document("Encore permite activar la verificación en dos pasos (2FA) desde la configuración de tu cuenta, usando una aplicación como Google Authenticator para mayor seguridad."),
                new Document("Los pagos en Encore se procesan de forma segura a través de Stripe. Nunca almacenamos los datos de tu tarjeta directamente en nuestros servidores."),
                new Document("Si una compra no se completa en 15 minutos, la reserva de entradas se libera automáticamente y las entradas vuelven a estar disponibles para otros usuarios.")
        );

        vectorStore.add(documents);
        System.out.println("Base de conocimiento cargada: " + documents.size() + " documentos.");
    }
}