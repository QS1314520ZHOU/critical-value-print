package com.digixmed.icu.cvprint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 危急值报告记录登记 - 启动类
 * 启动后访问：http://ip:18088/index.html
 */
@SpringBootApplication
public class CriticalValuePrintApplication {
    public static void main(String[] args) {
        SpringApplication.run(CriticalValuePrintApplication.class, args);
    }
}
