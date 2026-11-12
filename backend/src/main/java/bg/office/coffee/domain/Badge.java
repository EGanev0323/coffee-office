package bg.office.coffee.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Баджове за изпити кафета (общо, от началото). Не се пазят в базата – смятат се от броя редове
 * в consumption, затова отменено кафе връща и бадж назад, а старата история се зачита сама.
 * Картинките са във frontend/src/assets/badges/ под същото име (CUPS_50 → cups-50.webp).
 */
public enum Badge {

    CUPS_50(50, "Кафе ценител"),
    CUPS_100(100, "Кафе познавач"),
    CUPS_500(500, "Кафе майстор"),
    CUPS_1000(1000, "Кафе легенда");

    private final int threshold;
    private final String title;

    Badge(int threshold, String title) {
        this.threshold = threshold;
        this.title = title;
    }

    public int threshold() { return threshold; }
    public String title() { return title; }

    /** Получените баджове при толкова изпити кафета, от най-малкия към най-големия. */
    public static List<Badge> earned(long coffees) {
        return Arrays.stream(values()).filter(b -> coffees >= b.threshold).toList();
    }

    /** Следващият бадж или празно, ако всички са получени. */
    public static Optional<Badge> next(long coffees) {
        return Arrays.stream(values()).filter(b -> coffees < b.threshold).findFirst();
    }

    /** Прагът на последния получен бадж (0, ако няма) – откъдето тръгва лентата за прогрес. */
    public static int previousThreshold(long coffees) {
        return earned(coffees).stream().mapToInt(Badge::threshold).max().orElse(0);
    }
}
