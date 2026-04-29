package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tn.esprit.smartjobboard.dto.UserReferenceDto;

@Service
@RequiredArgsConstructor
public class FreelancerProfileClient {

    private final RestTemplate restTemplate;
    private final CurrentUserService currentUserService;

    @org.springframework.beans.factory.annotation.Value("${user.service.base-url}")
    private String userServiceBaseUrl;

    public UserReferenceDto fetchFreelancerProfile(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        String auth = currentUserService.getAuthorizationHeader();
        if (auth != null) {
            headers.set(HttpHeaders.AUTHORIZATION, auth);
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
        } catch (RestClientException ex) {
            return null;
        }
    }
}
