package com.joaoferraz.livara.studyplanner.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultScheduleFactoryTest {
    @Test
    void createsASequentialWorkflowWithPrimaryFocusRepeated() {
        ScheduleTemplate schedule = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
        List<StudyBlock> sequence = DefaultScheduleFactory.sequence(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);

        assertEquals(8, sequence.size());
        assertEquals(2, sequence.stream().filter(block -> block.focus() == FocusArea.MARKET_PROGRAMMING).count());
        assertEquals(15, schedule.pauseMinutes());
        assertEquals(8, schedule.totalBlocks());
        assertTrue(ScheduleValidator.validate(schedule).isEmpty());
        assertTrue(sequence.stream().allMatch(block -> block.duration().equals(Duration.ofHours(1))));
    }

    @Test
    void rotatesOnlyTheSchoolPairBetweenCycles() {
        List<StudyBlock> cycleA = DefaultScheduleFactory.sequence(Cycle.A, WorkflowTemplate.SCHOOL_SUBJECTS);
        List<StudyBlock> cycleB = DefaultScheduleFactory.sequence(Cycle.B, WorkflowTemplate.SCHOOL_SUBJECTS);

        assertEquals(EnumSet.of(FocusArea.PHYSICS, FocusArea.BIOLOGY),
                EnumSet.copyOf(cycleA.stream().map(StudyBlock::focus).filter(this::isSchool).toList()));
        assertEquals(EnumSet.of(FocusArea.CHEMISTRY, FocusArea.MATHEMATICS),
                EnumSet.copyOf(cycleB.stream().map(StudyBlock::focus).filter(this::isSchool).toList()));
        assertTrue(ScheduleValidator.validate(DefaultScheduleFactory.create(Cycle.B, WorkflowTemplate.SCHOOL_SUBJECTS)).isEmpty());
    }

    @Test
    void legacyConstructorDefaultsToMarketProgrammingWorkflow() {
        ScheduleTemplate schedule = DefaultScheduleFactory.create(Cycle.A);

        assertEquals(WorkflowTemplate.MARKET_PROGRAMMING, schedule.workflowTemplate());
        assertEquals(2, schedule.schemaVersion());
    }

    private boolean isSchool(FocusArea focus) {
        return switch (focus) {
            case PHYSICS, BIOLOGY, CHEMISTRY, MATHEMATICS -> true;
            default -> false;
        };
    }
}
