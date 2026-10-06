package bg.office.coffee.repo;

import bg.office.coffee.domain.Consumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConsumptionRepository extends JpaRepository<Consumption, Long> {

    List<Consumption> findTop30ByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<Consumption> findFirstByUser_IdOrderByCreatedAtDesc(Long userId);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    /** Редове: [userId, брой изпити] */
    @Query("select c.user.id, count(c) from Consumption c " +
           "where c.createdAt >= :from and c.createdAt < :to group by c.user.id")
    List<Object[]> countsPerUser(@Param("from") Instant from, @Param("to") Instant to);
}
