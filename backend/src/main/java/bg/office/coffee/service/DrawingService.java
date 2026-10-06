package bg.office.coffee.service;

import bg.office.coffee.domain.BoardStroke;
import bg.office.coffee.repo.AppUserRepository;
import bg.office.coffee.repo.BoardStrokeRepository;
import bg.office.coffee.web.dto.StrokeDto;
import bg.office.coffee.web.dto.StrokeRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Рисунките с маркер върху общата дъска. */
@Service
public class DrawingService {

    /** Цветовете на трите маркера от поставката + ERASER – линия, която трие рисунките под себе си. */
    public static final Set<String> COLORS = Set.of("#1E4BAF", "#B8372A", "#2B1F18", "ERASER");

    private static final int MAX_STROKES = 1000;
    private static final double MAX_Y = 10.0; // y е спрямо ширината – висока дъска на телефон може да е > 1

    private final BoardStrokeRepository strokes;
    private final AppUserRepository users;
    private final ObjectMapper json;

    public DrawingService(BoardStrokeRepository strokes, AppUserRepository users, ObjectMapper json) {
        this.strokes = strokes;
        this.users = users;
        this.json = json;
    }

    @Transactional(readOnly = true)
    public List<StrokeDto> list() {
        return strokes.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public StrokeDto add(Long userId, StrokeRequest req) {
        String color = req.color().trim().toUpperCase(Locale.ROOT);
        if (!COLORS.contains(color)) {
            throw new BusinessException("Непознат цвят на маркера.");
        }
        for (double[] p : req.points()) {
            if (p == null || p.length != 2
                    || !Double.isFinite(p[0]) || !Double.isFinite(p[1])
                    || p[0] < 0 || p[0] > 1 || p[1] < 0 || p[1] > MAX_Y) {
                throw new BusinessException("Невалидна рисунка.");
            }
        }
        if (strokes.count() >= MAX_STROKES) {
            throw new BusinessException("Дъската е пълна. Изтрий рисунките с гъбата и започни отначало.");
        }
        BoardStroke stroke = new BoardStroke();
        stroke.setUser(users.getReferenceById(userId));
        stroke.setColor(color);
        stroke.setPoints(write(req.points()));
        return toDto(strokes.save(stroke));
    }

    @Transactional
    public void clear() {
        strokes.deleteAllInBatch();
    }

    private StrokeDto toDto(BoardStroke s) {
        return new StrokeDto(s.getId(), s.getUser().getId(), s.getColor(), read(s.getPoints()));
    }

    private String write(double[][] points) {
        try {
            return json.writeValueAsString(points);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Рисунката не може да се запише.", e);
        }
    }

    private double[][] read(String points) {
        try {
            return json.readValue(points, double[][].class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Повредена рисунка в базата.", e);
        }
    }
}