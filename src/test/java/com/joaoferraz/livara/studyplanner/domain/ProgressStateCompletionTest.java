package com.joaoferraz.livara.studyplanner.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressStateCompletionTest {

    @Test
    void canSetAndUnsetIndividualCompletionDeterministically() {
        ProgressState state = ProgressState.empty(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING)
                .withCompleted("study-1.study", true)
                .withCompleted("study-1.annotation", true);

        assertTrue(state.isCompleted("study-1.study"));
        assertTrue(state.isCompleted("study-1.annotation"));

        ProgressState reopened = state.withCompleted("study-1.annotation", false);
        assertTrue(reopened.isCompleted("study-1.study"));
        assertFalse(reopened.isCompleted("study-1.annotation"));
    }

    @Test
    void canReplaceParentAndChildCompletionSetAsOneSnapshot() {
        ProgressState state = ProgressState.empty(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING)
                .withCompletedItems(Set.of(
                        "study-1",
                        "study-1.study",
                        "study-1.annotation",
                        "study-1.practice",
                        "study-1.apply"));

        assertTrue(state.isCompleted("study-1"));
        assertTrue(state.isCompleted("study-1.apply"));
        assertFalse(state.isCompleted("study-2"));
    }
}
