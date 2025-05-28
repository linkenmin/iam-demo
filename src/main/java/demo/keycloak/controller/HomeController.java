package demo.keycloak.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Map;
import java.util.List;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal != null) {
            model.addAttribute("username", principal.getAttribute("preferred_username"));
            
            // Extract roles from resource_access
            Map<String, Object> attributes = principal.getAttributes();
            if (attributes.containsKey("resource_access")) {
                Map<String, Object> resourceAccess = (Map<String, Object>) attributes.get("resource_access");
                if (resourceAccess.containsKey("groupware-backend")) {
                    Map<String, Object> client = (Map<String, Object>) resourceAccess.get("groupware-backend");
                    if (client.containsKey("roles")) {
                        List<String> roles = (List<String>) client.get("roles");
                        model.addAttribute("userRoles", roles);
                    }
                }
            }
            
            // User Info
            StringBuilder userInfo = new StringBuilder();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                if ("resource_access".equals(entry.getKey())) continue;
                userInfo.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
                userInfo.append("resource_access: ").append(attributes.get("resource_access")).append("\n");
            model.addAttribute("userInfo", userInfo.toString());
        }
        return "home";
    }
} 