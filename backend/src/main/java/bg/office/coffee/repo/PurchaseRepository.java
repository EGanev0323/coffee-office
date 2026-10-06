package bg.office.coffee.repo;

import bg.office.coffee.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    List<Purchase> findTop30ByUser_IdOrderByCreatedAtDesc(Long userId);

    /** null, ако няма покупки в периода. */
    @Query("select sum(p.amount) from Purchase p where p.createdAt >= :from and p.createdAt < :to")
    BigDecimal sumAmount(@Param("from") Instant from, @Param("to") Instant to);

    /** null, ако няма покупки в периода. */
    @Query("select sum(p.coffeeCount) from Purchase p where p.createdAt >= :from and p.createdAt < :to")
    Long sumCoffees(@Param("from") Instant from, @Param("to") Instant to);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    /** Редове: [userId, сума, брой кафета] */
    @Query("select p.user.id, sum(p.amount), sum(p.coffeeCount) from Purchase p " +
           "where p.createdAt >= :from and p.createdAt < :to group by p.user.id")
    List<Object[]> totalsPerUser(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select p from Purchase p join fetch p.user left join fetch p.createdBy " +
           "where p.createdAt >= :from and p.createdAt < :to order by p.createdAt desc")
    List<Purchase> findInRange(@Param("from") Instant from, @Param("to") Instant to);
}
