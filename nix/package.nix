{ pkgs, lib }:

pkgs.stdenv.mkDerivation (finalAttrs: {
  pname = "livara-study-planner";
  version = "0.1.0";
  src = lib.cleanSource ../.;

  nativeBuildInputs = [
    pkgs.gradle
    pkgs.makeWrapper
  ];

  # Keep the JavaFX runtime in the application distribution. The dependency
  # cache is intentionally checked into this repository once generated on the
  # target nixpkgs revision; an empty cache is useful only for evaluation and
  # must not be treated as a complete offline build.
  mitmCache = pkgs.gradle.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };
  __darwinAllowLocalNetworking = true;

  gradleBuildTask = "installDist";
  gradleFlags = [
    "-Dorg.gradle.java.home=${pkgs.jdk21}"
    "-Dfile.encoding=UTF-8"
  ];
  doCheck = true;

  installPhase = ''
    runHook preInstall
    mkdir -p "$out/share/${finalAttrs.pname}" "$out/bin"
    cp -r "build/install/${finalAttrs.pname}/." "$out/share/${finalAttrs.pname}/"
    makeWrapper "${pkgs.jdk21}/bin/java" "$out/bin/${finalAttrs.pname}" \
      --add-flags "-cp $out/share/${finalAttrs.pname}/lib/* com.joaoferraz.livara.studyplanner.cli.Main"
    runHook postInstall
  '';

  meta = {
    description = "Offline JavaFX study schedule planner";
    homepage = "https://github.com/Joaoferraz-byte/livara-study-planner";
    license = lib.licenses.mit;
    mainProgram = finalAttrs.pname;
    platforms = lib.platforms.linux;
  };
})
