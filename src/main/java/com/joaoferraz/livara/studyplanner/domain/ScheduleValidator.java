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
        if (template.totalBlocks() == 0) {
            errors.add("The template must contain at least one study block");
        }

        Set<FocusArea> present = EnumSet.noneOf(FocusArea.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            int expectedOrder = 1;
            for (StudyBlock block : template.blocks(day)) {
                if (!block.isStandardHour() && !block.duration().equals(Duration.ofMinutes(60))) {
                    errors.add(day + " block " + block.order() + " is not exactly 60 minutes");
                }
                if (block.order() != expectedOrder++) {
                    errors.add(day + " blocks must have contiguous order numbers");
                }
                if (block.breakAfterMinutes() != 0 && block.breakAfterMinutes() != template.pauseMinutes()) {
                    errors.add(day + " block " + block.order() + " has an invalid break duration");
                }
                present.add(block.focus());
            }
        }

        if (!present.contains(FocusArea.MARKET_PROGRAMMING)) {
            errors.add("Market programming focus is missing");
        }
        if (!present.contains(FocusArea.PROJECTS)) {
            errors.add("Applied projects focus is missing");
        }
        if (!present.contains(FocusArea.LOGIC)) {
            errors.add("Logic focus is missing");
        }
        if (!present.contains(FocusArea.ARCHITECTURE)) {
            errors.add("Architecture focus is missing");
        }
        if (!present.contains(FocusArea.OPTIMIZATION)) {
            errors.add("Optimization focus is missing");
        }
        if (template.cycle() == Cycle.A) {
            if (!present.contains(FocusArea.PHYSICS) || !present.contains(FocusArea.BIOLOGY)) {
                errors.add("Cycle A must include Physics and Biology");
            }
        } else if (!present.contains(FocusArea.CHEMISTRY) || !present.contains(FocusArea.MATHEMATICS)) {
            errors.add("Cycle B must include Chemistry and Mathematics");
        }
        return List.copyOf(errors);
    }

    public static void requireValid(ScheduleTemplate template) {
        List<String> errors = validate(template);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
    }
}
