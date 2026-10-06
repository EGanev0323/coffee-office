package bg.office.coffee.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record StatsDto(int year, int month,
                       BigDecimal totalCollected,
                       long purchasesCount,
                       long coffeesSold,
                       long coffeesConsumed,
                       long outstandingCoffees,
                       List<UserStatsDto> users) {
}
