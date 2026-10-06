package bg.office.coffee.web.dto;

/** Ред от общата дъска – само име и брой кафета, без суми. */
public record BoardEntryDto(Long id, String displayName, int balance, boolean unlimited) {
}
