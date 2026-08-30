package com.joaoferraz.livara.studyplanner.io;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.TemplateLibrary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateLibraryStoreTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsMultipleTemplatesWithTheSelectedEntry() throws Exception {
        ScheduleTemplate original = DefaultScheduleFactory.create(Cycle.A);
        ScheduleTemplate edited = original.withIdentity("Novo template · Estudo", "x");
        TemplateLibrary library = TemplateLibrary.single("original", original)
                .add("novo", edited);
        TemplateLibraryStore store = new TemplateLibraryStore();
        Path file = temporaryDirectory.resolve("nested/templates.json");

        store.save(file, library);
        TemplateLibrary loaded = store.load(file);

        assertEquals("novo", loaded.selectedTemplateId());
        assertEquals(List.of("original", "novo"), loaded.entries().stream()
                .map(TemplateLibrary.Entry::id).toList());
        assertEquals("Novo template · Estudo", loaded.selected().name());
        assertEquals(edited.sequence(), loaded.selected().sequence());
        assertTrue(Files.readString(file).contains("selectedTemplateId"));
        assertTrue(Files.readString(file).contains("Novo template · Estudo"));
    }

    @Test
    void migratesTheLegacyDefaultScheduleToIncludeCycleB() throws Exception {
        String legacy = """
                {
                  "schemaVersion": 1,
                  "name": "Market Programming",
                  "cycle": "A",
                  "pauseMinutes": 15,
                  "days": {
                    "MONDAY": [
                      {
                        "order": 1,
                        "focus": "market-programming",
                        "topic": "Legacy focus",
                        "durationMinutes": 60,
                        "breakAfterMinutes": 15
                      }
                    ]
                  }
                }
                """;
        TemplateLibrary loaded = new TemplateLibraryStore().fromJson(legacy);

        assertEquals(List.of(Cycle.A, Cycle.B), loaded.selected().createdCycles());
        assertEquals("Legacy focus", loaded.selected().blocks(Cycle.A)
                .get(java.time.DayOfWeek.MONDAY).getFirst().topic());
        assertTrue(loaded.selected().totalBlocks(Cycle.B) > 0);
    }

    @Test
    void keepsLegacyCustomSingleCycleSchedulesUnchanged() throws Exception {
        String legacy = """
                {
                  "schemaVersion": 1,
                  "name": "Personal one-cycle plan",
                  "cycle": "A",
                  "pauseMinutes": 15,
                  "days": {}
                }
                """;
        TemplateLibrary loaded = new TemplateLibraryStore().fromJson(legacy);

        assertEquals(List.of(Cycle.A), loaded.selected().createdCycles());
    }

    @Test
    void wrapsLegacySingleScheduleDocumentsWithoutChangingTheirData() throws Exception {
        ScheduleStore scheduleStore = new ScheduleStore();
        ScheduleTemplate original = DefaultScheduleFactory.create(Cycle.B)
                .withIdentity("Agenda legada", "calendar");
        TemplateLibraryStore store = new TemplateLibraryStore();
        Path file = temporaryDirectory.resolve("legacy.json");
        Files.writeString(file, scheduleStore.toJson(original));

        TemplateLibrary loaded = store.load(file);

        assertEquals("default", loaded.selectedTemplateId());
        assertEquals("Agenda legada", loaded.selected().name());
        assertEquals(Cycle.B, loaded.selected().cycle());
        assertEquals(original.blocksByCycle(), loaded.selected().blocksByCycle());
    }
}
