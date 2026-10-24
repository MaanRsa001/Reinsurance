package com.rms.config;

import com.rms.common.CurrentUser;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    /** Used by the Angular login screen to verify credentials and learn the user's authorities. */
    @GetMapping("/me")
    public Map<String, Object> me() {
        return Map.of("username", CurrentUser.username(), "authorities", new TreeSet<>(CurrentUser.authorities()));
    }
}
