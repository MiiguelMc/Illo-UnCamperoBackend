package com.illouncampero.Backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @PostConstruct // <--- ESTO ES LO QUE HACE QUE FUNCIONE SIN SER @BEAN
    public void init() {
        try {
            String firebaseConfig = System.getenv("FIREBASE_JSON");

            if (firebaseConfig == null || firebaseConfig.isEmpty()) {
                System.err.println("ERROR: La variable FIREBASE_JSON no está configurada en Render.");
                return;
            }

            InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("¡Firebase inicializado con éxito! ✅");
            }
        } catch (Exception e) {
            System.err.println("Error al inicializar Firebase: " + e.getMessage());
        }
    }
}