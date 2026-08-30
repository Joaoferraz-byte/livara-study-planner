package com.joaoferraz.livara.studyplanner.ui;

import javafx.scene.shape.SVGPath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TablerIcon {
    private static final Pattern PATH = Pattern.compile("<path\\s+[^>]*d=\\\"([^\\\"]+)\\\"[^>]*/?>");

    private TablerIcon() {
    }

    static SVGPath home() {
        return icon("home");
    }

    static SVGPath folder() {
        return icon("folder");
    }

    static SVGPath notes() {
        return icon("notebook");
    }

    static SVGPath project() {
        return icon("layout-dashboard");
    }

    static SVGPath calendar() {
        return icon("calendar");
    }

    static SVGPath palette() {
        return icon("palette");
    }

    static SVGPath book() {
        return icon("book-2");
    }

    static SVGPath archive() {
        return icon("archive");
    }

    static SVGPath bookmark() {
        return icon("bookmark");
    }

    static SVGPath settings() {
        return icon("settings");
    }

    private static SVGPath icon(String name) {
        String resource = "/icons/tabler/" + name + ".svg";
        try (InputStream stream = TablerIcon.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing Tabler icon resource: " + resource);
            }
            String svg = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Matcher matcher = PATH.matcher(svg);
            StringBuilder pathData = new StringBuilder();
            while (matcher.find()) {
                if (pathData.length() > 0) {
                    pathData.append(' ');
                }
                pathData.append(matcher.group(1));
            }
            if (pathData.length() == 0) {
                throw new IllegalStateException("Tabler icon has no path data: " + resource);
            }
            SVGPath path = new SVGPath();
            path.setContent(pathData.toString());
            path.getStyleClass().add("tabler-icon");
            return path;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load Tabler icon: " + resource, exception);
        }
    }
}
