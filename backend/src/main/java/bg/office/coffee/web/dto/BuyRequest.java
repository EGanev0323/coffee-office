package bg.office.coffee.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** quantity – колко пъти се купува пакетът наведнъж; липсва = 1. */
public record BuyRequest(
        @NotNull(message = "Избери пакет.") Long packageId,
        @Min(value = 1, message = "Купи поне 1 пакет.")
        @Max(value = BuyRequest.MAX_QUANTITY, message = "Наведнъж може да се купят най-много " + BuyRequest.MAX_QUANTITY + " пакета.")
        Integer quantity) {

    public static final int MAX_QUANTITY = 100;

    public int quantityOrOne() {
        return quantity == null ? 1 : quantity;
    }
}
