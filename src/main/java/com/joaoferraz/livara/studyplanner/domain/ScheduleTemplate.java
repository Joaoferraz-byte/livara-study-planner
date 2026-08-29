package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleTemplate {
    private final int schemaVersion;
    private final String name;
    private final Cycle cycle;
    private final int pauseMinutes;
    private final Map<DayOfWeek, List<StudyBlock>> blocksByDay;

    public ScheduleTemplate(int schemaVersion, String name, Cycle cycle, int pauseMinutes,
                            Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Schema version must be positive");
        }
        this.schemaVersion = schemaVersion;
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be blank");
        }
        this.cycle = Objects.requireNonNull(cycle, "cycle");
        if (pauseMinutes < 0) {
            throw new IllegalArgumentException("Pause duration cannot be negative");
        }
        this.pauseMinutes = pauseMinutes;
        Objects.requireNonNull(blocksByDay, "blocksByDay");
        EnumMap<DayOfWeek, List<StudyBlock>> copy = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            List<StudyBlock> blocks = blocksByDay.getOrDefault(day, List.of());
            copy.put(day, Collections.unmodifiableList(new ArrayList<>(blocks)));
        }
        this.blocksByDay = Collections.unmodifiableMap(copy);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String name() {
        return name;
    }

    public Cycle cycle() {
        return cycle;
    }

    public int pauseMinutes() {
        return pauseMinutes;
    }

    public Map<DayOfWeek, List<StudyBlock>> blocksByDay() {
        return blocksByDay;
    }

    public List<StudyBlock> blocks(DayOfWeek day) {
        return blocksByDay.getOrDefault(day, List.of());
    }

    public int totalBlocks() {
        return blocksByDay.values().stream().mapToInt(List::size).sum();
    }

    public ScheduleTemplate withCycle(Cycle newCycle) {
        return new ScheduleTemplate(schemaVersion, name, newCycle, pauseMinutes, blocksByDay);
    }
}
