package com.graphify.admin.dto;

public record AdminCreateUserRequest(
        String email,
        String displayName,
        String password,
        String role
) {}
