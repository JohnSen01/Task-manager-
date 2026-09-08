package com.tool.management.controller;

import com.tool.management.config.DataSourceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final String appName;
    private final DataSourceConfig dataSourceConfig;

    public HealthController(
            @Value("${app.name:task-manager}") String appName,
            DataSourceConfig dataSourceConfig) {
        this.appName = appName;
        this.dataSourceConfig = dataSourceConfig;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("app", appName);
        body.put("message", "Task Management System");
        body.put("health", "/health");
        body.put("tasks", "/tasks");
        return body;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("app", appName);
        body.put("database", dataSourceConfig.getActiveDbType().name());
        return body;
    }
}
