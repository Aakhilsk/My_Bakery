package com.mybakery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "APP_ADMIN_USERNAME=testadmin",
        "APP_ADMIN_PASSWORD=a-long-test-password",
        "spring.datasource.url=jdbc:h2:mem:mybakery-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PasswordEncodingTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void legacyPlainTextPasswordsRemainValidForAuthentication() {
        assertTrue(passwordEncoder.matches("admin123", "admin123"));
    }
}
