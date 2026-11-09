package com.examprep.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examprep.support.AbstractIntegrationTest;
import com.examprep.user.domain.UserAccount;
import com.examprep.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registrationStoresABcryptHash() throws Exception {
        String email = uniqueEmail();
        register(email);

        UserAccount account = userAccountRepository.findByEmail(email).orElseThrow();
        assertNotEquals("Password123", account.getPasswordHash());
        assertFalse(account.getPasswordHash().contains("Password123"));
        assertTrue(account.getPasswordHash().startsWith("$2"));
        assertTrue(passwordEncoder.matches("Password123", account.getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Another Student")
                        .param("email", email)
                        .param("password", "Password123"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "An account already exists for this email address")));
    }

    @Test
    void shortPasswordIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Test Student")
                        .param("email", uniqueEmail())
                        .param("password", "short"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Your password must be between 8 and 72 characters")));
    }

    @Test
    void wrongPasswordDoesNotSignIn() throws Exception {
        String email = uniqueEmail();
        register(email);

        mockMvc.perform(post("/login").with(csrf())
                        .param("email", email)
                        .param("password", "WrongPassword999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void protectedPagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/documents"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/study/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void signInRendersDashboardAndLogoutEndsTheSession() throws Exception {
        MockHttpSession session = registerAndSignIn(uniqueEmail());

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Your workspace")));

        mockMvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void landingAndAuthPagesRender() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Turn your course material into an exam-ready workspace")));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Register")));
    }

    @Test
    void staticAssetsAreServed() throws Exception {
        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.valueOf("text/css")));

        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk());
    }
}
