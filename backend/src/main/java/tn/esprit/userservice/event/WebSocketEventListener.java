package tn.esprit.userservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import tn.esprit.userservice.dto.chat.PresenceEventDTO;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.repository.UserRepository;
import tn.esprit.userservice.service.chat.AgencyChatService;
import tn.esprit.userservice.service.chat.AgencyPresenceService;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final AgencyPresenceService presenceService;
    private final AgencyChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/agency/") && destination.endsWith("/presence")) {
            try {
                String agencyIdStr = destination.split("/")[3];
                Long agencyId = Long.parseLong(agencyIdStr);
                
                String email = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
                if (email != null) {
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        Long userId = user.getId();
                        
                        // Store agencyId and userId in session
                        headerAccessor.getSessionAttributes().put("agencyId", agencyId);
                        headerAccessor.getSessionAttributes().put("userId", userId);
                        
                        presenceService.userJoined(agencyId, userId);
                        
                        PresenceEventDTO eventDTO = PresenceEventDTO.builder()
                                .agencyId(agencyId)
                                .userId(userId)
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .photo(user.getPhoto())
                                .event("JOIN")
                                .timestamp(java.time.Instant.now().toString())
                                .currentOnlineMembers(chatService.getOnlineMembersList(agencyId))
                                .build();
                                
                        messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/presence", eventDTO);
                    }
                }
            } catch (Exception e) {
                // Ignore parse errors or other exceptions during presence broadcast
            }
        }
    }

    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        
        if (sessionAttributes != null && sessionAttributes.containsKey("agencyId") && sessionAttributes.containsKey("userId")) {
            Long agencyId = (Long) sessionAttributes.get("agencyId");
            Long userId = (Long) sessionAttributes.get("userId");
            
            presenceService.userLeft(agencyId, userId);
            
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                PresenceEventDTO eventDTO = PresenceEventDTO.builder()
                        .agencyId(agencyId)
                        .userId(userId)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .photo(user.getPhoto())
                        .event("LEAVE")
                        .timestamp(java.time.Instant.now().toString())
                        .currentOnlineMembers(chatService.getOnlineMembersList(agencyId))
                        .build();
                        
                messagingTemplate.convertAndSend("/topic/agency/" + agencyId + "/presence", eventDTO);
            }
        }
    }
}
