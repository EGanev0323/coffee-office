package bg.office.coffee.web.dto;

import bg.office.coffee.domain.AppUser;

public record MeDto(Long id, String username, String displayName, String role, int balance, boolean unlimited) {
    public static MeDto from(AppUser u) {
        return new MeDto(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole().name(), u.getBalance(),
                u.isUnlimited());
    }
}
