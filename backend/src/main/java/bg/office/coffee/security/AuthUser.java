package bg.office.coffee.security;

import bg.office.coffee.domain.Role;

/**
 * Влезлият потребител, достъпен в контролерите чрез @AuthenticationPrincipal.
 * remember показва дали сесията е „Запомни ме“ – нужно е при издаване на нов токен.
 */
public record AuthUser(Long id, String username, Role role, boolean remember) {
}