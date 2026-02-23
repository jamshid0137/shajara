package com.example.shajara.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * Statik HTML/CSS/JS fayllarni Spring Security dan istisno qiladi.
 * SecurityConfig.java ga tegmasdan ishlaydi.
 */
@Configuration
public class StaticResourceConfig {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(
                        "/tree.html",
                        "/style.css",
                        "/app.js",
                        "/app2.js",
                        "/index.html");
    }
}
