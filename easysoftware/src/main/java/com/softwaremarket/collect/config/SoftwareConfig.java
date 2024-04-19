package com.softwaremarket.collect.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "softwareconfig")
public class SoftwareConfig {
    String sotfwareDetailUrl;
    String importEsIndex;
    String mappingPath;
    Integer pagesize;
}
