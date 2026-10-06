package bg.office.coffee.repo;

import bg.office.coffee.domain.BoardStroke;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardStrokeRepository extends JpaRepository<BoardStroke, Long> {

    List<BoardStroke> findAllByOrderByIdAsc();
}