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
    private final WorkflowTemplate workflowTemplate;
    private final int pauseMinutes;
    private final Map<DayOfWeek, List<StudyBlock>> blocksByDay;

    /**
     * Backwards-compatible constructor for callers and schema-v1 documents.
     */
    public ScheduleTemplate(int schemaVersion, String name, Cycle cycle, int pauseMinutes,
                            Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        this(schemaVersion, name, cycle, WorkflowTemplate.MARKET_PROGRAMMING, pauseMinutes, blocksByDay);
    }

    public ScheduleTemplate(int schemaVersion, String name, Cycle cycle, WorkflowTemplate workflowTemplate,
                            int pauseMinutes, Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Schema version must be positive");
        }
        this.schemaVersion = schemaVersion;
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be blank");
        }
        this.cycle = Objects.requireNonNull(cycle, "cycle");
        this.workflowTemplate = Objects.requireNonNull(workflowTemplate, "workflowTemplate");
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

    public WorkflowTemplate workflowTemplate() {
        return workflowTemplate;
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
        return new ScheduleTemplate(schemaVersion, name, newCycle, workflowTemplate, pauseMinutes, blocksByDay);
    }

    public ScheduleTemplate withWorkflowTemplate(WorkflowTemplate newWorkflowTemplate) {
        return new ScheduleTemplate(schemaVersion, newWorkflowTemplate.label(), cycle, newWorkflowTemplate,
                pauseMinutes, blocksByDay);
    }
}
