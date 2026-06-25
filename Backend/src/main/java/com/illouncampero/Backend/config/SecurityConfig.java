package com.illouncampero.Backend.config;

import com.illouncampero.Backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UsuarioRepository usuarioRepository;
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            UsuarioRepository usuarioRepository,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String supabaseAnonKey,
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String allowedOrigins) {
        this.usuarioRepository = usuarioRepository;
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                .referrerPolicy(ref -> ref
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy", "camera=(), microphone=(), geolocation=()"
                ))
            )
            .authorizeHttpRequests(auth -> auth
                // Preflight CORS
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Swagger y Actuator bloqueados siempre (application-prod.properties los deshabilita en prod)
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                 "/swagger-resources/**", "/webjars/**").denyAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                // API pública
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/usuarios/registro").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tienda/estado").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resenas").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/push/public-key").permitAll()
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
                // Cancelación y confirmación de pago por el cliente
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/cancelar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/confirmar-pago").authenticated()
                // Web Push (suscripcion del navegador)
                .requestMatchers(HttpMethod.POST, "/api/push/subscribe").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/push/unsubscribe").authenticated()
                // Autenticado
                .requestMatchers(HttpMethod.DELETE, "/api/usuarios/cuenta").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/resenas").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/cupones/validar").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/pagos/webhook").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/pagos/crear-intent").authenticated()
                .requestMatchers("/api/pedidos/**").authenticated()
                .requestMatchers("/api/usuarios/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new SupabaseJwtFilter(usuarioRepository, supabaseUrl, supabaseAnonKey),
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
