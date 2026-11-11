package bg.office.coffee.service;

import bg.office.coffee.domain.Badge;
import bg.office.coffee.domain.Consumption;
import bg.office.coffee.repo.ConsumptionRepository;
import bg.office.coffee.web.dto.BadgesDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Баджове за изпити кафета – смятат се от историята, без отделна таблица. */
@Service
public class BadgeService {

    private final ConsumptionRepository consumptions;

    public BadgeService(ConsumptionRepository consumptions) {
        this.consumptions = consumptions;
    }

    /** Всички баджове на колегата с датите на получаване и прогреса до следващия. */
    @Transactional(readOnly = true)
    public BadgesDto forUser(Long userId) {
        long coffees = consumptions.countByUser_Id(userId);
        List<BadgesDto.Item> items = Arrays.stream(Badge.values())
                .map(b -> BadgesDto.Item.of(b, coffees >= b.threshold() ? earnedAt(userId, b) : null))
                .toList();
        Badge next = Badge.next(coffees).orElse(null);
        return new BadgesDto(
                coffees,
                items,
                next == null ? null : next.name(),
                next == null ? 0 : next.threshold() - coffees,
                Badge.previousThreshold(coffees));
    }

    /** Кодовете на получените баджове за всеки колега – за магнитчетата на дъската. */
    @Transactional(readOnly = true)
    public Map<Long, List<String>> earnedPerUser() {
        Map<Long, List<String>> result = new HashMap<>();
        for (Object[] row : consumptions.totalsPerUser()) {
            Long userId = (Long) row[0];
            long coffees = ((Number) row[1]).longValue();
            List<String> codes = Badge.earned(coffees).stream().map(Badge::name).toList();
            if (!codes.isEmpty()) {
                result.put(userId, codes);
            }
        }
        return result;
    }

    /** Кога е изпито N-тото кафе (N = прагът на баджа). */
    private Instant earnedAt(Long userId, Badge badge) {
        List<Consumption> nth = consumptions.findByUser_IdOrderByCreatedAtAscIdAsc(
                userId, PageRequest.of(badge.threshold() - 1, 1));
        return nth.isEmpty() ? null : nth.get(0).getCreatedAt();
    }
}
