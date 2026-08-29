package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.ProgressState;
import com.joaoferraz.livara.studyplanner.domain.WorkflowTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class ProgressStore {
    public ProgressState loadOrEmpty(Path path, Cycle cycle, WorkflowTemplate workflowTemplate) throws IOException {
        if (!Files.exists(path)) {
            return ProgressState.empty(cycle, workflowTemplate);
        }
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        if (!cycle.name().equals(properties.getProperty("cycle"))
                || !workflowTemplate.id().equals(properties.getProperty("workflowTemplate"))) {
            return ProgressState.empty(cycle, workflowTemplate);
        }
        String completed = properties.getProperty("completedItems", "");
        Set<String> completedItems = Arrays.stream(completed.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        int activeItemIndex;
        try {
            activeItemIndex = Integer.parseInt(properties.getProperty("activeItemIndex", "0"));
        } catch (NumberFormatException exception) {
            activeItemIndex = 0;
        }
        return new ProgressState(1, cycle, workflowTemplate, completedItems, activeItemIndex);
    }

    public void save(Path path, ProgressState state) throws IOException {
        Path absolute = path.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Properties properties = new Properties();
        properties.setProperty("schemaVersion", Integer.toString(state.schemaVersion()));
        properties.setProperty("cycle", state.cycle().name());
        properties.setProperty("workflowTemplate", state.workflowTemplate().id());
        properties.setProperty("activeItemIndex", Integer.toString(state.activeItemIndex()));
        properties.setProperty("completedItems", String.join(",", state.completedItems()));
        Path temporary = absolute.resolveSibling(absolute.getFileName() + ".tmp");
        try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
            properties.store(writer, "Livara Study Planner execution state");
        }
        Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
