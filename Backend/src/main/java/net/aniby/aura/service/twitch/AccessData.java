package net.aniby.aura.service.twitch;

public record AccessData(String accessToken, String refreshToken, long expiresIn) {}