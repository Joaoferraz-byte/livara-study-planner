package com.joaoferraz.livara.studyplanner.domain;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Runtime execution state kept separate from the reusable schedule template. */
public final class ProgressState {
    private final int schemaVersion;
    private final Cycle cycle;
    private final WorkflowTemplate workflowTemplate;
    private final Set<String> completedItems;
    private final int activeItemIndex;

    public ProgressState(int schemaVersion, Cycle cycle, WorkflowTemplate workflowTemplate,
                         Set<String> completedItems, int activeItemIndex) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Progress schema version must be positive");
        }
        this.schemaVersion = schemaVersion;
        this.cycle = Objects.requireNonNull(cycle, "cycle");
        this.workflowTemplate = Objects.requireNonNull(workflowTemplate, "workflowTemplate");
        this.completedItems = Set.copyOf(new LinkedHashSet<>(Objects.requireNonNull(completedItems, "completedItems")));
        this.activeItemIndex = Math.max(0, activeItemIndex);
    }

    public static ProgressState empty(Cycle cycle, WorkflowTemplate workflowTemplate) {
        return new ProgressState(1, cycle, workflowTemplate, Set.of(), 0);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public Cycle cycle() {
        return cycle;
    }

    public WorkflowTemplate workflowTemplate() {
        return workflowTemplate;
    }

    public Set<String> completedItems() {
        return completedItems;
    }

    public int activeItemIndex() {
        return activeItemIndex;
    }

    public boolean isCompleted(String itemId) {
        return completedItems.contains(itemId);
    }

    public ProgressState toggle(String itemId) {
        Objects.requireNonNull(itemId, "itemId");
        return withCompleted(itemId, !isCompleted(itemId));
    }

    public ProgressState withCompleted(String itemId, boolean completed) {
        Objects.requireNonNull(itemId, "itemId");
        LinkedHashSet<String> updated = new LinkedHashSet<>(completedItems);
        if (completed) {
            updated.add(itemId);
        } else {
            updated.remove(itemId);
        }
        return new ProgressState(schemaVersion, cycle, workflowTemplate, updated, activeItemIndex);
    }

    public ProgressState withCompletedItems(Set<String> itemIds) {
        return new ProgressState(schemaVersion, cycle, workflowTemplate,
                Objects.requireNonNull(itemIds, "itemIds"), activeItemIndex);
    }

    public ProgressState withActiveItemIndex(int itemIndex) {
        return new ProgressState(schemaVersion, cycle, workflowTemplate, completedItems, itemIndex);
    }
}
