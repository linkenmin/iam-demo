package demo.keycloak.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import java.util.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    private Collection<GrantedAuthority> extractRolesFromResourceAccess(Map<String, Object> resourceAccess) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (resourceAccess != null && resourceAccess.containsKey("groupware-backend")) {
            Map<String, Object> client = (Map<String, Object>) resourceAccess.get("groupware-backend");
            List<String> roles = (List<String>) client.get("roles");
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
        }
        return authorities;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(issuerUri + "/protocol/openid-connect/certs").build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            // 1. Add SCOPE_xxx authorities
            org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter delegate = new org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter();
            authorities.addAll(delegate.convert(jwt));
            // 2. Parse resource_access
            authorities.addAll(extractRolesFromResourceAccess(jwt.getClaim("resource_access")));
            return authorities;
        });
        return jwtAuthenticationConverter;
    }

    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        OidcClientInitiatedLogoutSuccessHandler handler =
            new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri("{baseUrl}"); // Will automatically use the correct parameter
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
        LogoutSuccessHandler oidcLogoutSuccessHandler) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/public", "/error", "/css/**", "/js/**", "/login**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .requestMatchers("/user").hasRole("user")
                .requestMatchers("/admin").hasRole("admin")
                .anyRequest().authenticated()
            )
            // Only enable Resource Server for /api/**
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((req, res, ex) -> res.sendError(401, "Unauthorized"))
                .accessDeniedHandler((req, res, ex) -> res.sendError(403, "Forbidden"))
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(userRequest -> {
                        OidcUserService delegate = new OidcUserService();
                        OidcUser oidcUser = delegate.loadUser(userRequest);
                        Collection<GrantedAuthority> mapped = extractRolesFromResourceAccess(oidcUser.getAttribute("resource_access"));
                        mapped.addAll(oidcUser.getAuthorities());
                        return new DefaultOidcUser(mapped, oidcUser.getIdToken(), oidcUser.getUserInfo());
                    })
                    .userService(customOAuth2UserService())
                )
                .defaultSuccessUrl("/", true)
                .loginPage("/oauth2/authorization/groupware-backend")
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessHandler(oidcLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return request -> {
            OAuth2User user = delegate.loadUser(request);
            Collection<GrantedAuthority> authorities = extractRolesFromResourceAccess((Map<String, Object>) user.getAttributes().get("resource_access"));
            authorities.addAll(user.getAuthorities());
            return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(
                authorities, user.getAttributes(), "preferred_username");
        };
    }
} 