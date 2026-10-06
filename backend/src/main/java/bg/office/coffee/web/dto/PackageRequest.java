package bg.office.coffee.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PackageRequest(
        @NotBlank(message = "Въведи име на пакета.") @Size(max = 100, message = "Името е твърде дълго.") String name,
        @Min(value = 1, message = "Пакетът трябва да съдържа поне 1 кафе.")
        @Max(value = 1000, message = "Пакетът може да съдържа най-много 1000 кафета.") int coffeeCount,
        @NotNull(message = "Въведи цена.")
        @DecimalMin(value = "0.00", message = "Цената не може да е отрицателна.")
        @Digits(integer = 8, fraction = 2, message = "Цената може да има най-много 2 знака след десетичната точка.")
        BigDecimal price,
        boolean active,
        int sortOrder) {
}
