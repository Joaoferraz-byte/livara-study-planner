package com.joaoferraz.livara.studyplanner.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleValidatorTest {
    @Test
    void defaultCycleAIsAReusableSevenBlockWeek() {
        ScheduleTemplate template = DefaultScheduleFactory.create(Cycle.A);

        assertEquals(7, template.totalBlocks());
        assertTrue(ScheduleValidator.validate(template).isEmpty());
        assertEquals(2, template.blocks(DayOfWeek.MONDAY).size());
        assertEquals(15, template.blocks(DayOfWeek.MONDAY).getFirst().breakAfterMinutes());
        assertEquals(0, template.blocks(DayOfWeek.MONDAY).getLast().breakAfterMinutes());
        assertTrue(template.blocks(DayOfWeek.FRIDAY).stream()
                .anyMatch(block -> block.focus() == FocusArea.PHYSICS));
        assertTrue(template.blocks(DayOfWeek.SATURDAY).stream()
                .anyMatch(block -> block.focus() == FocusArea.BIOLOGY));
    }

    @Test
    void cycleBChangesOnlyTheAlternatingSchoolSubjects() {
        ScheduleTemplate template = DefaultScheduleFactory.create(Cycle.B);

        assertEquals(7, template.totalBlocks());
        assertTrue(ScheduleValidator.validate(template).isEmpty());
        assertEquals(FocusArea.CHEMISTRY, template.blocks(DayOfWeek.FRIDAY).getFirst().focus());
        assertEquals(FocusArea.MATHEMATICS, template.blocks(DayOfWeek.SATURDAY).getFirst().focus());
        assertEquals(Cycle.B, template.withCycle(Cycle.B).cycle());
    }
}
