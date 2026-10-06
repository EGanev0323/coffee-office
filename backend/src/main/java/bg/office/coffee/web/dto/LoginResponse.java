package bg.office.coffee.web.dto;

public record LoginResponse(String token, MeDto user) {
}
