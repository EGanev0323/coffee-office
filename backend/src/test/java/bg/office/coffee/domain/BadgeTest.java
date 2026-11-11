package bg.office.coffee.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Праговете на баджовете – без база. */
class BadgeTest {

    @Test
    void noBadgesBeforeFifty() {
        assertThat(Badge.earned(49)).isEmpty();
        assertThat(Badge.next(49)).contains(Badge.CUPS_50);
        assertThat(Badge.previousThreshold(49)).isZero();
    }

    @Test
    void badgeIsEarnedExactlyOnTheThreshold() {
        assertThat(Badge.earned(50)).containsExactly(Badge.CUPS_50);
        assertThat(Badge.next(50)).contains(Badge.CUPS_100);
        assertThat(Badge.previousThreshold(50)).isEqualTo(50);
    }

    @Test
    void severalBadgesInOrder() {
        assertThat(Badge.earned(730)).containsExactly(Badge.CUPS_50, Badge.CUPS_100, Badge.CUPS_500);
        assertThat(Badge.next(730)).contains(Badge.CUPS_1000);
        assertThat(Badge.previousThreshold(730)).isEqualTo(500);
    }

    @Test
    void nothingLeftAfterTheLastBadge() {
        assertThat(Badge.earned(1500)).hasSize(Badge.values().length);
        assertThat(Badge.next(1500)).isEmpty();
    }
}
