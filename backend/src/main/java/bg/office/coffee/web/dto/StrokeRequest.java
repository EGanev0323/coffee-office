package bg.office.coffee.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StrokeRequest(
        @NotBlank(message = "Избери маркер.") String color,
        @NotNull(message = "Липсва рисунка.")
        @Size(min = 1, max = 2000, message = "Линията е твърде дълга.") double[][] points) {
}