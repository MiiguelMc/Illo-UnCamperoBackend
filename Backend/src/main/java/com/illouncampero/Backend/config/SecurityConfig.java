package com.illouncampero.Backend.config;

import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // <--- IMPORTANTE: Debe ser este import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final Firestore db;

    public SecurityConfig(Firestore db) {
        this.db = db;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
                    config.setAllowedHeaders(List.of("*"));
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        // 1. Swagger y documentación siempre públicos
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 2. Productos: lectura pública, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")

                        // 3. Usuarios (Ajustado para seguridad del TFG):
                        // El registro debe ser público para que nuevos clientes entren
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                        // El resto (ver perfil o actualizar) requiere estar LOGUEADO
                        .requestMatchers("/api/usuarios/**").authenticated()

                        .requestMatchers("/api/pedidos/**").permitAll()
                        .requestMatchers("/api/usuarios/**").permitAll()
                        // 4. Todo lo demás (pedidos, etc.) requiere estar autenticado
                        .anyRequest().authenticated()
                )
                // 5. Añadimos tu filtro inyectándole la base de datos
                .addFilterBefore(new FirebaseFilter(db), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}