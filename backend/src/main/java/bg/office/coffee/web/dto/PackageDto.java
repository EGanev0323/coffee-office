package bg.office.coffee.web.dto;

import bg.office.coffee.domain.CoffeePackage;

import java.math.BigDecimal;

public record PackageDto(Long id, String name, int coffeeCount, BigDecimal price, boolean active, int sortOrder) {
    public static PackageDto from(CoffeePackage p) {
        return new PackageDto(p.getId(), p.getName(), p.getCoffeeCount(), p.getPrice(), p.isActive(), p.getSortOrder());
    }
}
