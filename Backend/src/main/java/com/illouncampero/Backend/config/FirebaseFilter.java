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
import java.util.Set;

public class FirebaseFilter extends OncePerRequestFilter {

    // Endpoints que requieren email verificado
    private static final Set<String> RUTAS_SENSIBLES = Set.of(
        "/api/pedidos", "/api/pagos", "/api/resenas"
    );

    private final Firestore db;

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
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                String uid = decodedToken.getUid();

                // Bloquear rutas sensibles si el email no está verificado
                String path = request.getRequestURI();
                boolean rutaSensible = RUTAS_SENSIBLES.stream().anyMatch(path::startsWith);
                if (rutaSensible && !decodedToken.isEmailVerified()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Debes verificar tu email antes de continuar\"}");
                    return;
                }

                DocumentSnapshot userDoc = db.collection("usuarios").document(uid).get().get();

                String rol = "CLIENTE";
                if (userDoc.exists() && userDoc.getString("rol") != null) {
                    rol = userDoc.getString("rol");
                }

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())
                );

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        uid, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
