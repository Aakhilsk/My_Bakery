package com.mybakery.config;

import com.mybakery.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Prevents an MFA-enabled admin from accessing protected resources until their TOTP code is verified. */
@Component
public class MfaEnforcementFilter extends OncePerRequestFilter {
    public static final String MFA_VERIFIED_SESSION_KEY = "MFA_VERIFIED";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean protectedPath = path.startsWith("/admin/")
                || (path.startsWith("/api/products") && !"GET".equalsIgnoreCase(request.getMethod()))
                || path.equals("/auth/account-settings")
                || path.equals("/auth/change-password")
                || path.equals("/auth/change-username")
                || path.equals("/auth/mfa/setup")
                || path.equals("/auth/mfa/enable")
                || path.equals("/auth/mfa/disable");
        if (protectedPath && authentication != null && authentication.getPrincipal() instanceof User user
                && Boolean.TRUE.equals(user.getMfaEnabled())
                && !Boolean.TRUE.equals(request.getSession(false) == null ? null :
                request.getSession(false).getAttribute(MFA_VERIFIED_SESSION_KEY))) {
            response.sendRedirect("/auth/mfa/verify");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
