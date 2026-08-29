# Livara Study Planner

Livara Study Planner is an offline JavaFX application and command-line tool for maintaining a reusable, evidence-aware study workflow. It is deliberately a **template planner**, not a course that hard-codes a path to mastery: the same sequence can be reused while subjects and projects change.

The default workflow contains eight one-hour focus blocks with fifteen-minute pauses between adjacent blocks. Programming for the market and applied projects receive the highest repetition, followed by logic/implementation, software architecture, software optimization, and alternating school subjects. Cycle A alternates Physics and Biology; cycle B alternates Chemistry and Mathematics. The spacing is configurable in the domain contract rather than presented as a rigid neuroscience prescription.

## Dashboard

The JavaFX screen is organized around the next session, not weekdays. Each focus block and pause is rendered as a card with a checkbox, semantic icon, duration, and active-state highlight. Completing a focus or pause is persisted automatically in a sidecar file next to the schedule. Completing all focus blocks advances from cycle A to B or from B to A automatically and resets only execution progress; the reusable workflow template remains selected.

The workflow selector offers market programming, applied projects, logic and implementation, software architecture, software optimization, and school subjects. The menu contains secondary actions such as validation, reload, manual cycle selection, and progress reset. The Vault panel provides a desktop shortcut to open the Vault in Oil through footclient, WezTerm, or a direct Neovim fallback and lists the main Vault areas with their semantic folder icons.

## Design

The domain model is independent of JavaFX. `ScheduleTemplate` describes the reusable schedule and selected `WorkflowTemplate`; `ScheduleService` validates block durations, pause placement, and focus coverage. `ScheduleStore` persists a versioned JSON document, accepting legacy documents without `workflowTemplate` and writing the new field on save. `ProgressState` and `ProgressStore` keep temporary completion state separate from the schedule, so UI execution state does not pollute the reusable template. JavaFX is an adapter over those services and can be replaced later without rewriting the scheduling rules.

The source uses Java 21 and JavaFX 21.0.12. JavaFX 21 is pinned rather than using a dynamic version because JavaFX 26 requires a newer JDK and the surrounding Livara configuration already standardizes on JDK 21. See `nix/README.md` for the NixOS packaging workflow.

## CLI

After a Gradle distribution is built, run:

```bash
./build/install/livara-study-planner/bin/livara-study-planner --help
./build/install/livara-study-planner/bin/livara-study-planner template --file ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner show --file ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner validate --file ~/.local/state/livara/study-schedule.json
./build/install/livara-study-planner/bin/livara-study-planner gui --file ~/.local/state/livara/study-schedule.json
```

When no file is supplied, the CLI uses `$XDG_STATE_HOME/livara/study-schedule.json`, falling back to `~/.local/state/livara/study-schedule.json`. The GUI stores execution progress in a sibling `.progress.properties` file. The planner never writes notes into the Vault automatically; opening the Vault is an explicit desktop action.

## Development

```bash
gradle test
./gradlew run --args='show --file /tmp/study-schedule.json'
```

For NixOS, use the flake package/app described in `nix/README.md`. A real Nix build must first populate the Gradle dependency cache according to the current nixpkgs Gradle documentation; the repository includes an explicit check that prevents an accidentally network-dependent package expression from being mistaken for a fully vendored build.

## License

Personal configuration and utility software for the Livara environment.
