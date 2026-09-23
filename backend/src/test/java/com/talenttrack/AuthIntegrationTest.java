package com.talenttrack;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        String email = uniqueEmail("cand");
        var reg = register("Test Candidate", email, "CANDIDATE", null);
        assertThat(reg.get("accessToken").asText()).isNotBlank();
        assertThat(reg.get("user").get("role").asText()).isEqualTo("CANDIDATE");

        var login = body(post("/api/auth/login", null, Map.of("email", email, "password", "Password1"))
                .andExpect(status().isOk()));
        get("/api/auth/me", login.get("accessToken").asText())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        String email = uniqueEmail("cand");
        register("Test", email, "CANDIDATE", null);
        post("/api/auth/login", null, Map.of("email", email, "password", "WrongPass9"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void duplicateEmailAndAdminSelfRegistrationAreRejected() throws Exception {
        String email = uniqueEmail("dup");
        register("First", email, "CANDIDATE", null);
        post("/api/auth/register", null, Map.of("fullName", "Second", "email", email, "password", "Password1",
                "role", "CANDIDATE")).andExpect(status().isConflict());
        post("/api/auth/register", null, Map.of("fullName", "Hacker", "email", uniqueEmail("adm"),
                "password", "Password1", "role", "ADMIN")).andExpect(status().isForbidden());
    }

    @Test
    void weakPasswordFailsValidation() throws Exception {
        post("/api/auth/register", null, Map.of("fullName", "Weak", "email", uniqueEmail("weak"),
                "password", "short", "role", "CANDIDATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void refreshTokensAreRotatedAndCannotBeReused() throws Exception {
        var reg = register("Rotator", uniqueEmail("rot"), "CANDIDATE", null);
        String first = reg.get("refreshToken").asText();

        var refreshed = body(post("/api/auth/refresh", null, Map.of("refreshToken", first)).andExpect(status().isOk()));
        String second = refreshed.get("refreshToken").asText();
        assertThat(second).isNotEqualTo(first);

        post("/api/auth/refresh", null, Map.of("refreshToken", first)).andExpect(status().isUnauthorized());
        // reuse of a revoked token revokes the whole token family
        post("/api/auth/refresh", null, Map.of("refreshToken", second)).andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointsRequireAValidToken() throws Exception {
        get("/api/auth/me", null).andExpect(status().isUnauthorized());
        get("/api/auth/me", "not-a-jwt").andExpect(status().isUnauthorized());
        get("/api/jobs", null).andExpect(status().isOk());
    }
}
