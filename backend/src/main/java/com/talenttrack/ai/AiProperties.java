package com.talenttrack.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String baseUrl, int timeoutSeconds) {
}
