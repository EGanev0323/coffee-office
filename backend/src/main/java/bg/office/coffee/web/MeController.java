package bg.office.coffee.web;

import bg.office.coffee.security.AuthUser;
import bg.office.coffee.service.CoffeeService;
import bg.office.coffee.service.PackageService;
import bg.office.coffee.service.UserService;
import bg.office.coffee.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Действия на влезлия колега върху собствените му кафета. */
@RestController
@RequestMapping("/api")
public class MeController {

    private final CoffeeService coffeeService;
    private final PackageService packageService;
    private final UserService userService;

    public MeController(CoffeeService coffeeService, PackageService packageService, UserService userService) {
        this.coffeeService = coffeeService;
        this.packageService = packageService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public MeDto me(@AuthenticationPrincipal AuthUser me) {
        return coffeeService.me(me.id());
    }

    /** Общата дъска – вижда се от всички влезли колеги, както старата дъска в офиса. */
    @GetMapping("/board")
    public List<BoardEntryDto> board() {
        return coffeeService.board();
    }

    @GetMapping("/packages")
    public List<PackageDto> packages() {
        return packageService.listActive();
    }

    @PostMapping("/me/purchases")
    public MeDto buy(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody BuyRequest req) {
        return coffeeService.buy(me.id(), req.packageId(), req.quantityOrOne(), me.id());
    }

    @PostMapping("/me/consumptions")
    public MeDto drink(@AuthenticationPrincipal AuthUser me) {
        return coffeeService.drink(me.id());
    }

    @DeleteMapping("/me/consumptions/last")
    public MeDto undo(@AuthenticationPrincipal AuthUser me) {
        return coffeeService.undoLastDrink(me.id());
    }

    @GetMapping("/me/activity")
    public List<ActivityDto> activity(@AuthenticationPrincipal AuthUser me) {
        return coffeeService.activity(me.id());
    }

    /** Връща нов токен за това устройство – всички останали устройства се отписват. */
    @PostMapping("/me/password")
    public LoginResponse changePassword(@AuthenticationPrincipal AuthUser me,
                                        @Valid @RequestBody ChangePasswordRequest req) {
        return userService.changeOwnPassword(me.id(), req, me.remember());
    }
}
