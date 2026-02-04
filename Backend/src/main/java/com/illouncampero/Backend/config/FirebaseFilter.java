package com.illouncampero.Backend.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class FirebaseFilter extends OncePerRequestFilter {

    private final Firestore db; // 1. Añadimos la variable

    // 2. Creamos el constructor para recibir la db desde SecurityConfig
    public FirebaseFilter(Firestore db) {
        this.db = db;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // Validar el Token con Firebase
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decodedToken.getUid();
                System.out.println("DEBUG FILTER: Token de Firebase válido. UID: " + uid);

                // 3. USAMOS la db inyectada (borramos la llamada a FirestoreClient)
                DocumentSnapshot userDoc = db.collection("usuarios").document(uid).get().get();

                String rol = "CLIENTE";
                if (userDoc.exists() && userDoc.getString("rol") != null) {
                    rol = userDoc.getString("rol");
                } else {
                    // SI SALE ESTE LOG, EL PROBLEMA ES QUE EL DOCUMENTO EN FIRESTORE NO SE LLAMA COMO EL UID
                    System.out.println("DEBUG FILTER: ERROR - No existe el documento '" + uid + "' en la colección 'usuarios'");
                }

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())
                );

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        uid, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                System.out.println("DEBUG FILTER: Error validando token: " + e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}