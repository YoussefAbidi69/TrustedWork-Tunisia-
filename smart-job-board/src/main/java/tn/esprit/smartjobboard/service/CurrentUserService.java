package tn.esprit.smartjobboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tn.esprit.smartjobboard.dto.UserReferenceDto;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the authenticated caller against user-service for authoritative {@code id} and {@code role}.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRestClient userRestClient;

    public UserReferenceDto requireCurrentUser() {
        String bearer = getAuthorizationHeader();
        if (bearer == null || !bearer.startsWith("Bearer ")) {
            throw new AccessDeniedException("Missing or invalid Authorization header.");
        }
        return userRestClient.fetchCurrentUser(bearer);
    }

    public String getAuthorizationHeader() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest req = attrs.getRequest();
        return req.getHeader("Authorization");
    }
}
