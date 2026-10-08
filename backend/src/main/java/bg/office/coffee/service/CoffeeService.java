package bg.office.coffee.service;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.CoffeePackage;
import bg.office.coffee.domain.Consumption;
import bg.office.coffee.domain.Purchase;
import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.repo.CoffeePackageRepository;
import bg.office.coffee.repo.ConsumptionRepository;
import bg.office.coffee.repo.PurchaseRepository;
import bg.office.coffee.web.dto.ActivityDto;
import bg.office.coffee.web.dto.BoardEntryDto;
import bg.office.coffee.web.dto.BuyRequest;
import bg.office.coffee.web.dto.MeDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** Покупки на пакети, изпити кафета и история на колегата. */
@Service
public class CoffeeService {

    /** Колко време след „Изпих кафе“ може да се отмени. */
    public static final Duration UNDO_WINDOW = Duration.ofMinutes(10);

    private final AppUserRepository users;
    private final CoffeePackageRepository packages;
    private final PurchaseRepository purchases;
    private final ConsumptionRepository consumptions;

    public CoffeeService(AppUserRepository users, CoffeePackageRepository packages,
                         PurchaseRepository purchases, ConsumptionRepository consumptions) {
        this.users = users;
        this.packages = packages;
        this.purchases = purchases;
        this.consumptions = consumptions;
    }

    @Transactional(readOnly = true)
    public MeDto me(Long userId) {
        return MeDto.from(loadUser(userId));
    }

    /**
     * Записва авансова покупка на пакет (quantity пъти наведнъж) и добавя кафетата към баланса.
     * actorId е този, който въвежда покупката (самият колега или админ).
     */
    @Transactional
    public MeDto buy(Long userId, Long packageId, int quantity, Long actorId) {
        if (quantity < 1 || quantity > BuyRequest.MAX_QUANTITY) {
            throw new BusinessException("Невалиден брой пакети.");
        }
        CoffeePackage pkg = packages.findById(packageId)
                .filter(CoffeePackage::isActive)
                .orElseThrow(() -> new NotFoundException("Този пакет вече не се предлага."));
        AppUser user = loadUser(userId);
        if (!user.isActive()) {
            throw new BusinessException("Профилът на колегата е деактивиран.");
        }
        if (user.isUnlimited()) {
            throw new BusinessException("Колегата има безкрайни кафета, не е нужно да купува пакети.");
        }

        Purchase purchase = new Purchase();
        purchase.setUser(user);
        purchase.setPackageName(pkg.getName());
        purchase.setQuantity(quantity);
        purchase.setCoffeeCount(pkg.getCoffeeCount() * quantity);
        purchase.setAmount(pkg.getPrice().multiply(BigDecimal.valueOf(quantity)));
        purchase.setCreatedBy(users.getReferenceById(actorId));
        purchases.save(purchase);

        users.increaseBalance(userId, pkg.getCoffeeCount() * quantity);
        return MeDto.from(loadUser(userId));
    }

    /**
     * Отбелязва изпито кафе. При обикновен колега намалява баланса с 1 (грешка, ако няма кафета),
     * при колега с безкрайни кафета само записва кафето за статистиката.
     */
    @Transactional
    public MeDto drink(Long userId) {
        boolean unlimited = loadUser(userId).isUnlimited();
        if (!unlimited && users.decreaseBalance(userId, 1) == 0) {
            throw new BusinessException("Нямаш налични кафета. Купи пакет, за да продължиш.");
        }
        Consumption consumption = new Consumption();
        consumption.setUser(users.getReferenceById(userId));
        consumptions.save(consumption);
        return MeDto.from(loadUser(userId));
    }

    /** Отменя последното отбелязано кафе, ако е в рамките на UNDO_WINDOW. */
    @Transactional
    public MeDto undoLastDrink(Long userId) {
        Consumption last = consumptions.findFirstByUser_IdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException("Няма изпито кафе за отмяна."));
        if (last.getCreatedAt().isBefore(Instant.now().minus(UNDO_WINDOW))) {
            throw new BusinessException("Кафе може да се отмени до " + UNDO_WINDOW.toMinutes()
                    + " минути след отбелязването. За по-стари корекции се обърни към администратора.");
        }
        boolean unlimited = loadUser(userId).isUnlimited();
        consumptions.delete(last);
        if (!unlimited) {
            users.increaseBalance(userId, 1);
        }
        return MeDto.from(loadUser(userId));
    }

    /** Изтрива погрешна покупка и връща кафетата обратно (само ако още не са изпити). */
    @Transactional
    public void deletePurchase(Long purchaseId) {
        Purchase purchase = purchases.findById(purchaseId)
                .orElseThrow(() -> new NotFoundException("Покупката не е намерена."));
        Long userId = purchase.getUser().getId();
        int coffees = purchase.getCoffeeCount();
        purchases.delete(purchase);
        if (users.decreaseBalance(userId, coffees) == 0) {
            // Транзакцията се връща назад, покупката остава.
            throw new BusinessException("Покупката не може да се изтрие: колегата вече е изпил част от тези кафета.");
        }
    }

    @Transactional(readOnly = true)
    public List<ActivityDto> activity(Long userId) {
        Stream<ActivityDto> bought = purchases.findTop30ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(p -> new ActivityDto("PURCHASE", p.getId(), p.getCreatedAt(), p.getCoffeeCount(),
                        p.getAmount(), p.getPackageName(), p.getQuantity()));
        Stream<ActivityDto> drunk = consumptions.findTop30ByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(c -> new ActivityDto("CONSUMPTION", c.getId(), c.getCreatedAt(), 1, null, "Изпито кафе", 1));
        return Stream.concat(bought, drunk)
                .sorted(Comparator.comparing(ActivityDto::createdAt).reversed())
                .limit(30)
                .toList();
    }

    /** Общата дъска: кой колко кафета има в момента. */
    @Transactional(readOnly = true)
    public List<BoardEntryDto> board() {
        return users.findBoard().stream()
                .map(u -> new BoardEntryDto(u.getId(), u.getDisplayName(), u.getBalance(), u.isUnlimited()))
                .toList();
    }

    private AppUser loadUser(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Потребителят не е намерен."));
    }
}
