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
