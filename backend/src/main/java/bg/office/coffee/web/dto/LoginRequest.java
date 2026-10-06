package bg.office.coffee.web.dto;

import jakarta.validation.constraints.NotBlank;

/** remember=true („Запомни ме“) издава дълготраен токен. */
public record LoginRequest(
        @NotBlank(message = "Въведи потребителско име.") String username,
        @NotBlank(message = "Въведи парола.") String password,
        boolean remember) {
}