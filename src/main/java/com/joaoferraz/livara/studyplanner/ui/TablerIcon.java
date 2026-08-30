package com.joaoferraz.livara.studyplanner.ui;

import javafx.scene.shape.SVGPath;

final class TablerIcon {
    private TablerIcon() {
    }

    static SVGPath home() {
        return icon("M3 10.5L12 3l9 7.5M5 10v10h14V10M9 20v-6h6v6");
    }

    static SVGPath folder() {
        return icon("M3 7h6l2 2h10v10H3z");
    }

    static SVGPath notes() {
        return icon("M6 3h12v18H6zM9 7h6M9 11h6M9 15h4");
    }

    static SVGPath project() {
        return icon("M4 4h16v16H4zM8 8h8M8 12h8M8 16h5");
    }

    static SVGPath calendar() {
        return icon("M5 4h14v16H5zM8 2v4M16 2v4M5 9h14");
    }

    static SVGPath palette() {
        return icon("M12 3a9 9 0 1 0 0 18h1.5a2 2 0 0 0 0-4H12a2 2 0 0 1 0-4h3a2 2 0 0 0 0-4H12zM7.5 10h.01M9.5 7h.01M14.5 7h.01");
    }

    static SVGPath book() {
        return icon("M4 4h6a2 2 0 0 1 2 2v14a2 2 0 0 0-2-2H4zM20 4h-6a2 2 0 0 0-2 2v14a2 2 0 0 1 2-2h6z");
    }

    private static SVGPath icon(String content) {
        SVGPath path = new SVGPath();
        path.setContent(content);
        path.getStyleClass().add("tabler-icon");
        return path;
    }
}
