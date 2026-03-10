package com.illouncampero.Backend.config;

import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

                // Swagger
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                // Productos: GET público, escritura solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")

                // Registro: público
                .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()

                // Tienda: GET público, PATCH solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/tienda/estado").permitAll()
                .requestMatchers(HttpMethod.PATCH, "/api/tienda/estado").hasRole("ADMIN")

                // Reseñas: GET público, POST autenticado
                .requestMatchers(HttpMethod.GET, "/api/resenas").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/resenas").authenticated()

                // Cupones: validar autenticado, gestión solo ADMIN
                .requestMatchers(HttpMethod.POST, "/api/cupones/validar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cupones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/cupones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/cupones/**").hasRole("ADMIN")

                // Pedidos activos y estadísticas: ADMIN o COCINA
                .requestMatchers(HttpMethod.GET, "/api/pedidos/activos").hasAnyRole("ADMIN", "COCINA")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/estadisticas/**").hasAnyRole("ADMIN", "COCINA")
                .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasAnyRole("ADMIN", "COCINA")

                // Pedidos: autenticados
                .requestMatchers("/api/pedidos/**").authenticated()

                // Usuarios: autenticados
                .requestMatchers("/api/usuarios/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(new FirebaseFilter(db), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
