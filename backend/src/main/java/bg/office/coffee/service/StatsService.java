package bg.office.coffee.service;

import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.repo.ConsumptionRepository;
import bg.office.coffee.repo.PurchaseRepository;
import bg.office.coffee.web.dto.PurchaseDto;
import bg.office.coffee.web.dto.StatsDto;
import bg.office.coffee.web.dto.UserStatsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Месечни отчети за админа. Границите на месеца се смятат в часовата зона на офиса. */
@Service
public class StatsService {

    private final AppUserRepository users;
    private final PurchaseRepository purchases;
    private final ConsumptionRepository consumptions;
    private final ZoneId zone;

    public StatsService(AppUserRepository users, PurchaseRepository purchases, ConsumptionRepository consumptions,
                        @Value("${app.timezone}") String timezone) {
        this.users = users;
        this.purchases = purchases;
        this.consumptions = consumptions;
        this.zone = ZoneId.of(timezone);
    }

    public YearMonth resolveMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            return YearMonth.now(zone);
        }
        if (month < 1 || month > 12 || year < 2000 || year > 2100) {
            throw new BusinessException("Невалиден месец.");
        }
        return YearMonth.of(year, month);
    }

    @Transactional(readOnly = true)
    public StatsDto monthStats(YearMonth ym) {
        Instant from = start(ym);
        Instant to = start(ym.plusMonths(1));

        Map<Long, Object[]> bought = purchases.totalsPerUser(from, to).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> r));
        Map<Long, Long> drunk = consumptions.countsPerUser(from, to).stream()
                .collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));

        List<UserStatsDto> perUser = users.findAllByOrderByDisplayNameAsc().stream()
                // Деактивираните колеги се показват само ако имат баланс или движение през месеца.
                .filter(u -> u.isActive() || u.getBalance() > 0
                        || bought.containsKey(u.getId()) || drunk.containsKey(u.getId()))
                .map(u -> {
                    Object[] b = bought.get(u.getId());
                    BigDecimal spent = b == null ? BigDecimal.ZERO : (BigDecimal) b[1];
                    long boughtCount = b == null ? 0 : ((Number) b[2]).longValue();
                    return new UserStatsDto(u.getId(), u.getDisplayName(), u.getUsername(), u.getBalance(),
                            u.isUnlimited(), u.isActive(), spent, boughtCount, drunk.getOrDefault(u.getId(), 0L));
                })
                .toList();

        return new StatsDto(
                ym.getYear(), ym.getMonthValue(),
                orZero(purchases.sumAmount(from, to)),
                purchases.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to),
                orZero(purchases.sumCoffees(from, to)),
                consumptions.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, to),
                orZero(users.totalBalance()),
                perUser);
    }

    @Transactional(readOnly = true)
    public List<PurchaseDto> monthPurchases(YearMonth ym) {
        return purchases.findInRange(start(ym), start(ym.plusMonths(1))).stream()
                .map(PurchaseDto::from)
                .toList();
    }

    private static BigDecimal orZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static long orZero(Long v) {
        return v == null ? 0L : v;
    }

    private Instant start(YearMonth ym) {
        return ym.atDay(1).atStartOfDay(zone).toInstant();
    }
}
