package com.illouncampero.Backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    // La base de datos Firestore está en la región europe-southwest1 (Madrid).
    // Esa región se sirve por un endpoint regional propio; el endpoint global por
    // gRPC devuelve 403. Hay que apuntar el cliente de Firestore a la dirección regional.
    private static final String FIRESTORE_HOST = "firestore.europe-southwest1.rep.googleapis.com:443";
    private static final String PROJECT_ID = "illo-uncampero";

    private static String firebaseJson() {
        String json = System.getenv("FIREBASE_JSON");
        if (json == null || json.isEmpty()) {
            throw new IllegalStateException("ERROR: La variable FIREBASE_JSON no está configurada.");
        }
        return json;
    }

    private static GoogleCredentials credentials() throws IOException {
        InputStream stream = new ByteArrayInputStream(firebaseJson().getBytes(StandardCharsets.UTF_8));
        return GoogleCredentials.fromStream(stream);
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials())
                .build();
        System.out.println("Firebase inicializado correctamente.");
        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) throws IOException {
        // Cliente de Firestore apuntando explícitamente al endpoint regional de Madrid.
        return FirestoreOptions.newBuilder()
                .setProjectId(PROJECT_ID)
                .setHost(FIRESTORE_HOST)
                .setCredentials(credentials())
                .build()
                .getService();
    }
}
