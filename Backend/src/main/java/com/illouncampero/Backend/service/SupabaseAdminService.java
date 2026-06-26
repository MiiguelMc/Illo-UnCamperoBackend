package com.illouncampero.Backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Operaciones de administracion contra Supabase Auth (GoTrue) usando la
 * service_role key (p. ej. eliminar un usuario por su uid).
 */
@Service
public class SupabaseAdminService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAdminService.class);

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final HttpClient http = HttpClient.newHttpClient();

    public SupabaseAdminService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-role-key}") String serviceRoleKey) {
        this.supabaseUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
    }

    /** Borra el usuario de Supabase Auth. Lanza excepcion si la API responde error. */
    public void deleteUser(String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(supabaseUrl + "/auth/v1/admin/users/" + uid))
                .header("Authorization", "Bearer " + serviceRoleKey)
                .header("apikey", serviceRoleKey)
                .DELETE()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            log.error("Error al borrar usuario en Supabase Auth ({}): {}", response.statusCode(), response.body());
            throw new IllegalStateException("No se pudo borrar el usuario en Supabase Auth (HTTP " + response.statusCode() + ")");
        }
    }
}
