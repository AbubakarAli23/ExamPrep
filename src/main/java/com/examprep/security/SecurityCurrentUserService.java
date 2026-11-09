package com.examprep.security;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityCurrentUserService implements CurrentUserService {

    @Override
    public UUID requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("You must be signed in to do that");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof StudentPrincipal student) {
            return student.getUserId();
        }
        throw new AccessDeniedException("You must be signed in to do that");
    }
}
