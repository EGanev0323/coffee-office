package bg.office.coffee.web;

import bg.office.coffee.security.AuthUser;
import bg.office.coffee.service.CoffeeService;
import bg.office.coffee.service.PackageService;
import bg.office.coffee.service.StatsService;
import bg.office.coffee.service.UserService;
import bg.office.coffee.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Всичко под /api/admin е достъпно само за роля ADMIN (виж SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final StatsService statsService;
    private final UserService userService;
    private final PackageService packageService;
    private final CoffeeService coffeeService;

    public AdminController(StatsService statsService, UserService userService,
                           PackageService packageService, CoffeeService coffeeService) {
        this.statsService = statsService;
        this.userService = userService;
        this.packageService = packageService;
        this.coffeeService = coffeeService;
    }

    // ----- Отчет -----

    @GetMapping("/stats")
    public StatsDto stats(@RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month) {
        return statsService.monthStats(statsService.resolveMonth(year, month));
    }

    @GetMapping("/purchases")
    public List<PurchaseDto> purchases(@RequestParam(required = false) Integer year,
                                       @RequestParam(required = false) Integer month) {
        return statsService.monthPurchases(statsService.resolveMonth(year, month));
    }

    @DeleteMapping("/purchases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePurchase(@PathVariable Long id) {
        coffeeService.deletePurchase(id);
    }

    // ----- Колеги -----

    @GetMapping("/users")
    public List<UserDto> users() {
        return userService.list();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest req) {
        return userService.create(req);
    }

    @PutMapping("/users/{id}")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest req,
                              @AuthenticationPrincipal AuthUser me) {
        return userService.update(id, req, me.id());
    }

    @PostMapping("/users/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.resetPassword(id, req);
    }

    /** Когато колега е дал парите директно на админа. */
    @PostMapping("/users/{id}/purchases")
    public MeDto recordPurchase(@PathVariable Long id, @Valid @RequestBody BuyRequest req,
                                @AuthenticationPrincipal AuthUser me) {
        return coffeeService.buy(id, req.packageId(), req.quantityOrOne(), me.id());
    }

    // ----- Пакети -----

    @GetMapping("/packages")
    public List<PackageDto> packages() {
        return packageService.listAll();
    }

    @PostMapping("/packages")
    @ResponseStatus(HttpStatus.CREATED)
    public PackageDto createPackage(@Valid @RequestBody PackageRequest req) {
        return packageService.create(req);
    }

    @PutMapping("/packages/{id}")
    public PackageDto updatePackage(@PathVariable Long id, @Valid @RequestBody PackageRequest req) {
        return packageService.update(id, req);
    }
}
