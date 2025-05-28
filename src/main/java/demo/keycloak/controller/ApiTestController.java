package demo.keycloak.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiTestController {

    @GetMapping("/api/guest")
    @PreAuthorize("hasRole('guest')")
    public String guestApi() {
        return "You have GUEST role, you can access /api/guest";
    }

    @GetMapping("/api/user")
    @PreAuthorize("hasRole('user')")
    public String userApi() {
        return "You have USER role, you can access /api/user";
    }

    @GetMapping("/api/admin")
    @PreAuthorize("hasRole('admin')")
    public String adminApi() {
        return "You have ADMIN role, you can access /api/admin";
    }
} 