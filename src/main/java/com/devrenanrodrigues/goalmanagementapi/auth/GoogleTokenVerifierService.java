package com.devrenanrodrigues.goalmanagementapi.auth;

import com.devrenanrodrigues.goalmanagementapi.auth.dto.GoogleUserInfo;
import com.devrenanrodrigues.goalmanagementapi.common.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class GoogleTokenVerifierService {

    private final RestClient restClient;

    @Value("${security.google.client-id}")
    private String googleClientId;

    public GoogleTokenVerifierService() {
        this.restClient = RestClient.create();
    }

    public GoogleUserInfo verifyToken(String token) {
        if (isJwtFormat(token)) {
            return verifyIdToken(token);
        }
        return verifyAccessToken(token);
    }

    private boolean isJwtFormat(String token) {
        return token.split("\\.").length == 3;
    }

    private GoogleUserInfo verifyIdToken(String idToken) {
        try {
            Map<String, Object> tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            validateAudience(tokenInfo);

            String sub = (String) tokenInfo.get("sub");
            String email = (String) tokenInfo.get("email");
            String name = (String) tokenInfo.get("name");
            String picture = (String) tokenInfo.get("picture");

            if (sub == null || email == null) {
                throw new UnauthorizedException("Não foi possível obter os dados da conta Google.");
            }

            return new GoogleUserInfo(sub, email, name, picture);
        } catch (RestClientException ex) {
            throw new UnauthorizedException("Token do Google inválido ou expirado.");
        }
    }

    private GoogleUserInfo verifyAccessToken(String accessToken) {
        try {
            Map<String, Object> tokenInfo = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?access_token={token}", accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            validateAudience(tokenInfo);

            Map<String, Object> userInfo = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (userInfo == null) {
                throw new UnauthorizedException("Não foi possível obter o perfil do usuário no Google.");
            }

            String sub = (String) userInfo.get("sub");
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            String picture = (String) userInfo.get("picture");

            if (sub == null || email == null) {
                throw new UnauthorizedException("Não foi possível obter o e-mail da conta Google.");
            }

            return new GoogleUserInfo(sub, email, name, picture);
        } catch (RestClientException ex) {
            throw new UnauthorizedException("Token do Google inválido ou expirado.");
        }
    }

    private void validateAudience(Map<String, Object> tokenInfo) {
        if (tokenInfo == null) {
            throw new UnauthorizedException("Token do Google inválido.");
        }

        String aud = (String) tokenInfo.get("aud");
        String azp = (String) tokenInfo.get("azp");

        boolean validAudience = googleClientId.equals(aud) || googleClientId.equals(azp);
        if (!validAudience) {
            throw new UnauthorizedException("O token do Google não pertence a esta aplicação.");
        }
    }
}

