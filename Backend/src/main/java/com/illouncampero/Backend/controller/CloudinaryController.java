package com.illouncampero.Backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @GetMapping("/firma")
    public ResponseEntity<Map<String, String>> generarFirma() throws Exception {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        // Los parámetros que se van a enviar al upload, ordenados alfabéticamente
        String paramsAFirmar = "folder=productos&timestamp=" + timestamp + apiSecret;

        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(paramsAFirmar.getBytes(StandardCharsets.UTF_8));
        String signature = HexFormat.of().formatHex(hash);

        return ResponseEntity.ok(Map.of(
                "timestamp", timestamp,
                "signature", signature,
                "apiKey",    apiKey,
                "cloudName", cloudName
        ));
    }
}
