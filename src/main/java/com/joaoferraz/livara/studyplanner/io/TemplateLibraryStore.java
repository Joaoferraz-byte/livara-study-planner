package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.TemplateLibrary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Persists the template library atomically and migrates the former single schedule document. */
public final class TemplateLibraryStore {
    private final ScheduleStore scheduleStore = new ScheduleStore();

    public TemplateLibrary load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        String json = Files.readString(path, StandardCharsets.UTF_8);
        Object parsed = ScheduleStore.parseJson(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Template library document root must be a JSON object");
        }
        if (!(root.get("templates") instanceof List<?>)) {
            return TemplateLibrary.single("default", migrateLegacyDefault(scheduleStore.fromJson(json)));
        }
        return fromRoot(root);
    }

    public TemplateLibrary fromJson(String json) throws IOException {
        Objects.requireNonNull(json, "json");
        Object parsed = ScheduleStore.parseJson(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IOException("Template library document root must be a JSON object");
        }
        if (!(root.get("templates") instanceof List<?>)) {
            return TemplateLibrary.single("default", migrateLegacyDefault(scheduleStore.fromJson(json)));
        }
        return fromRoot(root);
    }

    /**
     * The first multi-cycle release can read the former single-schedule format,
     * but that format had no way to persist cycle B. Only the seeded default
     * template is enriched here; custom templates remain exactly as authored.
     */
    private ScheduleTemplate migrateLegacyDefault(ScheduleTemplate schedule) {
        if (schedule.schemaVersion() < 2
                && schedule.cycle() == Cycle.A
                && schedule.name().equalsIgnoreCase(schedule.workflowTemplate().label())
                && !schedule.hasCycle(Cycle.B)) {
            return schedule.withAddedCycle(Cycle.B,
                    DefaultScheduleFactory.create(Cycle.B, schedule.workflowTemplate()).blocks(Cycle.B));
        }
        return schedule;
    }

    private ScheduleTemplate migrateDefaultCycle(String id, ScheduleTemplate schedule) {
        if (!"default".equals(id)
                || !schedule.hasCycle(Cycle.B)
                || schedule.totalBlocks(Cycle.B) > 0) {
            return schedule;
        }
        return schedule.withUpdatedCycle(Cycle.B,
                DefaultScheduleFactory.create(Cycle.B, schedule.workflowTemplate()).blocks(Cycle.B));
    }

    private TemplateLibrary fromRoot(Map<?, ?> root) throws IOException {
        int schemaVersion = integer(root, "schemaVersion");
        String selectedId = string(root, "selectedTemplateId");
        Object templatesValue = root.get("templates");
        if (!(templatesValue instanceof List<?> rawEntries) || rawEntries.isEmpty()) {
            throw new IOException("Template library must contain templates");
        }
        List<TemplateLibrary.Entry> entries = new ArrayList<>();
        for (Object rawEntry : rawEntries) {
            if (!(rawEntry instanceof Map<?, ?> entry)) {
                throw new IOException("Each template entry must be a JSON object");
            }
            String id = string(entry, "id");
            Object schedule = entry.get("schedule");
            if (!(schedule instanceof Map<?, ?>)) {
                throw new IOException("Template entry must contain a schedule object: " + id);
            }
            ScheduleTemplate loaded = scheduleStore.fromJson(writeJson(schedule));
            entries.add(new TemplateLibrary.Entry(id, migrateDefaultCycle(id, loaded)));
        }
        try {
            return new TemplateLibrary(schemaVersion, selectedId, entries);
        } catch (RuntimeException exception) {
            throw new IOException("Invalid template library: " + exception.getMessage(), exception);
        }
    }

    public void save(Path path, TemplateLibrary library) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(library, "library");
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        Files.writeString(temporary, toJson(library) + System.lineSeparator(), StandardCharsets.UTF_8);
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public String toJson(TemplateLibrary library) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"schemaVersion\": ").append(library.schemaVersion()).append(",\n");
        json.append("  \"selectedTemplateId\": ")
                .append(ScheduleStore.quote(library.selectedTemplateId())).append(",\n");
        json.append("  \"templates\": [\n");
        for (int index = 0; index < library.entries().size(); index++) {
            TemplateLibrary.Entry entry = library.entries().get(index);
            json.append("    {\n");
            json.append("      \"id\": ").append(ScheduleStore.quote(entry.id())).append(",\n");
            String schedule = indent(ScheduleStore.toJsonStatic(entry.schedule()), 6);
            json.append("      \"schedule\": ").append(schedule).append("\n");
            json.append("    }").append(index + 1 == library.entries().size() ? "\n" : ",\n");
        }
        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    private static String indent(String value, int spaces) {
        String prefix = " ".repeat(spaces);
        return value.replace("\n", "\n" + prefix);
    }

    private static String writeJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return ScheduleStore.quote(string);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append(ScheduleStore.quote(String.valueOf(entry.getKey())))
                        .append(':').append(writeJson(entry.getValue()));
            }
            return json.append('}').toString();
        }
        if (value instanceof List<?> list) {
            StringBuilder json = new StringBuilder("[");
            for (int index = 0; index < list.size(); index++) {
                if (index > 0) {
                    json.append(',');
                }
                json.append(writeJson(list.get(index)));
            }
            return json.append(']').toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private static String string(Map<?, ?> object, String key) throws IOException {
        Object value = object.get(key);
        if (value instanceof String string && !string.isBlank()) {
            return string;
        }
        throw new IOException("Missing string field: " + key);
    }

    private static int integer(Map<?, ?> object, String key) throws IOException {
        Object value = object.get(key);
        if (!(value instanceof Number number) || number.longValue() != number.doubleValue()) {
            throw new IOException("Missing integer field: " + key);
        }
        return Math.toIntExact(number.longValue());
    }
}
