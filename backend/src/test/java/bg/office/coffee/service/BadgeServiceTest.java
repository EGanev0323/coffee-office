package bg.office.coffee.service;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.Consumption;
import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.repo.ConsumptionRepository;
import bg.office.coffee.web.dto.BadgesDto;
import bg.office.coffee.web.dto.BoardEntryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Баджовете се смятат от изпитите кафета – проверка срещу истинска база. */
@SpringBootTest
@ActiveProfiles("test")
class BadgeServiceTest {

    @Autowired BadgeService badgeService;
    @Autowired CoffeeService coffeeService;
    @Autowired AppUserRepository users;
    @Autowired ConsumptionRepository consumptions;

    private AppUser newUser() {
        AppUser u = new AppUser();
        u.setUsername("b-" + UUID.randomUUID().toString().substring(0, 12));
        u.setDisplayName("Баджове");
        u.setPasswordHash("x");
        u.setUnlimited(true); // безкрайни кафета – балансът не пречи на теста
        return users.save(u);
    }

    private void drinks(AppUser u, int count) {
        for (int i = 0; i < count; i++) {
            Consumption c = new Consumption();
            c.setUser(u);
            consumptions.save(c);
        }
    }

    @Test
    void progressBeforeTheFirstBadge() {
        AppUser u = newUser();
        drinks(u, 12);

        BadgesDto dto = badgeService.forUser(u.getId());

        assertThat(dto.coffees()).isEqualTo(12);
        assertThat(dto.next()).isEqualTo("CUPS_50");
        assertThat(dto.remaining()).isEqualTo(38);
        assertThat(dto.progressFrom()).isZero();
        assertThat(dto.badges()).allSatisfy(b -> assertThat(b.earnedAt()).isNull());
    }

    @Test
    void fiftiethCoffeeEarnsTheBadgeAndShowsOnTheBoard() {
        AppUser u = newUser();
        drinks(u, 50);

        BadgesDto dto = badgeService.forUser(u.getId());

        assertThat(dto.badges().get(0).code()).isEqualTo("CUPS_50");
        assertThat(dto.badges().get(0).earnedAt()).isNotNull();
        assertThat(dto.badges().get(1).earnedAt()).isNull();
        assertThat(dto.next()).isEqualTo("CUPS_100");
        assertThat(dto.remaining()).isEqualTo(50);
        assertThat(dto.progressFrom()).isEqualTo(50);

        BoardEntryDto row = coffeeService.board().stream()
                .filter(e -> e.id().equals(u.getId())).findFirst().orElseThrow();
        assertThat(row.badges()).containsExactly("CUPS_50");
    }

    @Test
    void undoingTheFiftiethCoffeeTakesTheBadgeBack() {
        AppUser u = newUser();
        drinks(u, 50);

        coffeeService.undoLastDrink(u.getId());

        assertThat(badgeService.forUser(u.getId()).badges().get(0).earnedAt()).isNull();
        assertThat(badgeService.earnedPerUser()).doesNotContainKey(u.getId());
    }
}
