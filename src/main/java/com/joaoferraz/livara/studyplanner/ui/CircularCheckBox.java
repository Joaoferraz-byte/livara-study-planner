package com.joaoferraz.livara.studyplanner.ui;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

final class CircularCheckBox extends StackPane {
    private final CheckBox input = new CheckBox();
    private final StackPane visual = new StackPane();

    CircularCheckBox(String cssClass, double outerRadius, double subRadius, double coreRadius) {
        getStyleClass().addAll("circular-check", cssClass);
        setAlignment(Pos.CENTER);
        setPickOnBounds(false);
        setMinSize(outerRadius * 2, outerRadius * 2);
        setPrefSize(outerRadius * 2, outerRadius * 2);
        setMaxSize(outerRadius * 2, outerRadius * 2);

        Circle outer = circle("check-outer", outerRadius);
        Circle sub = circle("check-subcircle", subRadius);
        Circle core = circle("check-core", coreRadius);
        visual.getChildren().setAll(outer, sub, core);
        visual.setAlignment(Pos.CENTER);
        visual.setMouseTransparent(true);
        visual.setMinSize(outerRadius * 2, outerRadius * 2);
        visual.setPrefSize(outerRadius * 2, outerRadius * 2);
        visual.setMaxSize(outerRadius * 2, outerRadius * 2);

        input.setOpacity(0);
        input.setFocusTraversable(true);
        input.setMnemonicParsing(false);
        input.setMinSize(outerRadius * 2, outerRadius * 2);
        input.setPrefSize(outerRadius * 2, outerRadius * 2);
        input.setMaxSize(outerRadius * 2, outerRadius * 2);
        input.setMouseTransparent(false);
        input.selectedProperty().addListener((observable, oldValue, selected) ->
                pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), selected));

        getChildren().setAll(visual, input);
        StackPane.setAlignment(visual, Pos.CENTER);
        StackPane.setAlignment(input, Pos.CENTER);
    }

    private static Circle circle(String styleClass, double radius) {
        Circle circle = new Circle(radius);
        circle.getStyleClass().add(styleClass);
        return circle;
    }

    boolean isSelected() {
        return input.isSelected();
    }

    void setSelected(boolean selected) {
        input.setSelected(selected);
        pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), selected);
    }

    void setOnAction(EventHandler<ActionEvent> handler) {
        input.setOnAction(handler);
    }

    StackPane visual() {
        return visual;
    }
}
