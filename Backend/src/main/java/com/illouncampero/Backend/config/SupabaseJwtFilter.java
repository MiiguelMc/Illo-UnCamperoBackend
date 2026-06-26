package com.illouncampero.Backend.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.illouncampero.Backend.model.Usuario;
import com.illouncampero.Backend.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida el token de Supabase Auth contra el endpoint /auth/v1/user.
 *
 * Se valida en remoto (en vez de verificar la firma del JWT en local) para no
 * depender del algoritmo de firma del proyecto: Supabase puede firmar con HS256
 * (secreto legacy) o con claves asimétricas (ES256). El endpoint /auth/v1/user
 * valida el token sea cual sea el caso y devuelve el usuario.
 *
 * Para evitar una llamada por petición se cachea el resultado (token -> uid)
 * durante un tiempo corto; el mismo access_token se reutiliza en muchas peticiones
 * dentro de su hora de validez.
 */
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private static final long CACHE_TTL_MS = 120_000; // 2 minutos

    private final UsuarioRepository usuarioRepository;
    private final String supabaseUrl;
    private final String anonKey;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CachedUid> cache = new ConcurrentHashMap<>();

    private record CachedUid(String uid, long expiraEn) {}

    public SupabaseJwtFilter(UsuarioRepository usuarioRepository, String supabaseUrl, String anonKey) {
        this.usuarioRepository = usuarioRepository;
        this.supabaseUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        this.anonKey = anonKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                String uid = resolverUid(token);
                if (uid != null) {
                    String rol = usuarioRepository.findById(uid)
                            .map(Usuario::getRol)
                            .filter(r -> r != null && !r.isBlank())
                            .orElse("CLIENTE");

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                            new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase())
                    );

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            uid, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolverUid(String token) throws IOException, InterruptedException {
        long ahora = System.currentTimeMillis();

        CachedUid cached = cache.get(token);
        if (cached != null && cached.expiraEn() > ahora) {
            return cached.uid();
        }

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/auth/v1/user"))
                .header("Authorization", "Bearer " + token)
                .header("apikey", anonKey)
                .GET()
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            cache.remove(token);
            return null;
        }

        JsonNode body = objectMapper.readTree(res.body());
        String uid = body.path("id").asText(null);
        if (uid != null && !uid.isBlank()) {
            cache.put(token, new CachedUid(uid, ahora + CACHE_TTL_MS));
            // Limpieza oportunista para que el mapa no crezca sin límite.
            if (cache.size() > 1000) {
                cache.entrySet().removeIf(e -> e.getValue().expiraEn() <= ahora);
            }
            return uid;
        }
        return null;
    }
}
