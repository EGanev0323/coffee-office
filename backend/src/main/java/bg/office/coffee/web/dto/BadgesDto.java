package bg.office.coffee.web.dto;

import bg.office.coffee.domain.Badge;

import java.time.Instant;
import java.util.List;

/**
 * Баджовете на колегата за профила: всички (получени и още не), общият брой изпити кафета
 * и колко остават до следващия.
 *
 * @param coffees     изпити кафета общо
 * @param badges      всички баджове, по реда на праговете
 * @param next        кодът на следващия бадж или null, ако всички са получени
 * @param remaining   колко кафета остават до следващия (0, ако няма следващ)
 * @param progressFrom прагът на последния получен бадж – началото на лентата за прогрес
 */
public record BadgesDto(long coffees, List<Item> badges, String next, long remaining, int progressFrom) {

    /** earnedAt е кога е изпито кафето, с което баджът е получен (null, ако още не е). */
    public record Item(String code, int threshold, String title, Instant earnedAt) {
        public static Item of(Badge badge, Instant earnedAt) {
            return new Item(badge.name(), badge.threshold(), badge.title(), earnedAt);
        }
    }
}
