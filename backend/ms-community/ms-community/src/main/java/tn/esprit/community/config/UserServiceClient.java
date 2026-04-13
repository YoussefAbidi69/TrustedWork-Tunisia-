package tn.esprit.community.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserServiceClient {
    private final WebClient msUserClient;

    public UserServiceClient(WebClient msUserClient) {
        this.msUserClient = msUserClient;
    }

    public UserPublicDTO getUserById(Long userId) {
        return msUserClient.get()
                .uri("/users/{id}/public", userId)
                .retrieve()
                .bodyToMono(UserPublicDTO.class)
                .block();
    }

    public String getTrustLevel(Long userId) {
        return msUserClient.get()
                .uri("/users/{id}/trust-level", userId)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
