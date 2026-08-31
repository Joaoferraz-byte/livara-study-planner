package com.joaoferraz.livara.studyplanner.domain;

import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomCycleTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void acceptsAndPersistsMoreThanTwoCyclesWithTheirDefinitions() throws Exception {
        Cycle review = Cycle.custom("review", "Review", "Retrieval and review",
                List.of(FocusArea.REVIEW));
        Cycle build = Cycle.custom("build", "Build", "Projects and implementation", List.of());
        ScheduleTemplate original = DefaultScheduleFactory.createDraft(Cycle.A)
                .withAddedCycle(review, DefaultScheduleFactory.createDraft(review).blocks(review))
                .withAddedCycle(build, DefaultScheduleFactory.createDraft(build).blocks(build));

        ScheduleStore store = new ScheduleStore();
        Path file = temporaryDirectory.resolve("three-cycles.json");
        store.save(file, original);
        ScheduleTemplate loaded = store.load(file);

        assertEquals(List.of(Cycle.A, review, build), loaded.createdCycles());
        assertEquals("Review", loaded.createdCycles().get(1).label());
        assertEquals(List.of(FocusArea.REVIEW), loaded.createdCycles().get(1).requiredFocuses());
        assertTrue(ScheduleValidator.validate(loaded).isEmpty());
    }

    @Test
    void advancesThroughAllCreatedCyclesAndWrapsAround() {
        Cycle review = Cycle.custom("review", "Review", "Retrieval and review", List.of());
        Cycle build = Cycle.custom("build", "Build", "Projects and implementation", List.of());
        ScheduleTemplate template = DefaultScheduleFactory.createDraft(Cycle.A)
                .withAddedCycle(review, DefaultScheduleFactory.createDraft(review).blocks(review))
                .withAddedCycle(build, DefaultScheduleFactory.createDraft(build).blocks(build))
                .withCycle(Cycle.A);
        ScheduleService service = new ScheduleService(new ScheduleStore());

        ScheduleTemplate second = service.advanceCycle(template);
        ScheduleTemplate third = service.advanceCycle(second);
        ScheduleTemplate first = service.advanceCycle(third);

        assertEquals(review, second.cycle());
        assertEquals(build, third.cycle());
        assertEquals(Cycle.A, first.cycle());
    }
}
