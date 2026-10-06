package bg.office.coffee.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Въведи текущата парола.") String currentPassword,
        @NotBlank(message = "Въведи нова парола.") @Size(min = 6, max = 100, message = "Новата парола трябва да е поне 6 символа.") String newPassword) {
}
