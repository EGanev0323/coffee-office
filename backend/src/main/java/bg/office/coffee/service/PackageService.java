package bg.office.coffee.service;

import bg.office.coffee.domain.CoffeePackage;
import bg.office.coffee.repo.CoffeePackageRepository;
import bg.office.coffee.web.dto.PackageDto;
import bg.office.coffee.web.dto.PackageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Пакетите не се трият, а се скриват (active=false), за да остане историята на покупките четима.
 */
@Service
public class PackageService {

    private final CoffeePackageRepository packages;

    public PackageService(CoffeePackageRepository packages) {
        this.packages = packages;
    }

    @Transactional(readOnly = true)
    public List<PackageDto> listActive() {
        return packages.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream().map(PackageDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PackageDto> listAll() {
        return packages.findAllByOrderBySortOrderAscIdAsc().stream().map(PackageDto::from).toList();
    }

    @Transactional
    public PackageDto create(PackageRequest req) {
        CoffeePackage p = new CoffeePackage();
        apply(p, req);
        return PackageDto.from(packages.save(p));
    }

    @Transactional
    public PackageDto update(Long id, PackageRequest req) {
        CoffeePackage p = packages.findById(id).orElseThrow(() -> new NotFoundException("Пакетът не е намерен."));
        apply(p, req);
        return PackageDto.from(p);
    }

    private static void apply(CoffeePackage p, PackageRequest req) {
        p.setName(req.name().trim());
        p.setCoffeeCount(req.coffeeCount());
        p.setPrice(req.price());
        p.setActive(req.active());
        p.setSortOrder(req.sortOrder());
    }
}
