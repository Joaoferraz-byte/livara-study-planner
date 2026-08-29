# Livara Study Planner

Livara Study Planner is an offline JavaFX application and command-line tool for maintaining a reusable, neuroscience-informed study schedule. It is deliberately a **template planner**, not a course that hard-codes a path to mastery: the same weekly structure can be reused while subjects and projects change.

The default template uses seven one-hour blocks with fifteen-minute pauses between adjacent blocks. Programming for the market and applied projects receive the highest weight, followed by logic/implementation, software architecture, software optimization, and alternating school subjects. Cycle A alternates Physics and Biology; cycle B alternates Chemistry and Mathematics. The cycle is editable and can be exported as JSON without requiring a network service.

## Design

The domain model is independent of JavaFX. `ScheduleTemplate` describes a reusable week and `ScheduleService` validates block durations, pause placement, and day assignments. `ScheduleStore` persists a small versioned JSON document using only the JDK, so the CLI and GUI share the same state. JavaFX is an adapter over those services and can be replaced later without rewriting the scheduling rules.

The source uses Java 21 and JavaFX 21.0.12. JavaFX 21 is pinned rather than using a dynamic version because JavaFX 26 requires a newer JDK and the surrounding Livara configuration already standardizes on JDK 21. See `nix/README.md` for the NixOS packaging workflow.

## CLI

After a Gradle distribution is built, run:

```bash
./build/install/livara-study-planner/bin/livara-study-planner --help
./build/install/livara-study-planner/bin/livara-study-planner template --output ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner show --file ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner validate --file ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner gui --file ~/.local/state/livara/study-schedule.json
```

When no file is supplied, the CLI uses `$XDG_STATE_HOME/livara/study-schedule.json`, falling back to `~/.local/state/livara/study-schedule.json`. The planner never writes to the Vault automatically; exporting a reviewed schedule to a note is an explicit user action.

## Development

```bash
gradle test
./gradlew run --args='show --file /tmp/study-schedule.json'
```

For NixOS, use the flake package/app described in `nix/README.md`. A real Nix build must first populate the Gradle dependency cache according to the current nixpkgs Gradle documentation; the repository includes an explicit check that prevents an accidentally network-dependent package expression from being mistaken for a fully vendored build.

## License

Personal configuration and utility software for the Livara environment.
