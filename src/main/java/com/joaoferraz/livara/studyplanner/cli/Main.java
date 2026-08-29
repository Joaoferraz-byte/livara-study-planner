package com.joaoferraz.livara.studyplanner.cli;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.ScheduleService;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import com.joaoferraz.livara.studyplanner.ui.StudyPlannerApp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "gui" : args[0].toLowerCase();
        Path file = optionPath(args, "--file", defaultPath());
        ScheduleStore store = new ScheduleStore();
        ScheduleService service = new ScheduleService(store);

        switch (command) {
            case "help", "--help", "-h" -> printHelp();
            case "template" -> {
                Cycle cycle = optionCycle(args, Cycle.A);
                ScheduleTemplate template = DefaultScheduleFactory.create(cycle);
                service.save(file, template);
                System.out.println("Created " + file.toAbsolutePath());
            }
            case "show" -> show(store.load(file));
            case "validate" -> validate(service, store.load(file));
            case "next-cycle" -> {
                ScheduleTemplate next = service.advanceCycle(store.load(file));
                service.save(file, next);
                System.out.println("Advanced to " + next.cycle().label() + " in " + file.toAbsolutePath());
            }
            case "gui" -> StudyPlannerApp.launchWithPath(file);
            default -> {
                System.err.println("Unknown command: " + command);
                printHelp();
                System.exit(2);
            }
        }
    }

    private static void validate(ScheduleService service, ScheduleTemplate template) {
        List<String> errors = service.validate(template);
        if (errors.isEmpty()) {
            System.out.println("Valid: " + template.name() + " (" + template.cycle().label() + ")");
            System.out.println("Blocks: " + template.totalBlocks() + ", pause: " + template.pauseMinutes() + " minutes");
        } else {
            errors.forEach(error -> System.err.println("ERROR: " + error));
            System.exit(1);
        }
    }

    private static void show(ScheduleTemplate template) {
        System.out.printf("%s — %s — %d blocks%n", template.name(), template.cycle().subjects(), template.totalBlocks());
        for (DayOfWeek day : DayOfWeek.values()) {
            String blocks = template.blocks(day).stream()
                    .map(block -> block.order() + ". " + block.topic() + " (60 min)" +
                            (block.breakAfterMinutes() > 0 ? " + " + block.breakAfterMinutes() + " min pause" : ""))
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("—");
            System.out.printf("%-9s %s%n", day, blocks);
        }
    }

    private static void printHelp() {
        System.out.println("Livara Study Planner");
        System.out.println("Usage: livara-study-planner [command] [options]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  gui                 Open the JavaFX planner (default)");
        System.out.println("  template            Create a reusable weekly template");
        System.out.println("  show                Print the current schedule");
        System.out.println("  validate            Validate the schedule contract");
        System.out.println("  next-cycle          Replace the schedule with the next A/B cycle");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --file PATH         Schedule JSON path");
        System.out.println("  --cycle A|B         Cycle for template creation");
    }

    private static Path optionPath(String[] args, String option, Path fallback) {
        for (int index = 0; index + 1 < args.length; index++) {
            if (option.equals(args[index])) {
                return Paths.get(args[index + 1]);
            }
        }
        return fallback;
    }

    private static Cycle optionCycle(String[] args, Cycle fallback) {
        for (int index = 0; index + 1 < args.length; index++) {
            if ("--cycle".equals(args[index])) {
                return Cycle.valueOf(args[index + 1].toUpperCase());
            }
        }
        return fallback;
    }

    private static Path defaultPath() {
        String stateHome = System.getenv("XDG_STATE_HOME");
        if (stateHome == null || stateHome.isBlank()) {
            stateHome = System.getProperty("user.home") + "/.local/state";
        }
        return Paths.get(stateHome, "livara", "study-schedule.json");
    }
}
