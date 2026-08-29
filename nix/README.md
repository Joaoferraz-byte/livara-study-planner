# NixOS integration

The planner exposes a package and app from `nix/flake.nix`. The package builds the Gradle `installDist` output, wraps it with JDK 21, and installs the CLI entrypoint. JavaFX 21 is carried by the Gradle distribution and `openjfx21` is available in the development shell.

## Local development

```bash
nix develop .#default
./gradlew test
```

## Reproducible package dependencies

The package uses the nixpkgs Gradle setup hook and a checked-in `deps.json`. On a NixOS machine with the exact target nixpkgs revision, initialize or refresh the cache using the generated update script:

```bash
nix-build -A default.mitmCache.updateScript
```

Then review the resulting `deps.json`, commit it, and build with:

```bash
nix build .#default
nix run .#default -- template --file /tmp/study-schedule.json
```

The initial repository includes an empty cache marker so the expression remains easy to audit. It is not a substitute for running the update script: an offline Nix build must contain the resolved Maven artifacts and their hashes. This distinction avoids silently shipping a package that depends on network access during a system rebuild.
