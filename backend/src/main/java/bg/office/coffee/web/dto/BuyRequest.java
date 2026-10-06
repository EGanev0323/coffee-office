package bg.office.coffee.web.dto;

import jakarta.validation.constraints.NotNull;

public record BuyRequest(@NotNull(message = "Избери пакет.") Long packageId) {
}
