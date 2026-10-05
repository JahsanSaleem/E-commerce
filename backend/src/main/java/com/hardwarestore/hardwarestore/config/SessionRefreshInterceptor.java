package com.hardwarestore.hardwarestore.config;

import com.hardwarestore.hardwarestore.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessionRefreshInterceptor implements HandlerInterceptor {
    private final UserRepository users;
    public SessionRefreshInterceptor(UserRepository users) { this.users = users; }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (request.getMethod().equals("OPTIONS")) return true;
        var session = request.getSession(false);
        if (session != null && session.getAttribute("userId") instanceof Long userId) {
            var user = users.findById(userId).orElse(null);
            if (user == null) {
                session.invalidate();
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account no longer exists. Please login again.");
            }
            Object savedVersion = session.getAttribute("credentialVersion");
            long version = savedVersion instanceof Number number ? number.longValue() : 0;
            if (version != user.getCredentialVersion()) {
                session.invalidate();
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Password changed. Please sign in again.");
            }
            session.setAttribute("role", user.getRole());
            session.setAttribute("email", user.getEmail());
        }
        String path = request.getRequestURI();
        boolean accountManagement = path.startsWith("/api/admin/users");
        boolean operations = path.startsWith("/api/inventory") || path.startsWith("/api/orders/status/")
                || (path.startsWith("/api/orders/") && path.endsWith("/status"));
        if (accountManagement || operations) {
            if (session == null || session.getAttribute("userId") == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
            }
            Object role = session.getAttribute("role");
            if (accountManagement ? role != com.hardwarestore.hardwarestore.model.Role.ADMIN
                    : role != com.hardwarestore.hardwarestore.model.Role.ADMIN && role != com.hardwarestore.hardwarestore.model.Role.STAFF) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission for this operation.");
            }
        }
        return true;
    }
}
