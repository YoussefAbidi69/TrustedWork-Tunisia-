package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.smartjobboard.dto.UserReferenceDto;

/**
 * Calls user-service REST API using the end-user JWT (typically {@code GET /users/me}).
 */
@Service
@RequiredArgsConstructor
public class UserRestClient {

    private final RestTemplate restTemplate;

    @Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    public UserReferenceDto fetchCurrentUser(String authorizationHeaderValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<UserReferenceDto> res = restTemplate.exchange(
                    userServiceBaseUrl + "/users/me",
                    HttpMethod.GET,
                    entity,
                    UserReferenceDto.class
            );
            if (res.getBody() == null) {
                throw new RestClientException("Empty body from user-service /users/me");
            }
            return res.getBody();
        } catch (RestClientException e) {
            throw new RestClientException("Failed to resolve current user from user-service: " + e.getMessage(), e);
        }
    }

    public UserReferenceDto fetchPublicUser(Long userId) {
        org.springframework.web.context.request.ServletRequestAttributes attributes = 
            (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        String authorizationHeaderValue = attributes != null ? attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION) : null;

        HttpHeaders headers = new HttpHeaders();
        if (authorizationHeaderValue != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<UserReferenceDto> res = restTemplate.exchange(
                    userServiceBaseUrl + "/users/" + userId + "/public",
                    HttpMethod.GET,
                    entity,
                    UserReferenceDto.class
            );
            return res.getBody();
        } catch (RestClientException e) {
            // Fallback gracefully if we can't fetch the user
            return null;
        }
    }
}
