package com.tecsup.config;

import com.tecsup.entity.Role;
import com.tecsup.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
public class RoleInitializer implements CommandLineRunner {
    private final RoleRepository roles;
    public RoleInitializer(RoleRepository roles) { this.roles = roles; }
    @Override
    @Transactional
    public void run(String... args) {
        for (String name : List.of("USER", "ADMIN", "MODERATOR")) {
            if (roles.findByName(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roles.save(role);
            }
        }
    }
}
