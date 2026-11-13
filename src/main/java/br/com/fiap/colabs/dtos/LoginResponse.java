package br.com.fiap.colabs.dtos;

public record LoginResponse(
        String token,
        String type,
        String email,
        String role,
        Long citizenId,
        Long collectionPointId
) {
    public LoginResponse(String token, String email, String role, Long citizenId, Long collectionPointId) {
        this(token, "Bearer", email, role, citizenId, collectionPointId);
    }
}