package bg.office.coffee.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Една линия, нарисувана с маркер върху общата дъска. */
@Entity
@Table(name = "board_stroke")
public class BoardStroke {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 7)
    private String color;

    /** JSON масив от точки [x, y]. */
    @Column(nullable = false)
    private String points;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getPoints() { return points; }
    public void setPoints(String points) { this.points = points; }
    public Instant getCreatedAt() { return createdAt; }
}