package com.almang.inventory.user.auth.service;

import com.almang.inventory.global.config.properties.Cafe24Properties;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24AuthService {

    private final Cafe24Properties cafe24Properties;
    private final RestTemplate restTemplate; // RestTemplateConfig에서 빈으로 등록된 RestTemplate을 주입받습니다.
    private final RedisService redisService; // Access Token 저장을 위한 RedisService

    /**
     * 카페24 OAuth 인증 코드를 Access Token으로 교환합니다.
     * 
     * @param code 카페24로부터 받은 인증 코드
     * @return Access Token
     * @throws BaseException Access Token 교환 실패 시
     */
    public String exchangeCodeForAccessToken(String code) {
        log.info("카페24 인증 코드를 Access Token으로 교환 시작: code={}", code);

        // 카페24 OAuth Token 엔드포인트 URL 구성
        // OAuth URL이 "https://[mall_id].cafe24api.com/api/v2/oauth/authorize" 형태라면,
        // token URL은 "https://[mall_id].cafe24api.com/api/v2/oauth/token"이 됩니다.
        String tokenUrl = cafe24Properties.getOauthUrl().replace("/authorize", "/token");

        // 요청 본문 구성 (카페24 API 문서에 따라)
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("code", code);
        requestBody.add("redirect_uri", cafe24Properties.getRedirectUri());
        requestBody.add("client_id", cafe24Properties.getClientId());
        requestBody.add("client_secret", cafe24Properties.getClientSecret());

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            // 카페24 API에 POST 요청 전송
            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                String accessToken = (String) responseBody.get("access_token");
                
                if (accessToken == null || accessToken.isEmpty()) {
                    log.error("카페24 Access Token 응답에 access_token이 없습니다. 응답: {}", responseBody);
                    throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "카페24 Access Token 응답에 access_token이 없습니다.");
                }

                // expires_in 추출 (초 단위, 기본값: 3600초 = 1시간)
                Object expiresInObj = responseBody.get("expires_in");
                long expiresInSeconds = 3600; // 기본값
                if (expiresInObj != null) {
                    if (expiresInObj instanceof Number) {
                        expiresInSeconds = ((Number) expiresInObj).longValue();
                    } else if (expiresInObj instanceof String) {
                        try {
                            expiresInSeconds = Long.parseLong((String) expiresInObj);
                        } catch (NumberFormatException e) {
                            log.warn("카페24 Access Token 응답의 expires_in을 파싱할 수 없습니다: {}", expiresInObj);
                        }
                    }
                }

                // Redis에 Access Token 저장
                redisService.saveCafe24AccessToken(accessToken, expiresInSeconds);
                log.info("카페24 Access Token 획득 및 저장 성공: expires_in={}초", expiresInSeconds);

                return accessToken;
            } else {
                log.error("카페24 Access Token 교환 실패: HTTP 상태={}, 응답={}", response.getStatusCode(), response.getBody());
                throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "카페24 Access Token 교환 실패");
            }
        } catch (HttpClientErrorException e) {
            log.error("카페24 Access Token 교환 중 HTTP 오류 발생: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "카페24 Access Token 교환 중 오류 발생: " + e.getMessage());
        } catch (Exception e) {
            log.error("카페24 Access Token 교환 중 예상치 못한 오류 발생", e);
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR, "카페24 Access Token 교환 중 예상치 못한 오류 발생: " + e.getMessage());
        }
    }

    /**
     * 저장된 카페24 Access Token을 조회합니다.
     * 
     * @return 카페24 Access Token (없으면 null)
     */
    public String getCafe24AccessToken() {
        return redisService.getCafe24AccessToken();
    }

    /**
     * 저장된 카페24 Access Token이 있는지 확인합니다.
     * 
     * @return Access Token이 존재하면 true, 없으면 false
     */
    public boolean hasCafe24AccessToken() {
        String accessToken = redisService.getCafe24AccessToken();
        return accessToken != null && !accessToken.isEmpty();
    }
}

