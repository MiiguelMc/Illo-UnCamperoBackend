package com.illouncampero.Backend.config;

import com.google.cloud.firestore.Firestore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
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
                config.setAllowedOrigins(List.of(
                    "http://localhost:4200",
                    "https://illo-uncampero.web.app",
                    "https://illo-uncampero.firebaseapp.com"
                ));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
                config.setAllowedHeaders(List.of("*"));
                return config;
            }))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; connect-src 'self' https://firebaseapp.com https://googleapis.com; object-src 'none'")
                )
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .permissionsPolicy(perm -> perm
                    .policy("camera=(), microphone=(), geolocation=()")
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Swagger y Actuator bloqueados siempre (application-prod.properties los deshabilita en prod)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                 "/swagger-resources/**", "/webjars/**").denyAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                // API pública
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tienda/estado").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resenas").permitAll()
                // Solo ADMIN
                .requestMatchers(HttpMethod.GET, "/api/cloudinary/firma").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/tienda/estado").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/cupones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/cupones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/cupones/**").hasRole("ADMIN")
                // ADMIN o COCINA
                .requestMatchers(HttpMethod.GET, "/api/pedidos/activos").hasAnyRole("ADMIN", "COCINA")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/todos").hasAnyRole("ADMIN", "COCINA")
                .requestMatchers(HttpMethod.GET, "/api/pedidos/estadisticas/**").hasAnyRole("ADMIN", "COCINA")
                .requestMatchers(HttpMethod.PATCH, "/api/pedidos/*/estado").hasAnyRole("ADMIN", "COCINA")
                // Autenticado
                .requestMatchers(HttpMethod.DELETE, "/api/usuarios/cuenta").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/resenas").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cupones/validar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/pagos/crear-intent").authenticated()
                .requestMatchers("/api/pedidos/**").authenticated()
                .requestMatchers("/api/usuarios/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new FirebaseFilter(db), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
