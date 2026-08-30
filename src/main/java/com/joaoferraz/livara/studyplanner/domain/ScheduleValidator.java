package com.joaoferraz.livara.studyplanner.domain;

import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ScheduleValidator {
    private ScheduleValidator() {
    }

    public static List<String> validate(ScheduleTemplate template) {
        List<String> errors = new ArrayList<>();
        if (template.pauseMinutes() != 15) {
            errors.add("The default study contract requires a 15-minute pause between blocks");
        }
        if (template.createdCycles().isEmpty()) {
            errors.add("The template must contain at least one study cycle");
            return List.copyOf(errors);
        }
        for (Cycle cycle : template.createdCycles()) {
            validateCycle(template, cycle, errors, cycle == template.cycle());
        }
        return List.copyOf(errors);
    }

    private static void validateCycle(ScheduleTemplate template, Cycle cycle,
                                      List<String> errors, boolean active) {
        String prefix = active ? "" : "Cycle " + cycle.label() + ": ";
        if (template.totalBlocks(cycle) == 0) {
            errors.add(prefix + "The cycle must contain at least one study block");
            return;
        }

        Set<FocusArea> present = EnumSet.noneOf(FocusArea.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            int expectedOrder = 1;
            for (StudyBlock block : template.blocks(cycle, day)) {
                if (!block.isStandardHour() && !block.duration().equals(Duration.ofMinutes(60))) {
                    errors.add(prefix + day + " block " + block.order() + " is not exactly 60 minutes");
                }
                if (block.order() != expectedOrder++) {
                    errors.add(prefix + day + " blocks must have contiguous order numbers");
                }
                if (block.breakAfterMinutes() != 0 && block.breakAfterMinutes() != template.pauseMinutes()) {
                    errors.add(prefix + day + " block " + block.order() + " has an invalid break duration");
                }
                present.add(block.focus());
            }
        }

        requireFocus(present, FocusArea.MARKET_PROGRAMMING,
                prefix + "Market programming focus is missing", errors);
        requireFocus(present, FocusArea.PROJECTS,
                prefix + "Applied projects focus is missing", errors);
        requireFocus(present, FocusArea.LOGIC,
                prefix + "Logic focus is missing", errors);
        requireFocus(present, FocusArea.ARCHITECTURE,
                prefix + "Architecture focus is missing", errors);
        requireFocus(present, FocusArea.OPTIMIZATION,
                prefix + "Optimization focus is missing", errors);
        if (cycle == Cycle.A) {
            requireFocus(present, FocusArea.PHYSICS,
                    prefix + "Cycle A must include Physics and Biology", errors);
            requireFocus(present, FocusArea.BIOLOGY,
                    prefix + "Cycle A must include Physics and Biology", errors);
        } else {
            requireFocus(present, FocusArea.CHEMISTRY,
                    prefix + "Cycle B must include Chemistry and Mathematics", errors);
            requireFocus(present, FocusArea.MATHEMATICS,
                    prefix + "Cycle B must include Chemistry and Mathematics", errors);
        }
    }

    private static void requireFocus(Set<FocusArea> present, FocusArea focus,
                                     String message, List<String> errors) {
        if (!present.contains(focus) && !errors.contains(message)) {
            errors.add(message);
        }
    }

    public static void requireValid(ScheduleTemplate template) {
        List<String> errors = validate(template);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }
}
