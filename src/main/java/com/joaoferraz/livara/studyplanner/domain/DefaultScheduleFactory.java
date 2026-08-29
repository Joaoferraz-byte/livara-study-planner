package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public final class DefaultScheduleFactory {
    private DefaultScheduleFactory() {
    }

    public static ScheduleTemplate create(Cycle cycle) {
        return create(cycle, WorkflowTemplate.MARKET_PROGRAMMING);
    }

    public static ScheduleTemplate create(Cycle cycle, WorkflowTemplate workflowTemplate) {
        List<StudyBlock> sequence = sequence(cycle, workflowTemplate);
        EnumMap<DayOfWeek, List<StudyBlock>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, List.of());
        }

        // The domain remains compatible with the existing weekday JSON schema.
        // The UI treats these entries as one ordered sequence of focus blocks;
        // weekdays are retained only as a persistence/legacy projection.
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
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, List.copyOf(grouped.get(day)));
        }

        return new ScheduleTemplate(2, workflowTemplate.label(), cycle, workflowTemplate, 15, days);
    }

    public static List<StudyBlock> sequence(Cycle cycle, WorkflowTemplate workflowTemplate) {
        FocusArea schoolOne = cycle == Cycle.A ? FocusArea.PHYSICS : FocusArea.CHEMISTRY;
        FocusArea schoolTwo = cycle == Cycle.A ? FocusArea.BIOLOGY : FocusArea.MATHEMATICS;
        String schoolOneTopic = cycle == Cycle.A ? "Física · recuperação de conceitos" : "Química · recuperação de conceitos";
        String schoolTwoTopic = cycle == Cycle.A ? "Biologia · recuperação de conceitos" : "Matemática · recuperação de conceitos";
        FocusArea primary = workflowTemplate.primaryFocus();
        FocusArea secondary = workflowTemplate.secondaryFocus();

        List<StudyBlock> sequence = new ArrayList<>();
        add(sequence, primary, topic(primary, workflowTemplate, "Bloco principal"));
        add(sequence, secondary, topic(secondary, workflowTemplate, "Aplicação e conexão"));
        addIfMissing(sequence, FocusArea.MARKET_PROGRAMMING, workflowTemplate, "Prática profissional");
        addIfMissing(sequence, FocusArea.PROJECTS, workflowTemplate, "Projeto aplicado");
        addIfMissing(sequence, FocusArea.LOGIC, workflowTemplate, "Resolver e implementar");
        addIfMissing(sequence, FocusArea.ARCHITECTURE, workflowTemplate, "Desenhar e justificar");
        addIfMissing(sequence, FocusArea.OPTIMIZATION, workflowTemplate, "Medir e melhorar");
        add(sequence, schoolOne, schoolOneTopic);
        add(sequence, schoolTwo, schoolTwoTopic);
        add(sequence, primary, topic(primary, workflowTemplate, "Recuperação espaçada"));
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
