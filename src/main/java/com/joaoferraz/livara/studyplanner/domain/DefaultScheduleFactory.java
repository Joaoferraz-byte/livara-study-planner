package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DefaultScheduleFactory {
    private DefaultScheduleFactory() {
    }

    public static ScheduleTemplate create(Cycle cycle) {
        return create(cycle, WorkflowTemplate.MARKET_PROGRAMMING);
    }

    public static ScheduleTemplate create(Cycle cycle, WorkflowTemplate workflowTemplate) {
        LinkedHashMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new LinkedHashMap<>();
        for (Cycle value : builtInCyclesFor(cycle)) {
            cycles.put(value, project(value, workflowTemplate));
        }
        return ScheduleTemplate.withCycles(2, workflowTemplate.label(), cycle, workflowTemplate, 15, cycles);
    }

    /**
     * Creates the first editable state of a user-created template. It starts
     * with a valid sequence so selecting it never persists an invalid library.
     */
    public static ScheduleTemplate createDraft(Cycle cycle) {
        WorkflowTemplate workflow = WorkflowTemplate.MARKET_PROGRAMMING;
        LinkedHashMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new LinkedHashMap<>();
        cycles.put(cycle, project(cycle, workflow));
        return ScheduleTemplate.withCycles(2, "New study template", cycle,
                workflow, 15, "layout-dashboard", cycles);
    }

    public static Map<DayOfWeek, List<StudyBlock>> emptyCycle() {
        java.util.EnumMap<DayOfWeek, List<StudyBlock>> days = new java.util.EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, List.of());
        }
        return days;
    }

    private static List<Cycle> builtInCyclesFor(Cycle activeCycle) {
        if (activeCycle.equals(Cycle.A) || activeCycle.equals(Cycle.B)) {
            return List.of(Cycle.A, Cycle.B);
        }
        return List.of(activeCycle);
    }

    private static Map<DayOfWeek, List<StudyBlock>> project(Cycle cycle, WorkflowTemplate workflowTemplate) {
        return project(cycle, sequence(cycle, workflowTemplate));
    }

    private static Map<DayOfWeek, List<StudyBlock>> project(Cycle cycle, List<StudyBlock> sequence) {
        DayOfWeek[] activeDays = {
                DayOfWeek.MONDAY,
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY,
                DayOfWeek.SATURDAY,
                DayOfWeek.SATURDAY
        };
        java.util.EnumMap<DayOfWeek, List<StudyBlock>> grouped = new java.util.EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            grouped.put(day, new ArrayList<>());
        }
        for (int index = 0; index < sequence.size(); index++) {
            StudyBlock source = sequence.get(index);
            DayOfWeek day = index < activeDays.length ? activeDays[index] : DayOfWeek.SATURDAY;
            List<StudyBlock> dayBlocks = grouped.get(day);
            int order = dayBlocks.size() + 1;
            int pause = index + 1 < sequence.size() ? 15 : 0;
            dayBlocks.add(new StudyBlock(order, source.focus(), source.topic(), source.duration(), pause));
        }
        java.util.EnumMap<DayOfWeek, List<StudyBlock>> result = new java.util.EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day, List.copyOf(grouped.get(day)));
        }
        return result;
    }

    public static List<StudyBlock> sequence(Cycle cycle, WorkflowTemplate workflowTemplate) {
        FocusArea primary = workflowTemplate.primaryFocus();
        FocusArea secondary = workflowTemplate.secondaryFocus();

        List<StudyBlock> sequence = new ArrayList<>();
        add(sequence, primary, topic(primary, workflowTemplate, "Main focus"));
        add(sequence, secondary, topic(secondary, workflowTemplate, "Application and connection"));
        addIfMissing(sequence, FocusArea.MARKET_PROGRAMMING, workflowTemplate, "Professional practice");
        addIfMissing(sequence, FocusArea.PROJECTS, workflowTemplate, "Applied project");
        addIfMissing(sequence, FocusArea.LOGIC, workflowTemplate, "Solve and implement");
        addIfMissing(sequence, FocusArea.ARCHITECTURE, workflowTemplate, "Design and justify");
        addIfMissing(sequence, FocusArea.OPTIMIZATION, workflowTemplate, "Measure and improve");
        for (FocusArea focus : cycle.requiredFocuses()) {
            addIfMissing(sequence, focus, cycle.subjects() + " · concept retrieval");
        }
        add(sequence, primary, topic(primary, workflowTemplate, "Spaced retrieval"));
        return List.copyOf(sequence);
    }

    private static String topic(FocusArea focus, WorkflowTemplate workflowTemplate, String suffix) {
        return focus.label() + " · " + suffix + " · " + workflowTemplate.label();
    }

    private static void addIfMissing(List<StudyBlock> sequence, FocusArea focus,
                                     WorkflowTemplate workflowTemplate, String suffix) {
        if (sequence.stream().noneMatch(block -> block.focus() == focus)) {
            add(sequence, focus, topic(focus, workflowTemplate, suffix));
        }
    }

    private static void addIfMissing(List<StudyBlock> sequence, FocusArea focus, String topic) {
        if (sequence.stream().noneMatch(block -> block.focus() == focus)) {
            add(sequence, focus, topic);
        }
    }

    private static void add(List<StudyBlock> sequence, FocusArea focus, String topic) {
        sequence.add(new StudyBlock(sequence.size() + 1, focus, topic, Duration.ofHours(1), 15));
    }
}
