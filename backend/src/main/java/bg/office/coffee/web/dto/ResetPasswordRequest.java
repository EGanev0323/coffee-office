package bg.office.coffee.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Въведи нова парола.") @Size(min = 6, max = 100, message = "Паролата трябва да е поне 6 символа.") String password) {
}
