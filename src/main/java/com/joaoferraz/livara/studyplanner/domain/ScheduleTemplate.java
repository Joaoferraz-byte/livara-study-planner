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
    private final String iconId;
    private final Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> blocksByCycle;

    /** Backwards-compatible constructor for callers and schema-v1 documents. */
    public ScheduleTemplate(int schemaVersion, String name, Cycle cycle, int pauseMinutes,
                            Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        this(schemaVersion, name, cycle, WorkflowTemplate.MARKET_PROGRAMMING, pauseMinutes, blocksByDay);
    }

    /** Backwards-compatible constructor for a template that contains its active cycle projection. */
    public ScheduleTemplate(int schemaVersion, String name, Cycle cycle, WorkflowTemplate workflowTemplate,
                            int pauseMinutes, Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        this(schemaVersion, name, cycle, workflowTemplate, pauseMinutes, "layout-dashboard",
                singleCycle(cycle, blocksByDay), true);
    }

    private static Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> singleCycle(
            Cycle cycle, Map<DayOfWeek, List<StudyBlock>> blocksByDay) {
        EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> result = new EnumMap<>(Cycle.class);
        result.put(cycle, blocksByDay);
        return result;
    }

    private ScheduleTemplate(int schemaVersion, String name, Cycle cycle, WorkflowTemplate workflowTemplate,
                             int pauseMinutes, String iconId,
                             Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> blocksByCycle,
                             boolean complete) {
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
        this.iconId = Objects.requireNonNull(iconId, "iconId").trim();
        if (this.iconId.isBlank()) {
            throw new IllegalArgumentException("Template icon cannot be blank");
        }
        Objects.requireNonNull(blocksByCycle, "blocksByCycle");
        EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> copy = new EnumMap<>(Cycle.class);
        for (Cycle value : Cycle.values()) {
            Map<DayOfWeek, List<StudyBlock>> source = blocksByCycle.getOrDefault(value, Map.of());
            EnumMap<DayOfWeek, List<StudyBlock>> dayCopy = new EnumMap<>(DayOfWeek.class);
            for (DayOfWeek day : DayOfWeek.values()) {
                dayCopy.put(day, Collections.unmodifiableList(new ArrayList<>(source.getOrDefault(day, List.of()))));
            }
            copy.put(value, Collections.unmodifiableMap(dayCopy));
        }
        this.blocksByCycle = Collections.unmodifiableMap(copy);
    }

    public static ScheduleTemplate withCycles(int schemaVersion, String name, Cycle activeCycle,
                                              WorkflowTemplate workflowTemplate, int pauseMinutes,
                                              Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> blocksByCycle) {
        return withCycles(schemaVersion, name, activeCycle, workflowTemplate, pauseMinutes,
                "layout-dashboard", blocksByCycle);
    }

    public static ScheduleTemplate withCycles(int schemaVersion, String name, Cycle activeCycle,
                                              WorkflowTemplate workflowTemplate, int pauseMinutes, String iconId,
                                              Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> blocksByCycle) {
        return new ScheduleTemplate(schemaVersion, name, activeCycle, workflowTemplate, pauseMinutes,
                iconId, blocksByCycle, true);
    }

    public int schemaVersion() { return schemaVersion; }
    public String name() { return name; }
    public Cycle cycle() { return cycle; }
    public WorkflowTemplate workflowTemplate() { return workflowTemplate; }
    public int pauseMinutes() { return pauseMinutes; }
    public String iconId() { return iconId; }

    /** Returns the active cycle projection kept for existing callers. */
    public Map<DayOfWeek, List<StudyBlock>> blocksByDay() { return blocks(cycle); }
    public Map<DayOfWeek, List<StudyBlock>> blocks(Cycle value) { return blocksByCycle.get(value); }
    public List<StudyBlock> blocks(DayOfWeek day) { return blocks(cycle, day); }
    public List<StudyBlock> blocks(Cycle value, DayOfWeek day) {
        return blocksByCycle.getOrDefault(value, Map.of()).getOrDefault(day, List.of());
    }
    public Map<Cycle, Map<DayOfWeek, List<StudyBlock>>> blocksByCycle() { return blocksByCycle; }
    public int totalBlocks() { return totalBlocks(cycle); }
    public int totalBlocks(Cycle value) { return blocks(value).values().stream().mapToInt(List::size).sum(); }

    public ScheduleTemplate withCycle(Cycle newCycle) {
        return withCycles(schemaVersion, name, newCycle, workflowTemplate, pauseMinutes, iconId, blocksByCycle);
    }

    public ScheduleTemplate withWorkflowTemplate(WorkflowTemplate newWorkflowTemplate) {
        return withCycles(schemaVersion, name, cycle, newWorkflowTemplate, pauseMinutes, iconId, blocksByCycle);
    }
}
