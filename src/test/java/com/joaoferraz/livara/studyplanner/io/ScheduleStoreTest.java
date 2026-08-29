package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsTheScheduleWithoutLosingUnicodeOrCycleData() throws Exception {
        ScheduleStore store = new ScheduleStore();
        ScheduleTemplate original = DefaultScheduleFactory.create(Cycle.B);
        Path file = temporaryDirectory.resolve("nested/schedule.json");

        store.save(file, original);
        ScheduleTemplate loaded = store.load(file);

        assertEquals(original.name(), loaded.name());
        assertEquals(original.cycle(), loaded.cycle());
        assertEquals(original.pauseMinutes(), loaded.pauseMinutes());
        assertEquals(original.totalBlocks(), loaded.totalBlocks());
        assertEquals(original.blocksByDay(), loaded.blocksByDay());
        assertTrue(store.toJson(loaded).contains("Chemistry"));
    }
}
