package com.library.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class Dtos {
    private Dtos() {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record LoginResponse(String token, UserView user) {}

    public record CreateUserRequest(@NotBlank String fullName,
                                    @NotBlank @Email String email,
                                    @NotBlank @Size(min = 6) String password,
                                    @NotNull StaffUser.Role role) {}

    public record UserView(String id, String fullName, String email, StaffUser.Role role, boolean active) {
        static UserView of(StaffUser u) {
            return new UserView(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive());
        }
    }
}
