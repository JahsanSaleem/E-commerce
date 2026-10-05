package com.hardwarestore.hardwarestore.config;
import com.hardwarestore.hardwarestore.service.GoogleAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
public class GoogleLoginConfig {
    @Bean
    @ConditionalOnProperty(name = "store.google.enabled", havingValue = "true")
    ClientRegistrationRepository googleRegistration(
            @Value("${store.google.client-id}") String id,
            @Value("${store.google.client-secret}") String secret,
            @Value("${store.google.redirect-uri:http://localhost:8081/login/oauth2/code/google}") String redirect) {
        if (id.isBlank() || secret.isBlank()) throw new IllegalArgumentException("Google client credentials are required");
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(id).clientSecret(secret).redirectUri(redirect).scope("openid", "profile", "email").build());
    }
    @Bean
    AuthenticationSuccessHandler googleSuccess(GoogleAccountService accounts,
            @Value("${store.google.frontend-url:http://localhost:5173}") String frontend) {
        return (request, response, authentication) -> {
            try {
                OidcUser identity = (OidcUser) authentication.getPrincipal();
                var user = accounts.signIn(identity.getSubject(), identity.getEmail(),
                        Boolean.TRUE.equals(identity.getEmailVerified()), identity.getFullName());
                var session = request.getSession();
                request.changeSessionId();
                session.setAttribute("userId", user.getId());
                session.setAttribute("email", user.getEmail());
                session.setAttribute("role", user.getRole());
        session.setAttribute("credentialVersion", user.getCredentialVersion());
                response.sendRedirect(frontend + "/login");
            } catch (RuntimeException failure) {
                String code = "existing-account".equals(failure.getMessage()) ? "existing-account" : "failed";
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.sendRedirect(frontend + "/login?google=" + code);
            }
        };
    }
    @Bean
    AuthenticationFailureHandler googleFailure(
            @Value("${store.google.frontend-url:http://localhost:5173}") String frontend) {
        return (request, response, failure) -> {
            SecurityContextHolder.clearContext();
            var session = request.getSession(false);
            if (session != null) session.invalidate();
            response.sendRedirect(frontend + "/login?google=failed");
        };
    }
    @Bean
    SecurityFilterChain googleSecurity(HttpSecurity http,
            @Value("${store.google.enabled:false}") boolean enabled,
            AuthenticationSuccessHandler googleSuccess, AuthenticationFailureHandler googleFailure) throws Exception {
        // Store /api/** permissions remain in existing session interceptors/controllers.
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .requestCache(cache -> cache.requestCache(new NullRequestCache()));
        if (enabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .oauth2Login(login -> login.successHandler(googleSuccess).failureHandler(googleFailure));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().denyAll())
                    .exceptionHandling(errors -> errors.authenticationEntryPoint((req, res, ex) -> res.sendError(404)));
        }
        return http.build();
    }
}
