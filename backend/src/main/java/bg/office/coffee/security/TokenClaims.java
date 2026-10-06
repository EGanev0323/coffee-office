package bg.office.coffee.security;

/** Данните, които ни трябват от валиден JWT. */
public record TokenClaims(Long userId, int tokenVersion, boolean remember) {
}