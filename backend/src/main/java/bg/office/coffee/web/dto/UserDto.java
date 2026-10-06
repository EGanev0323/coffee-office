package bg.office.coffee.web.dto;

import bg.office.coffee.domain.AppUser;

import java.time.Instant;

public record UserDto(Long id, String username, String displayName, String role,
                      int balance, boolean unlimited, boolean active, boolean boardVisible, Instant createdAt) {
    public static UserDto from(AppUser u) {
        return new UserDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(),
                u.getBalance(), u.isUnlimited(), u.isActive(), u.isBoardVisible(), u.getCreatedAt());
    }
}