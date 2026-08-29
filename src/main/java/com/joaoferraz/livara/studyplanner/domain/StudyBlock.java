package com.joaoferraz.livara.studyplanner.domain;

import java.time.Duration;
import java.util.Objects;

public record StudyBlock(
        int order,
        FocusArea focus,
        String topic,
        Duration duration,
        int breakAfterMinutes) {

    public StudyBlock {
        if (order < 1) {
            throw new IllegalArgumentException("Block order must be positive");
        }
        Objects.requireNonNull(focus, "focus");
        topic = Objects.requireNonNull(topic, "topic").trim();
        if (topic.isBlank()) {
            throw new IllegalArgumentException("Block topic cannot be blank");
        }
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("Block duration must be positive");
        }
        if (breakAfterMinutes < 0) {
            throw new IllegalArgumentException("Break duration cannot be negative");
        }
    }

    public boolean isStandardHour() {
        return duration.equals(Duration.ofHours(1));
    }
}
