package com.gamemoyeo.auth.adapter.in.bootstrap;

import com.gamemoyeo.auth.application.AdminAuthenticationService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.admin.bootstrap.enabled", havingValue = "true")
public class LocalAdminBootstrapRunner implements ApplicationRunner {

    private final AdminAuthenticationService service;
    private final String username;
    private final String password;

    public LocalAdminBootstrapRunner(
        AdminAuthenticationService service,
        @Value("${app.admin.bootstrap.username}") String username,
        @Value("${app.admin.bootstrap.password}") String password
    ) {
        this.service = service;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        service.provisionLocalAdmin(username, password);
    }
}
