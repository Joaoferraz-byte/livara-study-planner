package com.joaoferraz.livara.studyplanner.domain;

import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScheduleServiceTest {
    @Test
    void advanceCyclePreservesTemplateDocumentAndChangesOnlyActiveCycle() {
        ScheduleTemplate defaults = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
        ScheduleTemplate custom = ScheduleTemplate.withCycles(
                2,
                "Deep work template",
                Cycle.A,
                WorkflowTemplate.MARKET_PROGRAMMING,
                15,
                "x",
                defaults.blocksByCycle());

        ScheduleTemplate advanced = new ScheduleService(new ScheduleStore()).advanceCycle(custom);

        assertEquals(Cycle.B, advanced.cycle());
        assertEquals(custom.name(), advanced.name());
        assertEquals(custom.iconId(), advanced.iconId());
        assertEquals(custom.workflowTemplate(), advanced.workflowTemplate());
        assertEquals(custom.pauseMinutes(), advanced.pauseMinutes());
        assertEquals(custom.blocksByCycle(), advanced.blocksByCycle());
        assertEquals(custom.totalBlocks(Cycle.B), advanced.totalBlocks(Cycle.B));
    }

    @Test
    void editingIntoAnotherCycleProjectsTheSequenceIntoThatCycle() {
        ScheduleTemplate defaults = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
        ScheduleTemplate targetCycle = DefaultScheduleFactory.create(Cycle.B, WorkflowTemplate.MARKET_PROGRAMMING);

        ScheduleTemplate edited = new ScheduleService(new ScheduleStore()).editTemplate(
                defaults,
                "New study template",
                Cycle.B,
                WorkflowTemplate.MARKET_PROGRAMMING,
                15,
                "x",
                targetCycle.sequence());

        assertEquals(Cycle.B, edited.cycle());
        assertEquals(defaults.sequence().size(), edited.sequence().size());
        assertEquals(targetCycle.sequence().stream().map(StudyBlock::topic).toList(),
                edited.sequence().stream().map(StudyBlock::topic).toList());
        assertEquals(defaults.blocks(Cycle.A), edited.blocks(Cycle.A));
    }

    @Test
    void withIdentityPreservesCyclesWhileChangingOnlyPresentationIdentity() {
        ScheduleTemplate defaults = DefaultScheduleFactory.create(Cycle.B, WorkflowTemplate.MARKET_PROGRAMMING);

        ScheduleTemplate renamed = defaults.withIdentity("My template", "x");

        assertEquals("My template", renamed.name());
        assertEquals("x", renamed.iconId());
        assertEquals(defaults.blocksByCycle(), renamed.blocksByCycle());
        assertEquals(defaults.cycle(), renamed.cycle());
        assertEquals(defaults.workflowTemplate(), renamed.workflowTemplate());
    }
}
