package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultScheduleFactory {
    private DefaultScheduleFactory() {
    }

    public static ScheduleTemplate create(Cycle cycle) {
        EnumMap<DayOfWeek, List<StudyBlock>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            days.put(day, List.of());
        }

        days.put(DayOfWeek.MONDAY, List.of(
                block(1, FocusArea.MARKET_PROGRAMMING, "Programação para o mercado", 15),
                block(2, FocusArea.PROJECTS, "Aplicação do conhecimento em projeto", 0)));
        days.put(DayOfWeek.TUESDAY, List.of(
                block(1, FocusArea.LOGIC, "Treino de lógica e implementação", 0)));
        days.put(DayOfWeek.WEDNESDAY, List.of(
                block(1, FocusArea.ARCHITECTURE, "Arquitetura de software", 0)));
        days.put(DayOfWeek.THURSDAY, List.of(
                block(1, FocusArea.OPTIMIZATION, "Otimização de software", 0)));
        days.put(DayOfWeek.FRIDAY, List.of(
                block(1, cycle == Cycle.A ? FocusArea.PHYSICS : FocusArea.CHEMISTRY,
                        cycle == Cycle.A ? "Física" : "Química", 0)));
        days.put(DayOfWeek.SATURDAY, List.of(
                block(1, cycle == Cycle.A ? FocusArea.BIOLOGY : FocusArea.MATHEMATICS,
                        cycle == Cycle.A ? "Biologia" : "Matemática", 0)));

        return new ScheduleTemplate(1, "Ciclo semanal de estudos", cycle, 15, days);
    }

    private static StudyBlock block(int order, FocusArea focus, String topic, int breakAfterMinutes) {
        return new StudyBlock(order, focus, topic, Duration.ofHours(1), breakAfterMinutes);
    }
}
