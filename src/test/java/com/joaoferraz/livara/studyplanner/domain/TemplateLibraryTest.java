package com.joaoferraz.livara.studyplanner.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TemplateLibraryTest {
    @Test
    void addingTemplateSelectsItAndKeepsItsIdentityAndSequenceTogether() {
        ScheduleTemplate original = DefaultScheduleFactory.create(Cycle.A);
        ScheduleTemplate draft = original.withIdentity("New study template", "x");

        TemplateLibrary library = TemplateLibrary.single("original", original).add("new", draft);

        assertEquals("new", library.selectedTemplateId());
        assertEquals("New study template", library.selected().name());
        assertEquals("x", library.selected().iconId());
        assertEquals(draft.sequence(), library.selected().sequence());
        assertEquals(List.of("original", "new"), library.entries().stream().map(TemplateLibrary.Entry::id).toList());
    }

    @Test
    void replacingSelectedTemplateDoesNotChangeSelection() {
        ScheduleTemplate original = DefaultScheduleFactory.create(Cycle.A);
        TemplateLibrary library = TemplateLibrary.single("active", original);
        ScheduleTemplate edited = original.withIdentity("Edited", "calendar");

        TemplateLibrary updated = library.updateSelected(edited);

        assertEquals("active", updated.selectedTemplateId());
        assertEquals("Edited", updated.selected().name());
        assertEquals("calendar", updated.selected().iconId());
    }

    @Test
    void removingSelectedTemplateSelectsTheNextNearestEntry() {
        ScheduleTemplate schedule = DefaultScheduleFactory.create(Cycle.A);
        TemplateLibrary library = TemplateLibrary.single("one", schedule)
                .add("two", schedule.withIdentity("Two", "x"))
                .add("three", schedule.withIdentity("Three", "calendar"));

        TemplateLibrary updated = library.select("two").remove("two");

        assertEquals("three", updated.selectedTemplateId());
        assertEquals(List.of("one", "three"), updated.entries().stream().map(TemplateLibrary.Entry::id).toList());
    }

    @Test
    void cannotRemoveTheFinalTemplate() {
        assertThrows(IllegalStateException.class,
                () -> TemplateLibrary.single(DefaultScheduleFactory.create(Cycle.A)).remove("default"));
    }
}
