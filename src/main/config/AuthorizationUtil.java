package config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import exception.ResourceNotFoundException;

@Component
public class AuthorizationUtil {
    
    /**
     * Gets the current user ID from the JWT token
     */
    public Integer getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            try {
                return Integer.parseInt(auth.getName());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    

    public Integer getCurrentClientId() {
        return isClient() ? getCurrentUserId() : null;
    }

    /**
     * Checks if the current user has ADMIN role
     */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
        }
        return false;
    }
    
    /**
     * Checks if the current user has CLIENT role
     */
    public boolean isClient() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_CLIENT"));
        }
        return false;
    }
    
    /**
     * Checks if current user owns the account or is admin
     * Throws ResourceNotFoundException if unauthorized
     */
    public void checkAccountAccess(Integer accountId, Integer accountOwnerId) {
        Integer currentUserId = getCurrentUserId();
        if (currentUserId == null) {
            throw new ResourceNotFoundException("Account", accountId.toString());
        }
        
        if (!isAdmin() && !currentUserId.equals(accountOwnerId)) {
            throw new ResourceNotFoundException("Account", accountId.toString());
        }
    }
    

    public void checkUserAccess(Integer userId) {
        Integer currentUserId = getCurrentUserId();
        if (!isAdmin() && (currentUserId == null || !currentUserId.equals(userId))) {
            throw new ResourceNotFoundException("User", userId.toString());
        }
    }

    /**
     * Checks if current user is admin
     * Throws ResourceNotFoundException if not authorized
     */
    public void checkAdminAccess() {
        if (!isAdmin()) {
            throw new ResourceNotFoundException("Resource", "Unauthorized");
        }
    }
}
