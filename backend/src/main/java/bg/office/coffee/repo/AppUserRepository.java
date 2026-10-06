package bg.office.coffee.repo;

import bg.office.coffee.domain.AppUser;
import bg.office.coffee.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByRoleAndActiveTrue(Role role);

    List<AppUser> findAllByOrderByDisplayNameAsc();

       /** За общата дъска: всички активни колеги, които не са скрити от админа – и тези с 0 кафета. */
    @Query("select u from AppUser u where u.active = true and u.boardVisible = true order by u.displayName")
    List<AppUser> findBoard();

    /** Атомарно увеличава баланса – без race condition при едновременни заявки. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AppUser u set u.balance = u.balance + :amount where u.id = :id")
    int increaseBalance(@Param("id") Long id, @Param("amount") int amount);

    /** Атомарно намалява баланса само ако има достатъчно кафета. Връща 0, ако няма. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AppUser u set u.balance = u.balance - :amount where u.id = :id and u.balance >= :amount")
    int decreaseBalance(@Param("id") Long id, @Param("amount") int amount);

    /** Предплатени, още неизпити кафета. Колегите с безкрайни кафета не се броят. null, ако няма такива. */
    @Query("select sum(u.balance) from AppUser u where u.unlimited = false")
    Long totalBalance();
}
