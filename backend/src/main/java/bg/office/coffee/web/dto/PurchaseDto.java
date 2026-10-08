package bg.office.coffee.web.dto;

import bg.office.coffee.domain.Purchase;

import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseDto(Long id, Long userId, String userName, String packageName, int quantity, int coffeeCount,
                          BigDecimal amount, Instant createdAt, String createdBy) {
    public static PurchaseDto from(Purchase p) {
        String by = p.getCreatedBy() == null ? null : p.getCreatedBy().getDisplayName();
        return new PurchaseDto(p.getId(), p.getUser().getId(), p.getUser().getDisplayName(), p.getPackageName(),
                p.getQuantity(), p.getCoffeeCount(), p.getAmount(), p.getCreatedAt(), by);
    }
}
