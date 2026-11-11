package bg.office.coffee.web.dto;

import java.util.List;

/**
 * Ред от общата дъска – само име, брой кафета и баджове (магнитчетата до името), без суми.
 * badges са кодовете от {@link bg.office.coffee.domain.Badge}, от най-малкия към най-големия.
 */
public record BoardEntryDto(Long id, String displayName, int balance, boolean unlimited, List<String> badges) {
}
