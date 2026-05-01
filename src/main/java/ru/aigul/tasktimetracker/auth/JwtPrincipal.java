package ru.aigul.tasktimetracker.auth;

import ru.aigul.tasktimetracker.entity.Role;

public class JwtPrincipal {
    private final Long id;
    private final String username;
    private final Role role;

    public JwtPrincipal(Long id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}

