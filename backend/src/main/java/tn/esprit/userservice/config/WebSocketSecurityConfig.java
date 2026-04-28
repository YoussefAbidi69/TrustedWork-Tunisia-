package tn.esprit.userservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import tn.esprit.userservice.security.JwtService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        if (jwtService.isTokenValid(token)) {
                            String email = jwtService.extractEmail(token);
                            String roles = jwtService.extractRoles(token);

                            List<SimpleGrantedAuthority> authorities;
                            if (roles != null && !roles.isBlank()) {
                                authorities = Arrays.stream(roles.split(","))
                                        .map(String::trim)
                                        .filter(role -> !role.isEmpty())
                                        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                                        .map(SimpleGrantedAuthority::new)
                                        .toList();
                            } else {
                                authorities = Collections.emptyList();
                            }

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(email, null, authorities);
                            accessor.setUser(auth);
                        } else {
                            throw new IllegalArgumentException("Invalid JWT token");
                        }
                    } else {
                        throw new IllegalArgumentException("Missing JWT token");
                    }
                }
                return message;
            }
        });
    }
}
