package com.devrenanrodrigues.goalmanagementapi.auth.dto;

public record GoogleUserInfo(
    String sub,
    String email,
    String name,
    String picture
) {}

