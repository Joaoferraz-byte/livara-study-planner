package com.joaoferraz.livara.studyplanner.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persistent collection of reusable schedule templates and its active selection.
 * The library is immutable so UI transitions cannot leave identity, sequence and
 * selected template out of sync.
 */
public final class TemplateLibrary {
    public record Entry(String id, ScheduleTemplate schedule) {
        public Entry {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Template id cannot be blank");
            }
            Objects.requireNonNull(schedule, "schedule");
        }
    }

    private final int schemaVersion;
    private final String selectedTemplateId;
    private final List<Entry> entries;

    public TemplateLibrary(int schemaVersion, String selectedTemplateId, List<Entry> entries) {
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Library schema version must be positive");
        }
        Objects.requireNonNull(selectedTemplateId, "selectedTemplateId");
        if (selectedTemplateId.isBlank()) {
            throw new IllegalArgumentException("Selected template id cannot be blank");
        }
        Objects.requireNonNull(entries, "entries");
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Template library cannot be empty");
        }
        Map<String, Entry> unique = new LinkedHashMap<>();
        for (Entry entry : entries) {
            if (unique.put(entry.id(), entry) != null) {
                throw new IllegalArgumentException("Duplicate template id: " + entry.id());
            }
        }
        if (!unique.containsKey(selectedTemplateId)) {
            throw new IllegalArgumentException("Selected template does not exist: " + selectedTemplateId);
        }
        this.schemaVersion = schemaVersion;
        this.selectedTemplateId = selectedTemplateId;
        this.entries = List.copyOf(unique.values());
    }

    public static TemplateLibrary single(String id, ScheduleTemplate schedule) {
        return new TemplateLibrary(1, id, List.of(new Entry(id, schedule)));
    }

    public static TemplateLibrary single(ScheduleTemplate schedule) {
        return single("default", schedule);
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public String selectedTemplateId() {
        return selectedTemplateId;
    }

    public List<Entry> entries() {
        return entries;
    }

    public Entry selectedEntry() {
        return entries.stream()
                .filter(entry -> entry.id().equals(selectedTemplateId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Selected template disappeared"));
    }

    public ScheduleTemplate selected() {
        return selectedEntry().schedule();
    }

    public TemplateLibrary select(String id) {
        requireEntry(id);
        return new TemplateLibrary(schemaVersion, id, entries);
    }

    public TemplateLibrary updateSelected(ScheduleTemplate schedule) {
        Objects.requireNonNull(schedule, "schedule");
        return replace(selectedTemplateId, schedule);
    }

    public TemplateLibrary replace(String id, ScheduleTemplate schedule) {
        Objects.requireNonNull(schedule, "schedule");
        List<Entry> updated = new ArrayList<>(entries.size());
        boolean found = false;
        for (Entry entry : entries) {
            if (entry.id().equals(id)) {
                updated.add(new Entry(id, schedule));
                found = true;
            } else {
                updated.add(entry);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Template does not exist: " + id);
        }
        return new TemplateLibrary(schemaVersion, selectedTemplateId, updated);
    }

    public TemplateLibrary add(ScheduleTemplate schedule) {
        return add(UUID.randomUUID().toString(), schedule);
    }

    public TemplateLibrary add(String id, ScheduleTemplate schedule) {
        Objects.requireNonNull(schedule, "schedule");
        List<Entry> updated = new ArrayList<>(entries);
        updated.add(new Entry(id, schedule));
        return new TemplateLibrary(schemaVersion, id, updated);
    }

    /**
     * Removes an entry and selects the nearest remaining entry. A caller must
     * decide how to restore a default when removing the final item.
     */
    public TemplateLibrary remove(String id) {
        requireEntry(id);
        if (entries.size() == 1) {
            throw new IllegalStateException("The final template cannot be removed");
        }
        List<Entry> remaining = entries.stream()
                .filter(entry -> !entry.id().equals(id))
                .toList();
        String nextSelection = selectedTemplateId;
        if (selectedTemplateId.equals(id)) {
            int removedIndex = indexOf(id);
            int nextIndex = Math.min(removedIndex, remaining.size() - 1);
            nextSelection = remaining.get(nextIndex).id();
        }
        return new TemplateLibrary(schemaVersion, nextSelection, remaining);
    }

    private int indexOf(String id) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index).id().equals(id)) {
                return index;
            }
        }
        throw new IllegalArgumentException("Template does not exist: " + id);
    }

    private void requireEntry(String id) {
        if (entries.stream().noneMatch(entry -> entry.id().equals(id))) {
            throw new IllegalArgumentException("Template does not exist: " + id);
        }
    }
}
