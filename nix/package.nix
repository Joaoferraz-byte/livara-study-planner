{ pkgs, lib }:

let
  # JavaFX Maven artifacts extract native libraries to ~/.openjfx/cache.
  # These native libraries have no Nix RPATH, so expose their NixOS runtime
  # dependencies only to this application process.
  x11 = name: legacy: pkgs.${name} or pkgs.xorg.${legacy};
  javafxRuntimeLibs = with pkgs; [
    gtk3
    glib
    pango
    cairo
    gdk-pixbuf
    atk
    freetype
    fontconfig
    libGL
  ] ++ [
    (x11 "libx11" "libX11")
    (x11 "libxtst" "libXtst")
    (x11 "libxxf86vm" "libXxf86vm")
    (x11 "libxrender" "libXrender")
    (x11 "libxext" "libXext")
  ];
in
pkgs.stdenv.mkDerivation (finalAttrs: {
  pname = "livara-study-planner";
  version = "0.1.0";
  src = lib.cleanSource ../.;

  nativeBuildInputs = [
    pkgs.gradle
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
    cat > "$out/bin/${finalAttrs.pname}" <<EOF
#!${pkgs.runtimeShell}
export LD_LIBRARY_PATH="${lib.makeLibraryPath javafxRuntimeLibs}''${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
exec ${pkgs.jdk21}/bin/java -cp "$out/share/${finalAttrs.pname}/lib/*" com.joaoferraz.livara.studyplanner.cli.Main "\$@"
EOF
    chmod +x "$out/bin/${finalAttrs.pname}"
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
