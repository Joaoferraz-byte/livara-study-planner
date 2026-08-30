package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultScheduleFactory {
    private DefaultScheduleFactory() {
    }

    public static ScheduleTemplate create(Cycle cycle) {
        return create(cycle, WorkflowTemplate.MARKET_PROGRAMMING);
    }

    public static ScheduleTemplate create(Cycle cycle, WorkflowTemplate workflowTemplate) {
        EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new EnumMap<>(Cycle.class);
        for (Cycle value : Cycle.values()) {
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
        EnumMap<Cycle, Map<DayOfWeek, List<StudyBlock>>> cycles = new EnumMap<>(Cycle.class);
        cycles.put(cycle, project(cycle, workflow));
        return ScheduleTemplate.withCycles(2, "New study template", cycle,
                workflow, 15, "layout-dashboard", cycles);
    }

    public static Map<DayOfWeek, List<StudyBlock>> emptyCycle() {
        EnumMap<DayOfWeek, List<StudyBlock>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, List.of());
        }
        return days;
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
        EnumMap<DayOfWeek, List<StudyBlock>> grouped = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            grouped.put(day, new ArrayList<>());
        }
        for (int index = 0; index < sequence.size(); index++) {
            StudyBlock source = sequence.get(index);
            List<StudyBlock> dayBlocks = grouped.get(activeDays[index]);
            int order = dayBlocks.size() + 1;
            int pause = index + 1 < sequence.size() ? 15 : 0;
            dayBlocks.add(new StudyBlock(order, source.focus(), source.topic(), source.duration(), pause));
        }
        EnumMap<DayOfWeek, List<StudyBlock>> result = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            result.put(day, List.copyOf(grouped.get(day)));
        }
        return result;
    }

    private static List<StudyBlock> draftSequence(Cycle cycle) {
        FocusArea firstSchool = cycle == Cycle.A ? FocusArea.PHYSICS : FocusArea.CHEMISTRY;
        FocusArea secondSchool = cycle == Cycle.A ? FocusArea.BIOLOGY : FocusArea.MATHEMATICS;
        return List.of(
                new StudyBlock(1, FocusArea.REVIEW, "New study goal", Duration.ofHours(1), 15),
                new StudyBlock(2, FocusArea.MARKET_PROGRAMMING, "New professional practice block", Duration.ofHours(1), 15),
                new StudyBlock(3, FocusArea.PROJECTS, "New applied project block", Duration.ofHours(1), 15),
                new StudyBlock(4, FocusArea.LOGIC, "New implementation block", Duration.ofHours(1), 15),
                new StudyBlock(5, FocusArea.ARCHITECTURE, "New design block", Duration.ofHours(1), 15),
                new StudyBlock(6, FocusArea.OPTIMIZATION, "New measurement block", Duration.ofHours(1), 15),
                new StudyBlock(7, firstSchool, "New science block 1", Duration.ofHours(1), 15),
                new StudyBlock(8, secondSchool, "New science block 2", Duration.ofHours(1), 15));
    }

    public static List<StudyBlock> sequence(Cycle cycle, WorkflowTemplate workflowTemplate) {
        FocusArea schoolOne = cycle == Cycle.A ? FocusArea.PHYSICS : FocusArea.CHEMISTRY;
        FocusArea schoolTwo = cycle == Cycle.A ? FocusArea.BIOLOGY : FocusArea.MATHEMATICS;
        String schoolOneTopic = cycle == Cycle.A ? "Physics · concept retrieval" : "Chemistry · concept retrieval";
        String schoolTwoTopic = cycle == Cycle.A ? "Biology · concept retrieval" : "Mathematics · concept retrieval";
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
        add(sequence, schoolOne, schoolOneTopic);
        add(sequence, schoolTwo, schoolTwoTopic);
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

    private static void add(List<StudyBlock> sequence, FocusArea focus, String topic) {
        sequence.add(new StudyBlock(sequence.size() + 1, focus, topic, Duration.ofHours(1), 15));
    }
}
