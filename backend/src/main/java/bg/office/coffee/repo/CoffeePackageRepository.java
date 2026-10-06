package bg.office.coffee.repo;

import bg.office.coffee.domain.CoffeePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CoffeePackageRepository extends JpaRepository<CoffeePackage, Long> {

    List<CoffeePackage> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    List<CoffeePackage> findAllByOrderBySortOrderAscIdAsc();
}
