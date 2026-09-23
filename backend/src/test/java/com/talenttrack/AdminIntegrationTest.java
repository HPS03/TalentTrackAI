package com.talenttrack;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTestSupport {

    private String adminToken() throws Exception {
        return body(post("/api/auth/login", null, Map.of("email", "admin@talenttrack.ai", "password", "Admin@123"))
                .andExpect(status().isOk())).get("accessToken").asText();
    }

    @Test
    void adminSeesStatsAndCanDisableUsers() throws Exception {
        String admin = adminToken();
        var reg = register("Soon Disabled", uniqueEmail("dis"), "CANDIDATE", null);
        long userId = reg.get("user").get("id").asLong();
        String userToken = reg.get("accessToken").asText();

        get("/api/admin/dashboard", admin).andExpect(status().isOk()).andExpect(jsonPath("$.admins").value(1));
        get("/api/admin/users?role=CANDIDATE&q=Soon", admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Soon Disabled"));

        patch("/api/admin/users/" + userId + "/status", admin, Map.of("enabled", false))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));

        // existing access token stops working immediately, and login is refused
        get("/api/auth/me", userToken).andExpect(status().isUnauthorized());
        post("/api/auth/login", null, Map.of("email", reg.get("user").get("email").asText(), "password", "Password1"))
                .andExpect(status().isForbidden());
    }
}
