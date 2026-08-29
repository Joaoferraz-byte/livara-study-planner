package com.joaoferraz.livara.studyplanner.domain;

import com.joaoferraz.livara.studyplanner.io.ScheduleStore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ScheduleService {
    private final ScheduleStore store;

    public ScheduleService(ScheduleStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public ScheduleTemplate loadOrCreate(Path path) throws IOException {
        if (!path.toFile().exists()) {
            ScheduleTemplate template = DefaultScheduleFactory.create(Cycle.A);
            save(path, template);
            return template;
        }
        ScheduleTemplate template = store.load(path);
        requireValid(template);
        return template;
    }

    public void save(Path path, ScheduleTemplate template) throws IOException {
        requireValid(template);
        store.save(path, template);
    }

    public List<String> validate(ScheduleTemplate template) {
        return ScheduleValidator.validate(template);
    }

    public ScheduleTemplate advanceCycle(ScheduleTemplate template) {
        requireValid(template);
        return DefaultScheduleFactory.create(template.cycle().next());
    }

    private void requireValid(ScheduleTemplate template) {
        ScheduleValidator.requireValid(template);
    }
}
