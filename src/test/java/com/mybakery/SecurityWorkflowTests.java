package com.mybakery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "APP_ADMIN_USERNAME=testadmin",
        "APP_ADMIN_PASSWORD=a-long-test-password",
        "spring.datasource.url=jdbc:h2:mem:mybakery-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class SecurityWorkflowTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicProductReadIsAvailableButWriteRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/products")).andExpect(status().isOk());
        mockMvc.perform(post("/api/products")
                        .contentType("application/json")
                        .content("{\"name\":\"Cake\",\"price\":10,\"category\":\"Cake\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountSettingsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/auth/account-settings")).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminProductsPageRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/products")).andExpect(status().is3xxRedirection());
    }

    @Test
    void adminRootRedirectsToProductsAfterAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void successfulLoginAlwaysRedirectsToAdminProducts() throws Exception {
        mockMvc.perform(formLogin("/auth/login")
                        .user("testadmin")
                        .password("a-long-test-password"))
                .andExpect(redirectedUrl("/admin/products"));
    }
}
