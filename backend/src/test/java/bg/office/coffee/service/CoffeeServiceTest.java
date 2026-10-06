package bg.office.coffee.service;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.CoffeePackage;
import bg.office.coffee.domain.Purchase;
import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.repo.CoffeePackageRepository;
import bg.office.coffee.repo.ConsumptionRepository;
import bg.office.coffee.repo.PurchaseRepository;
import bg.office.coffee.web.dto.MeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Покупки, изпити кафета и отмяна срещу истинска PostgreSQL база (атомарните заявки за баланса
 * и check (balance >= 0) се проверяват наистина). Без @Transactional на теста – иначе
 * отказаната транзакция на сървиса няма да се върне назад преди проверките.
 */
@SpringBootTest
@ActiveProfiles("test")
class CoffeeServiceTest {

    @Autowired CoffeeService coffeeService;
    @Autowired AppUserRepository users;
    @Autowired CoffeePackageRepository packages;
    @Autowired PurchaseRepository purchases;
    @Autowired ConsumptionRepository consumptions;

    private CoffeePackage pkg;

    @BeforeEach
    void setUp() {
        pkg = new CoffeePackage();
        pkg.setName("Тестови 5");
        pkg.setCoffeeCount(5);
        pkg.setPrice(new BigDecimal("2.00"));
        pkg = packages.save(pkg);
    }

    private AppUser newUser(boolean unlimited) {
        AppUser u = new AppUser();
        u.setUsername("t-" + UUID.randomUUID().toString().substring(0, 12));
        u.setDisplayName("Тест");
        u.setPasswordHash("x");
        u.setUnlimited(unlimited);
        return users.save(u);
    }

    private int balance(AppUser u) {
        return users.findById(u.getId()).orElseThrow().getBalance();
    }

    @Test
    void buyCopiesPackageAndAddsCoffees() {
        AppUser u = newUser(false);

        MeDto me = coffeeService.buy(u.getId(), pkg.getId(), u.getId());

        assertThat(me.balance()).isEqualTo(5);
        Purchase p = purchases.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId()).get(0);
        assertThat(p.getPackageName()).isEqualTo("Тестови 5");
        assertThat(p.getCoffeeCount()).isEqualTo(5);
        assertThat(p.getAmount()).isEqualByComparingTo("2.00");

        // Промяна на пакета не променя историята.
        pkg.setPrice(new BigDecimal("9.99"));
        packages.save(pkg);
        assertThat(purchases.findById(p.getId()).orElseThrow().getAmount()).isEqualByComparingTo("2.00");
    }

    @Test
    void hiddenPackageCannotBeBought() {
        AppUser u = newUser(false);
        pkg.setActive(false);
        packages.save(pkg);

        assertThatThrownBy(() -> coffeeService.buy(u.getId(), pkg.getId(), u.getId()))
                .isInstanceOf(NotFoundException.class);
        assertThat(balance(u)).isZero();
    }

    @Test
    void unlimitedUserCannotBuy() {
        AppUser u = newUser(true);

        assertThatThrownBy(() -> coffeeService.buy(u.getId(), pkg.getId(), u.getId()))
                .isInstanceOf(BusinessException.class);
        assertThat(purchases.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId())).isEmpty();
    }

    @Test
    void drinkDecreasesBalanceAndFailsAtZero() {
        AppUser u = newUser(false);
        coffeeService.buy(u.getId(), pkg.getId(), u.getId());

        for (int i = 4; i >= 0; i--) {
            assertThat(coffeeService.drink(u.getId()).balance()).isEqualTo(i);
        }
        assertThatThrownBy(() -> coffeeService.drink(u.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Нямаш налични кафета");
        assertThat(balance(u)).isZero();
        assertThat(consumptions.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId())).hasSize(5);
    }

    @Test
    void unlimitedDrinkKeepsBalanceButIsRecorded() {
        AppUser u = newUser(true);

        MeDto me = coffeeService.drink(u.getId());

        assertThat(me.balance()).isZero();
        assertThat(me.unlimited()).isTrue();
        assertThat(consumptions.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId())).hasSize(1);
    }

    @Test
    void undoReturnsTheCoffee() {
        AppUser u = newUser(false);
        coffeeService.buy(u.getId(), pkg.getId(), u.getId());
        coffeeService.drink(u.getId());

        MeDto me = coffeeService.undoLastDrink(u.getId());

        assertThat(me.balance()).isEqualTo(5);
        assertThat(consumptions.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId())).isEmpty();
        assertThatThrownBy(() -> coffeeService.undoLastDrink(u.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void deletePurchaseRemovesCoffees() {
        AppUser u = newUser(false);
        coffeeService.buy(u.getId(), pkg.getId(), u.getId());
        Long purchaseId = purchases.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId()).get(0).getId();

        coffeeService.deletePurchase(purchaseId);

        assertThat(balance(u)).isZero();
        assertThat(purchases.findById(purchaseId)).isEmpty();
    }

    @Test
    void deletePurchaseIsRefusedWhenCoffeesWereDrunk() {
        AppUser u = newUser(false);
        coffeeService.buy(u.getId(), pkg.getId(), u.getId());
        coffeeService.drink(u.getId());
        Long purchaseId = purchases.findTop30ByUser_IdOrderByCreatedAtDesc(u.getId()).get(0).getId();

        assertThatThrownBy(() -> coffeeService.deletePurchase(purchaseId))
                .isInstanceOf(BusinessException.class);

        // Транзакцията е върната назад: покупката и балансът са непроменени.
        assertThat(purchases.findById(purchaseId)).isPresent();
        assertThat(balance(u)).isEqualTo(4);
    }
}
