package tn.esprit.userservice.service.chat;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
public class AgencyPresenceService {

    // agencyId -> Set of userId
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<Long>> onlineUsers = new ConcurrentHashMap<>();

    public void userJoined(Long agencyId, Long userId) {
        onlineUsers.computeIfAbsent(agencyId, k -> new CopyOnWriteArraySet<>()).add(userId);
    }

    public void userLeft(Long agencyId, Long userId) {
        CopyOnWriteArraySet<Long> users = onlineUsers.get(agencyId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                onlineUsers.remove(agencyId);
            }
        }
    }

    public Set<Long> getOnlineUsers(Long agencyId) {
        CopyOnWriteArraySet<Long> users = onlineUsers.get(agencyId);
        return users != null ? users : Set.of();
    }

    public boolean isOnline(Long agencyId, Long userId) {
        CopyOnWriteArraySet<Long> users = onlineUsers.get(agencyId);
        return users != null && users.contains(userId);
    }
}
