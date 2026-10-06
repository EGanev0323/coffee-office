package bg.office.coffee.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ред от историята на колегата.
 * type: PURCHASE (добавени кафета) или CONSUMPTION (изпито кафе).
 */
public record ActivityDto(String type, Long id, Instant createdAt, int coffees, BigDecimal amount, String label) {
}
