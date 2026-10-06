package bg.office.coffee.web.dto;

import java.math.BigDecimal;

/** Колега в месечния отчет: текущ баланс + движение за избрания месец. */
public record UserStatsDto(Long id, String displayName, String username, int balance, boolean unlimited, boolean active,
                           BigDecimal spent, long bought, long consumed) {
}
