package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.ProgressState;
import com.joaoferraz.livara.studyplanner.domain.WorkflowTemplate;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressStoreTest {
    @Test
    void savesAndLoadsCompletionForTheSameCycleAndWorkflow() throws Exception {
        Path file = Files.createTempFile("livara-progress", ".properties");
        ProgressStore store = new ProgressStore();
        ProgressState state = ProgressState.empty(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING).toggle("study-1");

        store.save(file, state);

        ProgressState loaded = store.loadOrEmpty(file, Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
        assertTrue(loaded.isCompleted("study-1"));
        assertFalse(loaded.isCompleted("study-2"));
    }

    @Test
    void resetsProgressWhenCycleChanges() throws Exception {
        Path file = Files.createTempFile("livara-progress", ".properties");
        ProgressStore store = new ProgressStore();
        store.save(file, ProgressState.empty(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING).toggle("study-1"));

        ProgressState cycleChanged = store.loadOrEmpty(file, Cycle.B, WorkflowTemplate.MARKET_PROGRAMMING);

        assertFalse(cycleChanged.isCompleted("study-1"));
    }
}
