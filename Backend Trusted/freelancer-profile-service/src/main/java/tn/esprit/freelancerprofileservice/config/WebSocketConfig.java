package tn.esprit.freelancerprofileservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Endpoint de connexion WebSocket natif (sans SockJS)
     * Compatible avec @stomp/stompjs côté Angular 18 + Vite
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket natif (utilisé par @stomp/stompjs)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");

        // Endpoint SockJS fallback (navigateurs restrictifs, proxy, Docker)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Configuration du broker de messages
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Préfixe pour les messages envoyés depuis le backend vers les controllers
        registry.setApplicationDestinationPrefixes("/app");

        // Topics auxquels les clients peuvent s'abonner
        registry.enableSimpleBroker("/topic", "/queue");
    }
}