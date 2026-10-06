package bg.office.coffee.web;

import bg.office.coffee.security.AuthUser;
import bg.office.coffee.service.BoardEvents;
import bg.office.coffee.service.DrawingService;
import bg.office.coffee.web.dto.StrokeDto;
import bg.office.coffee.web.dto.StrokeRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/** Рисуване с маркер по общата дъска – за всички влезли колеги. */
@RestController
@RequestMapping("/api/board")
public class BoardController {

    private final DrawingService drawingService;
    private final BoardEvents boardEvents;

    public BoardController(DrawingService drawingService, BoardEvents boardEvents) {
        this.drawingService = drawingService;
        this.boardEvents = boardEvents;
    }

    /** Поток от събития в реално време (Server-Sent Events). */
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter events(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // nginx да не буферира потока
        return boardEvents.subscribe();
    }

    @GetMapping("/strokes")
    public List<StrokeDto> strokes() {
        return drawingService.list();
    }

    @PostMapping("/strokes")
    @ResponseStatus(HttpStatus.CREATED)
    public StrokeDto addStroke(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody StrokeRequest req) {
        StrokeDto stroke = drawingService.add(me.id(), req);
        boardEvents.publish("stroke", stroke); // след commit на транзакцията
        return stroke;
    }

    @DeleteMapping("/strokes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearStrokes(@AuthenticationPrincipal AuthUser me) {
        drawingService.clear();
        boardEvents.publish("clear", Map.of("by", me.id()));
    }
}