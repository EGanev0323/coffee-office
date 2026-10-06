package bg.office.coffee.web.dto;

import bg.office.coffee.domain.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank(message = "Въведи потребителско име.")
        @Size(min = 3, max = 50, message = "Потребителското име трябва да е между 3 и 50 символа.")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "Потребителското име може да съдържа само латински букви, цифри, точка, тире и долна черта.")
        String username,
        @NotBlank(message = "Въведи име на колегата.") @Size(max = 100, message = "Името е твърде дълго.") String displayName,
        @NotBlank(message = "Въведи начална парола.") @Size(min = 6, max = 100, message = "Паролата трябва да е поне 6 символа.") String password,
        @NotNull(message = "Избери роля.") Role role) {
}
