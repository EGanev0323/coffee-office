package bg.office.coffee.web.dto;

/** Линия от дъската. points са [x, y], нормализирани спрямо ширината на дъската. */
public record StrokeDto(Long id, Long userId, String color, double[][] points) {
}