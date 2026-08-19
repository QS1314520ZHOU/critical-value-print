package com.digixmed.icu.cvprint.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置。打印程序以 iframe 嵌入宿主系统，前后端同源时可不开启；
 * 前端单独部署时在 application.yml 中配置 critical-value.allowed-origins。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${critical-value.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
