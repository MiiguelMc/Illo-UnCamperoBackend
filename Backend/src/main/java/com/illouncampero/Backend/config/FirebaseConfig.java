package com.illouncampero.Backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class FirebaseConfig {

    @Bean
    public void init() {
        try {
            String firebaseConfig = System.getenv("FIREBASE_JSON");

            if (firebaseConfig == null) {
                System.err.println("ERROR: La variable FIREBASE_JSON no está configurada.");
                return;
            }

            InputStream serviceAccount = new ByteArrayInputStream(firebaseConfig.getBytes(StandardCharsets.UTF_8));

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("¡Firebase inicializado con éxito!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}