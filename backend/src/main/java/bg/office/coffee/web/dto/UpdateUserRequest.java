package bg.office.coffee.web.dto;

import bg.office.coffee.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "Въведи име на колегата.") @Size(max = 100, message = "Името е твърде дълго.") String displayName,
        @NotNull(message = "Избери роля.") Role role,
        boolean active,
        boolean unlimited,
        Boolean boardVisible) { // null = без промяна
}