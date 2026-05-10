package com.immunesentinel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "sentinel")
public class SentinelProperties {
    private String baseUrl;
    private String tokenSecret;
    private String adminToken = "please-set-admin-token";
    private int tokenTtlHours = 48;
    private Storage storage = new Storage();

    @Data
    public static class Storage {
        private String localDir;
        private String publicPrefix = "/files";
    }
}
