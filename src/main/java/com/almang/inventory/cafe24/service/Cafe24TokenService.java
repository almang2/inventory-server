package com.almang.inventory.cafe24.service;

import com.almang.inventory.cafe24.domain.Cafe24Token;
import com.almang.inventory.cafe24.dto.Cafe24TokenResponse;
import com.almang.inventory.cafe24.repository.Cafe24TokenRepository;
import com.almang.inventory.global.config.properties.Cafe24Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24TokenService {

    private final Cafe24TokenRepository cafe24TokenRepository;
    private final Cafe24Properties cafe24Properties;
    private final RestClient restClient = RestClient.create();

    @Transactional
    public void saveTokenFromResponse(Cafe24TokenResponse response) {
        LocalDateTime issuedAt = response.issuedAt() != null
                ? LocalDateTime.parse(response.issuedAt(), DateTimeFormatter.ISO_DATE_TIME)
                : LocalDateTime.now();

        LocalDateTime expiresAt = response.expiresAt() != null
                ? LocalDateTime.parse(response.expiresAt(), DateTimeFormatter.ISO_DATE_TIME)
                : issuedAt.plusHours(2); // Default to 2 hours if missing

        LocalDateTime refreshTokenExpiresAt = response.refreshTokenExpiresAt() != null
                ? LocalDateTime.parse(response.refreshTokenExpiresAt(), DateTimeFormatter.ISO_DATE_TIME)
                : issuedAt.plusWeeks(2); // Default to 2 weeks if missing

        Cafe24Token token = cafe24TokenRepository.findById(response.mallId())
                .orElse(new Cafe24Token(response.mallId(), response.accessToken(), response.refreshToken(), issuedAt,
                        expiresAt, refreshTokenExpiresAt));

        token.updateToken(response.accessToken(), response.refreshToken(), issuedAt, expiresAt, refreshTokenExpiresAt);
        cafe24TokenRepository.save(token);
    }

    @Transactional
    public void exchangeCodeForToken(String code) {
        try {
            String basicAuth = Base64.getEncoder()
                    .encodeToString((cafe24Properties.getClientId() + ":" + cafe24Properties.getClientSecret())
                            .getBytes(StandardCharsets.UTF_8));

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("grant_type", "authorization_code");
            formData.add("code", code);
            formData.add("redirect_uri", cafe24Properties.getRedirectUri());

            String tokenUrl = UriComponentsBuilder.fromUriString(cafe24Properties.getOauthUrl())
                    .pathSegment("api", "v2", "oauth", "token")
                    .build()
                    .toUriString();

            log.info("Requesting token from: {}", tokenUrl);

            String jsonResponse = restClient.post()
                    .uri(tokenUrl)
                    .header("Authorization", "Basic " + basicAuth)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(String.class);

            log.info("Cafe24 Token Response: {}", jsonResponse);

            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);
            Cafe24TokenResponse response = objectMapper.readValue(jsonResponse, Cafe24TokenResponse.class);

            if (response != null) {
                saveTokenFromResponse(response);
                log.info("Cafe24 token saved for mall: {}", response.mallId());
            } else {
                throw new IllegalStateException("Failed to retrieve Cafe24 token");
            }
        } catch (Exception e) {
            log.error("Error exchanging code for token", e);
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    @Transactional
    public String getAccessToken(String mallId) {
        Cafe24Token token = cafe24TokenRepository.findById(mallId)
                .orElseThrow(() -> new IllegalStateException("No token found for mallId: " + mallId));

        if (token.getExpiresAt().isBefore(LocalDateTime.now().plusMinutes(5))) {
            log.info("Access token for mall {} is expired or expiring soon. Refreshing...", mallId);
            return refreshToken(token);
        }

        return token.getAccessToken();
    }

    private String refreshToken(Cafe24Token token) {
        String basicAuth = Base64.getEncoder()
                .encodeToString((cafe24Properties.getClientId() + ":" + cafe24Properties.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", token.getRefreshToken());

        Cafe24TokenResponse response = restClient.post()
                .uri(cafe24Properties.getOauthUrl() + "/api/v2/oauth/token")
                .header("Authorization", "Basic " + basicAuth)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Cafe24TokenResponse.class);

        if (response == null) {
            throw new IllegalStateException("Failed to refresh token");
        }

        saveTokenFromResponse(response);
        return response.accessToken();
    }
}
