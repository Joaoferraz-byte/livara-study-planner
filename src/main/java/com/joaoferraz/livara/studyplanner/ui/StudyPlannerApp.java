package com.joaoferraz.livara.studyplanner.ui;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.FocusArea;
import com.joaoferraz.livara.studyplanner.domain.ProgressState;
import com.joaoferraz.livara.studyplanner.domain.ScheduleService;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.StudyBlock;
import com.joaoferraz.livara.studyplanner.domain.WorkflowTemplate;
import com.joaoferraz.livara.studyplanner.io.ProgressStore;
import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import com.joaoferraz.livara.studyplanner.io.TemplateLibraryStore;
import com.joaoferraz.livara.studyplanner.domain.TemplateLibrary;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudyPlannerApp extends Application {
    private static final Pattern COLOR = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"(#[0-9a-fA-F]{6})\\\"");
    private static final double WIDE_BREAKPOINT = 1040;
    private static final double PROGRESS_RING_SIZE = 112;
    private static final double PROGRESS_RING_RADIUS = 49;
    private static final double PROGRESS_RING_CIRCUMFERENCE = 2 * Math.PI * PROGRESS_RING_RADIUS;
    private static final List<String> VAULT_FOLDERS = List.of(
            "Black Box", "Source Notes", "Projects", "Daily Notes", "Xournal++", "References");
    private static final List<String> TEMPLATE_ICON_IDS = List.of(
            "x", "layout-dashboard", "calendar", "notebook", "book-2", "bookmark", "folder", "palette", "archive");

    private static Path requestedPath;

    private final ScheduleStore store = new ScheduleStore();
    private final ScheduleService service = new ScheduleService(store);
    private final TemplateLibraryStore libraryStore = new TemplateLibraryStore();
    private final ProgressStore progressStore = new ProgressStore();
    private final VBox sessionList = new VBox(8);
    private final GridPane dashboardGrid = new GridPane();
    private final Label cycleLabel = new Label();
    private final Label progressLabel = new Label();
    private final Label completionSummaryValue = new Label();
    private final Label statusLabel = new Label();
    private final Label focusDescription = new Label();
    private final Label flowSummary = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label activeMetric = new Label();
    private final Label remainingMetric = new Label();
    private final Label rhythmMetric = new Label();
    private final Label rotationValue = new Label();
    private final Label completedCyclesValue = new Label();
    private final VBox sideColumn = new VBox(14);
    private final VBox vaultPanel = new VBox(12);
    private final VBox summaryPanel = new VBox(10);
    private final VBox hero = new VBox(8);
    private final StackPane progressRing = new StackPane();
    private final Circle progressArc = new Circle(PROGRESS_RING_RADIUS);
    private final Label progressRingValue = new Label();
    private final MenuButton menu = new MenuButton("Menu");
    private final StackPane mainViewHost = new StackPane();
    private VBox dashboardPage;
    private Node menuIcon;
    private Node manageIcon;
    private Stage primaryStage;
    private boolean compactLayout;
    private ScheduleTemplate current;
    private TemplateLibrary library;
    private TemplateLibrary lastSavedLibrary;

    private static final class BlockEditorRow {
        private int order;
        private final ComboBox<FocusArea> focus;
        private final TextField topic;
        private final Spinner<Integer> duration;
        private final Spinner<Integer> pause;
        private final VBox row;
        private final VBox properties;
        private final Label focusSummary;
        private final Label topicSummary;
        private final Label timingSummary;
        private boolean expanded;
        private Runnable editAction = () -> { };

        private BlockEditorRow(int order, StudyBlock block, Runnable removeAction) {
            this.order = order;
            this.focus = new ComboBox<>();
            this.focus.getItems().addAll(FocusArea.values());
            this.focus.setConverter(new StringConverter<>() {
                @Override public String toString(FocusArea value) { return value == null ? "" : value.label(); }
                @Override public FocusArea fromString(String value) { return value == null ? null : FocusArea.fromId(value); }
            });
            this.focus.setValue(block.focus());
            this.topic = new TextField(block.topic());
            this.duration = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 180,
                    (int) block.duration().toMinutes(), 15));
            this.pause = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 60,
                    block.breakAfterMinutes(), 15));
            Label dayLabel = new Label(order + ".");
            dayLabel.getStyleClass().add("manager-order-label");
            focusSummary = new Label();
            focusSummary.getStyleClass().add("block-card-focus");
            topicSummary = new Label();
            topicSummary.getStyleClass().add("block-card-topic");
            timingSummary = new Label();
            timingSummary.getStyleClass().add("block-card-timing");
            VBox summary = new VBox(3, focusSummary, topicSummary);
            HBox.setHgrow(summary, Priority.ALWAYS);
            Button edit = new Button("Edit");
            edit.getStyleClass().add("secondary-button");
            edit.setOnAction(event -> editAction.run());
            Button remove = new Button("Remove");
            remove.getStyleClass().add("danger-button");
            remove.setOnAction(event -> removeAction.run());
            StackPane orderSlot = new StackPane(dayLabel);
            orderSlot.setMinWidth(18);
            orderSlot.setPrefWidth(18);
            orderSlot.setMaxWidth(18);
            orderSlot.setAlignment(Pos.TOP_LEFT);
            HBox identity = new HBox(6, orderSlot, summary, timingSummary, edit, remove);
            identity.setAlignment(Pos.CENTER_LEFT);
            properties = new VBox(5);
            properties.getStyleClass().add("block-card-details");
            properties.setVisible(false);
            properties.setManaged(false);
            row = new VBox(7, identity, properties);
            row.getStyleClass().add("block-editor-row");
            row.setOnMouseClicked(event -> {
                if (!(event.getTarget() instanceof Button)) {
                    toggleProperties();
                }
            });
            refreshSummary();
        }

        private void setEditAction(Runnable action) {
            editAction = action;
        }

        private void setOrder(int value) {
            order = value;
        }

        private void refreshSummary() {
            focusSummary.setText(focus.getValue() == null ? "Choose a focus" : focus.getValue().label());
            topicSummary.setText(topic.getText());
            timingSummary.setText(duration.getValue() + " min  ·  " + pause.getValue() + " min pause");
            properties.getChildren().setAll(new Label("Focus: " + focusSummary.getText()),
                    new Label("Topic: " + topicSummary.getText()),
                    new Label("Timing: " + timingSummary.getText()));
            properties.getChildren().forEach(node -> node.getStyleClass().add("block-card-detail"));
        }

        private void toggleProperties() {
            expanded = !expanded;
            if (expanded) {
                properties.setManaged(true);
                properties.setVisible(true);
                properties.setOpacity(0);
                FadeTransition fade = new FadeTransition(Duration.millis(170), properties);
                fade.setToValue(1);
                fade.play();
            } else {
                FadeTransition fade = new FadeTransition(Duration.millis(130), properties);
                fade.setToValue(0);
                fade.setOnFinished(event -> {
                    properties.setVisible(false);
                    properties.setManaged(false);
                    properties.setOpacity(1);
                });
                fade.play();
            }
            row.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("expanded"), expanded);
        }

        private StudyBlock toBlock(int order) {
            return new StudyBlock(order, focus.getValue(), topic.getText(),
                    java.time.Duration.ofMinutes(duration.getValue()), pause.getValue());
        }
    }
    private ProgressState progress;
    private List<StudySessionItem> sessionItems = List.of();
    private String expandedItemId;
    private boolean animateNextExpansion;
    private boolean animateSessionRefresh;
    private Path schedulePath;
    private Path progressPath;
    private boolean usingLegacyProgressPath;

    public static void launchWithPath(Path path) {
        requestedPath = Objects.requireNonNull(path, "path").toAbsolutePath();
        launch();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        schedulePath = requestedPath == null ? defaultPath() : requestedPath;
        progressPath = schedulePath.resolveSibling(schedulePath.getFileName() + ".progress.properties");
        try {
            if (Files.exists(schedulePath)) {
                library = libraryStore.load(schedulePath);
            } else {
                library = TemplateLibrary.single("default",
                        DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING));
            }
            current = library.selected();
            List<String> errors = service.validate(current);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException(String.join("\\n", errors));
            }
            usingLegacyProgressPath = isLegacyDefaultLibrary();
            activateSelectedTemplate();
            lastSavedLibrary = library;
            if (!Files.exists(schedulePath)) {
                persistSchedule();
            }
        } catch (IOException | RuntimeException exception) {
            showError("Unable to load schedule", exception.getMessage());
            library = TemplateLibrary.single("default",
                    DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING));
            current = library.selected();
            usingLegacyProgressPath = true;
            activateSelectedTemplate();
            lastSavedLibrary = library;
        }

        StackPane sceneRoot = new StackPane();
        sceneRoot.getStyleClass().add("root");
        applyMatugenPalette(sceneRoot);
        Pane ambient = buildAmbientLayer();
        BorderPane contentRoot = new BorderPane();
        contentRoot.setPadding(new Insets(22, 26, 16, 26));
        contentRoot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(contentRoot, Pos.TOP_LEFT);
        dashboardPage = new VBox(16, buildHero(), buildDashboardGrid());
        dashboardPage.getStyleClass().add("dashboard-page");
        VBox.setVgrow(dashboardGrid, Priority.ALWAYS);
        mainViewHost.getChildren().setAll(dashboardPage);
        VBox shell = new VBox(16, buildTopBar(), mainViewHost);
        shell.getStyleClass().add("dashboard-shell");
        VBox.setVgrow(mainViewHost, Priority.ALWAYS);
        contentRoot.setCenter(shell);
        sceneRoot.getChildren().addAll(ambient, contentRoot);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        double initialWidth = Math.min(1240, Math.max(720, bounds.getWidth() - 40));
        double initialHeight = Math.min(820, Math.max(620, bounds.getHeight() - 60));
        Scene scene = new Scene(sceneRoot, initialWidth, initialHeight);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css"), "style.css resource is missing").toExternalForm());
        scene.widthProperty().addListener((observable, oldValue, width) -> updateResponsiveLayout(width.doubleValue()));
        stage.setTitle("Livara Study Planner");
        stage.setMinWidth(720);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
        updateResponsiveLayout(scene.getWidth());
        renderSession();
    }

    private Pane buildAmbientLayer() {
        Pane ambient = new Pane();
        ambient.setMouseTransparent(true);
        ambient.getStyleClass().add("ambient-layer");
        Circle halo = new Circle();
        halo.getStyleClass().add("ambient-halo");
        halo.centerXProperty().bind(ambient.widthProperty().multiply(.88));
        halo.centerYProperty().bind(ambient.heightProperty().multiply(.04));
        halo.radiusProperty().bind(ambient.widthProperty().multiply(.28));
        Circle orbit = new Circle();
        orbit.getStyleClass().add("ambient-orbit");
        orbit.centerXProperty().bind(ambient.widthProperty().multiply(.96));
        orbit.centerYProperty().bind(ambient.heightProperty().multiply(.14));
        orbit.radiusProperty().bind(ambient.widthProperty().multiply(.24));
        ambient.getChildren().addAll(halo, orbit);
        return ambient;
    }

    private HBox buildTopBar() {
        Label brand = new Label("LIVARA");
        brand.getStyleClass().add("brand-mark");
        Label context = new Label("STUDY PLANNER");
        context.getStyleClass().add("top-context");
        HBox left = new HBox(10, brand, context);
        left.setAlignment(Pos.CENTER_LEFT);

        Button homeButton = new Button();
        homeButton.setGraphic(toolbarIcon("home"));
        homeButton.setFocusTraversable(false);
        homeButton.setAccessibleText("Open dashboard");
        homeButton.setTooltip(new Tooltip("Dashboard"));
        homeButton.getStyleClass().add("menu-button");
        homeButton.setOnAction(event -> showDashboardPage());

        menu.setText("");
        menuIcon = toolbarIcon("menu-2");
        menu.setGraphic(menuIcon);
        menu.setOnMouseEntered(event -> animateScale(menuIcon, compactLayout ? 0.9 : 1.12));
        menu.setOnMouseExited(event -> animateScale(menuIcon, compactLayout ? 0.78 : 1.0));
        menu.setFocusTraversable(false);
        menu.setAccessibleText("Open planner menu");
        menu.setTooltip(new Tooltip("Planner menu"));
        menu.getStyleClass().add("menu-button");
        javafx.scene.control.Menu workflowMenu = new javafx.scene.control.Menu("Workflow template");
        MenuItem activeTemplate = new MenuItem(current.workflowTemplate().label() + " (active)");
        activeTemplate.setDisable(true);
        workflowMenu.getItems().add(activeTemplate);
        MenuItem reload = new MenuItem("Reload schedule");
        reload.setOnAction(event -> reloadSchedule());
        MenuItem validate = new MenuItem("Validate schedule");
        validate.setOnAction(event -> validateCurrent());
        MenuItem cycleA = new MenuItem("Apply cycle A");
        cycleA.setOnAction(event -> setCycle(Cycle.A));
        MenuItem cycleB = new MenuItem("Apply cycle B");
        cycleB.setOnAction(event -> setCycle(Cycle.B));
        MenuItem reset = new MenuItem("Reset session progress");
        reset.setOnAction(event -> resetProgress());
        menu.getItems().addAll(workflowMenu, new SeparatorMenuItem(), reload, validate,
                new SeparatorMenuItem(), cycleA, cycleB, reset);
        menu.showingProperty().addListener((observable, wasShowing, isShowing) -> {
            if (isShowing) {
                Platform.runLater(this::animateMenuPopup);
            }
        });

        Button manage = new Button();
        manageIcon = toolbarIcon("edit");
        manage.setGraphic(manageIcon);
        manage.setFocusTraversable(false);
        manage.setAccessibleText("Manage templates and cycles");
        manage.setTooltip(new Tooltip("Manage templates and cycles"));
        manage.getStyleClass().addAll("menu-button", "manage-button");
        manage.setOnAction(event -> showTemplatePage());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, manage, homeButton, menu);
        actions.setAlignment(Pos.CENTER);
        actions.setMinHeight(28);
        actions.setPrefHeight(28);
        actions.setMaxHeight(28);
        HBox bar = new HBox(left, spacer, actions);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("app-bar");
        return bar;
    }

    private Node toolbarIcon(String iconId) {
        SVGPath glyph = TablerIcon.icon(iconId);
        glyph.getStyleClass().add("menu-icon");
        glyph.setScaleX(2.0 / 3.0);
        glyph.setScaleY(2.0 / 3.0);
        StackPane slot = new StackPane(glyph);
        slot.setMinSize(20, 20);
        slot.setPrefSize(20, 20);
        slot.setMaxSize(20, 20);
        slot.setAlignment(Pos.CENTER);
        slot.getStyleClass().add("icon-slot");
        return slot;
    }

    private VBox buildHero() {
        Label eyebrow = new Label("TODAY'S SESSION");
        eyebrow.getStyleClass().add("eyebrow");
        Label title = new Label("Build a focused study rhythm.");
        title.getStyleClass().add("hero-title");
        Label subtitle = new Label("One clear sequence of focus blocks and recovery pauses. No weekly table to manage.");
        subtitle.getStyleClass().add("hero-subtitle");
        cycleLabel.getStyleClass().add("hero-meta");
        focusDescription.setWrapText(true);
        focusDescription.getStyleClass().add("hero-description");

        Circle track = new Circle(PROGRESS_RING_RADIUS);
        track.getStyleClass().add("progress-ring-track");
        progressArc.setCenterX(0);
        progressArc.setCenterY(0);
        progressArc.setRadius(PROGRESS_RING_RADIUS);
        progressArc.getStrokeDashArray().setAll(PROGRESS_RING_CIRCUMFERENCE,
                PROGRESS_RING_CIRCUMFERENCE);
        progressArc.setStrokeDashOffset(PROGRESS_RING_CIRCUMFERENCE);
        progressArc.getStyleClass().add("progress-ring-arc");
        progressRingValue.getStyleClass().add("progress-ring-value");
        StackPane.setAlignment(track, Pos.CENTER);
        StackPane.setAlignment(progressArc, Pos.CENTER);
        StackPane.setAlignment(progressRingValue, Pos.CENTER);
        progressRing.getChildren().setAll(track, progressArc, progressRingValue);
        progressRing.setMinSize(PROGRESS_RING_SIZE, PROGRESS_RING_SIZE);
        progressRing.setPrefSize(PROGRESS_RING_SIZE, PROGRESS_RING_SIZE);
        progressRing.setMaxSize(PROGRESS_RING_SIZE, PROGRESS_RING_SIZE);
        progressRing.getStyleClass().add("progress-ring");

        VBox copy = new VBox(7, eyebrow, title, subtitle, cycleLabel, focusDescription);
        copy.getStyleClass().add("hero-copy");
        copy.setMinWidth(0);
        HBox heroContent = new HBox(22, copy, progressRing);
        heroContent.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(copy, Priority.ALWAYS);
        hero.getChildren().setAll(heroContent);
        hero.getStyleClass().add("hero-widget");
        return hero;
    }

    private GridPane buildDashboardGrid() {
        dashboardGrid.getStyleClass().add("dashboard-grid");
        dashboardGrid.setHgap(14);
        dashboardGrid.setVgap(14);
        dashboardGrid.setMaxWidth(Double.MAX_VALUE);
        dashboardGrid.add(buildMetricsWidget(), 0, 0, 2, 1);

        VBox flowWidget = buildFlowWidget();
        sideColumn.getStyleClass().add("widget-column");
        sideColumn.getChildren().setAll(buildSummaryWidget(), buildVaultWidget());
        dashboardGrid.add(flowWidget, 0, 1);
        dashboardGrid.add(sideColumn, 1, 1);
        GridPane.setHgrow(flowWidget, Priority.ALWAYS);
        GridPane.setVgrow(flowWidget, Priority.ALWAYS);
        GridPane.setHgrow(sideColumn, Priority.SOMETIMES);
        GridPane.setVgrow(sideColumn, Priority.ALWAYS);
        return dashboardGrid;
    }

    private VBox buildMetricsWidget() {
        VBox widget = widget("SESSION OVERVIEW");
        GridPane metrics = new GridPane();
        metrics.setHgap(10);
        metrics.setMaxWidth(Double.MAX_VALUE);
        metrics.getStyleClass().add("metrics-grid");
        metrics.add(metric("ACTIVE", activeMetric, "next focus"), 0, 0);
        metrics.add(metric("PROGRESS", progressLabel, "focus blocks"), 1, 0);
        metrics.add(metric("RHYTHM", rhythmMetric, "focus · pause"), 2, 0);
        for (int column = 0; column < 3; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(33.333);
            constraints.setHgrow(Priority.ALWAYS);
            metrics.getColumnConstraints().add(constraints);
        }
        widget.getChildren().add(metrics);
        return widget;
    }

    private VBox metric(String labelText, Label value, String hint) {
        Label label = new Label(labelText);
        label.getStyleClass().add("metric-label");
        value.getStyleClass().add("metric-value");
        Label caption = new Label(hint);
        caption.getStyleClass().add("metric-hint");
        VBox metric = new VBox(5, label, value, caption);
        metric.getStyleClass().add("metric-card");
        metric.setMaxWidth(Double.MAX_VALUE);
        return metric;
    }

    private VBox buildFlowWidget() {
        Label title = new Label("Today's flow");
        title.getStyleClass().add("widget-title");
        flowSummary.getStyleClass().add("widget-subtitle");
        VBox heading = new VBox(3, title, flowSummary);
        heading.getStyleClass().add("widget-heading");

        sessionList.getStyleClass().add("session-list");
        sessionList.setFillWidth(true);
        VBox flowContent = new VBox(12, heading, sessionList);
        flowContent.getStyleClass().add("flow-content");
        flowContent.setFillWidth(true);

        ScrollPane scroll = new ScrollPane(flowContent);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("flow-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox widget = widget(null);
        widget.setMinHeight(260);
        widget.setPrefHeight(420);
        widget.getStyleClass().add("flow-widget");
        widget.getChildren().add(scroll);
        VBox.setVgrow(widget, Priority.ALWAYS);
        return widget;
    }

    private VBox buildSummaryWidget() {
        Label title = new Label("Session status");
        title.getStyleClass().add("widget-title");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-bar");
        remainingMetric.getStyleClass().add("summary-value");
        completionSummaryValue.getStyleClass().add("summary-row-value");
        Label caption = new Label("The next incomplete item is highlighted. Completing every focus and pause advances the cycle automatically.");
        caption.setWrapText(true);
        caption.getStyleClass().add("widget-copy");
        rotationValue.getStyleClass().add("summary-row-value");
        completedCyclesValue.getStyleClass().add("summary-row-value");
        Label rotationLabel = new Label("CURRENT ROTATION");
        rotationLabel.getStyleClass().add("summary-row-label");
        Label completedLabel = new Label("COMPLETED CYCLES");
        completedLabel.getStyleClass().add("summary-row-label");
        HBox rotationRow = summaryRow(rotationLabel, rotationValue);
        rotationRow.getStyleClass().add("summary-divider");
        HBox completedRow = summaryRow(completedLabel, completedCyclesValue);
        completedRow.getStyleClass().add("summary-row-plain");
        Label completionLabel = new Label("SESSION COMPLETION");
        completionLabel.getStyleClass().add("summary-row-label");
        HBox completionRow = summaryRow(completionLabel, completionSummaryValue);
        summaryPanel.getChildren().setAll(title, remainingMetric, completionRow, progressBar, caption,
                rotationRow, completedRow);
        summaryPanel.getStyleClass().add("widget");
        return summaryPanel;
    }

    private HBox summaryRow(Label label, Label value) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, label, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("summary-row");
        return row;
    }

    private VBox buildVaultWidget() {
        Label title = new Label("Vault");
        title.getStyleClass().add("widget-title");
        Label hint = new Label("Open notes and projects in Oil through Neovim.");
        hint.setWrapText(true);
        hint.getStyleClass().add("widget-copy");
        Button openVault = new Button("Open Vault in Neovim");
        openVault.getStyleClass().add("primary-button");
        openVault.setMaxWidth(Double.MAX_VALUE);
        openVault.setOnAction(event -> openVaultInNvim());

        VBox folders = new VBox(2);
        folders.getStyleClass().add("folder-list");
        for (String folder : VAULT_FOLDERS) {
            Button shortcut = new Button(folder);
            shortcut.setGraphic(folderIcon(folder));
            shortcut.getStyleClass().add("folder-link");
            shortcut.setMaxWidth(Double.MAX_VALUE);
            shortcut.setAlignment(Pos.CENTER_LEFT);
            shortcut.setOnAction(event -> openVaultFolderInNvim(folder));
            folders.getChildren().add(shortcut);
        }
        vaultPanel.getChildren().setAll(title, hint, openVault, new Separator(), folders);
        vaultPanel.getStyleClass().add("widget");
        return vaultPanel;
    }

    private SVGPath folderIcon(String folder) {
        return switch (folder) {
            case "Black Box" -> TablerIcon.archive();
            case "Source Notes" -> TablerIcon.notes();
            case "Projects" -> TablerIcon.project();
            case "Daily Notes" -> TablerIcon.calendar();
            case "Xournal++" -> TablerIcon.palette();
            case "References" -> TablerIcon.bookmark();
            default -> TablerIcon.folder();
        };
    }

    private VBox widget(String eyebrowText) {
        VBox widget = new VBox(10);
        widget.getStyleClass().add("widget");
        if (eyebrowText != null) {
            Label eyebrow = new Label(eyebrowText);
            eyebrow.getStyleClass().add("widget-eyebrow");
            widget.getChildren().add(eyebrow);
        }
        return widget;
    }

    private void animateMenuPopup() {
        Node popup = menu.lookup(".context-menu");
        if (popup == null) {
            for (Window window : Window.getWindows()) {
                if (window != primaryStage && window.isShowing() && window.getScene() != null) {
                    popup = window.getScene().lookup(".context-menu");
                    if (popup != null) {
                        break;
                    }
                }
            }
        }
        if (popup == null) {
            return;
        }
        popup.setOpacity(0);
        popup.setScaleX(0.96);
        popup.setScaleY(0.96);
        FadeTransition fade = new FadeTransition(Duration.millis(150), popup);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(180), popup);
        scale.setToX(1);
        scale.setToY(1);
        new ParallelTransition(fade, scale).play();
    }

    private void showTemplatePage() {
        current = library.selected();
        Label eyebrow = new Label("PLANNER MANAGEMENT");
        eyebrow.getStyleClass().add("manager-eyebrow");
        Label title = new Label("Templates & cycles");
        title.getStyleClass().add("manager-title");
        Label subtitle = new Label("Shape the active study system without leaving the dashboard.");
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("manager-subtitle");
        TextField name = new TextField(current.name());
        name.setPromptText("Template name");
        name.getStyleClass().add("manager-field");
        ComboBox<Cycle> cycle = new ComboBox<>();
        cycle.getItems().addAll(Cycle.values());
        cycle.setConverter(new StringConverter<>() {
            @Override public String toString(Cycle value) { return value == null ? "" : "Cycle " + value.label(); }
            @Override public Cycle fromString(String value) { return Cycle.A; }
        });
        cycle.setValue(current.cycle());
        cycle.setMaxWidth(Double.MAX_VALUE);
        cycle.getStyleClass().add("manager-field");
        ComboBox<WorkflowTemplate> workflow = new ComboBox<>();
        workflow.getItems().addAll(WorkflowTemplate.values());
        workflow.setConverter(new StringConverter<>() {
            @Override public String toString(WorkflowTemplate value) { return value == null ? "" : value.label(); }
            @Override public WorkflowTemplate fromString(String value) { return WorkflowTemplate.MARKET_PROGRAMMING; }
        });
        workflow.setValue(current.workflowTemplate());
        workflow.setMaxWidth(Double.MAX_VALUE);
        workflow.getStyleClass().add("manager-field");
        Spinner<Integer> pause = new Spinner<>(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 60, current.pauseMinutes(), 15));
        pause.setMaxWidth(Double.MAX_VALUE);
        pause.getStyleClass().add("manager-field");
        ComboBox<String> icon = new ComboBox<>();
        icon.getItems().addAll(TEMPLATE_ICON_IDS);
        icon.setValue(current.iconId());
        icon.setConverter(new StringConverter<>() {
            @Override public String toString(String value) {
                return value == null ? "" : value.replace('-', ' ').toUpperCase(java.util.Locale.ENGLISH);
            }
            @Override public String fromString(String value) { return value == null ? "layout-dashboard" : value; }
        });
        icon.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : value.replace('-', ' ').toUpperCase(java.util.Locale.ENGLISH));
                setGraphic(empty || value == null ? null : TablerIcon.icon(value));
            }
        });
        icon.setMaxWidth(Double.MAX_VALUE);
        icon.getStyleClass().add("manager-field");
        GridPane form = new GridPane();
        form.setHgap(12);
        form.setVgap(10);
        form.getStyleClass().add("manager-form");
        form.setVisible(false);
        form.setManaged(false);
        addManagerField(form, 0, "TEMPLATE NAME", name);
        addManagerField(form, 1, "CYCLE", cycle);
        addManagerField(form, 2, "WORKFLOW", workflow);
        addManagerField(form, 3, "PAUSE MINUTES", pause);
        addManagerField(form, 4, "TEMPLATE ICON", icon);

        VBox blockEditor = new VBox(6);
        blockEditor.getStyleClass().add("block-editor");
        List<BlockEditorRow> blockRows = new ArrayList<>();
        int initialOrder = 1;
        for (StudyBlock block : current.sequence()) {
            addBlockEditorRow(blockEditor, blockRows, initialOrder++, block);
        }
        Button addBlock = new Button("Add block");
        addBlock.getStyleClass().add("secondary-button");
        addBlock.setOnAction(event -> showAddBlockPopup(blockEditor, blockRows,
                new StudyBlock(blockRows.size() + 1, FocusArea.MARKET_PROGRAMMING,
                        "New focus block", java.time.Duration.ofMinutes(60), pause.getValue())));
        HBox blockTools = new HBox(addBlock);
        blockTools.setAlignment(Pos.CENTER_RIGHT);
        blockTools.getStyleClass().add("block-editor-tools");
        Label blocksTitle = new Label("STUDY SEQUENCE");
        blocksTitle.getStyleClass().add("manager-label");
        VBox blockSection = new VBox(7, blocksTitle, blockEditor, blockTools);
        blockSection.getStyleClass().add("block-editor-section");

        Label scope = new Label("This editor updates the active schedule and preserves its validated block sequence.\n"
                + "Cycle A and B are available as the built-in rotation modes.");
        scope.setWrapText(true);
        scope.getStyleClass().add("manager-note");

        Button save = new Button("Save changes");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            try {
                ScheduleTemplate edited = rebuildEditedTemplate(name.getText(), cycle.getValue(), workflow.getValue(), pause.getValue(), icon.getValue(), blockRows);
                library = library.updateSelected(edited);
                current = library.selected();
                progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
                persistSchedule();
                persistProgress();
                expandedItemId = null;
                renderSession();
                statusLabel.setText("Template updated.");
                showDashboardPage();
            } catch (RuntimeException exception) {
                showError("Unable to save template", exception.getMessage());
            }
        });

        Button restore = new Button("Restore");
        restore.getStyleClass().add("secondary-button");
        restore.setTooltip(new Tooltip("Restore the last saved template"));
        restore.setOnAction(event -> restoreLastSavedTemplate());

        Button remove = new Button("Delete template");
        remove.getStyleClass().add("danger-button");
        remove.setOnAction(event -> {
            try {
                String removedId = library.selectedTemplateId();
                if (library.entries().size() == 1) {
                    ScheduleTemplate fallback = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
                    library = TemplateLibrary.single("default", fallback);
                    usingLegacyProgressPath = true;
                } else {
                    library = library.remove(removedId);
                }
                activateSelectedTemplate();
                persistSchedule();
                persistProgress();
                expandedItemId = null;
                renderSession();
                statusLabel.setText("Template deleted; active template updated.");
                showDashboardPage();
            } catch (RuntimeException exception) {
                showError("Unable to delete template", exception.getMessage());
            }
        });

        HBox actions = new HBox(9, restore, new Region(), save);
        HBox.setHgrow(actions.getChildren().get(1), Priority.ALWAYS);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.getStyleClass().add("manager-actions");
        VBox intro = new VBox(4, eyebrow, title, subtitle);
        HBox pageHeader = new HBox(16, intro);
        pageHeader.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(intro, Priority.ALWAYS);
        Label active = new Label("ACTIVE TEMPLATE\n" + current.name());
        active.getStyleClass().add("manager-active-badge");
        pageHeader.getChildren().add(active);

        FlowPane templateLibrary = new FlowPane();
        templateLibrary.setHgap(10);
        templateLibrary.setVgap(10);
        templateLibrary.setPrefWrapLength(760);
        templateLibrary.getStyleClass().add("template-library");
        List<VBox> templateCards = new ArrayList<>();
        for (TemplateLibrary.Entry entry : library.entries()) {
            ScheduleTemplate schedule = entry.schedule();
            addTemplateCard(templateLibrary, templateCards, schedule.name(),
                    "Complete study system · " + schedule.totalBlocks() + " blocks",
                    TablerIcon.icon(schedule.iconId()), entry.id().equals(library.selectedTemplateId()),
                    () -> selectTemplate(entry.id()));
        }
        addTemplateCard(templateLibrary, templateCards, "New template",
                "Create and edit a new study system", TablerIcon.icon("plus"), false,
                this::startNewTemplate);
        Label libraryTitle = new Label("TEMPLATE LIBRARY");
        libraryTitle.getStyleClass().add("manager-label");
        Label libraryHint = new Label("Templates contain their cycles, blocks, pauses and tasks. Select one to edit it below.");
        libraryHint.getStyleClass().add("manager-section-hint");
        VBox librarySection = new VBox(8, new VBox(2, libraryTitle, libraryHint), templateLibrary);
        librarySection.getStyleClass().add("manager-section");

        Label detailsTitle = new Label("Template identity");
        detailsTitle.getStyleClass().add("manager-section-title");
        Label detailsHint = new Label("One complete system: choose its identity here, then edit the active cycle below.");
        detailsHint.getStyleClass().add("manager-section-hint");
        Label identitySummary = new Label();
        identitySummary.getStyleClass().add("identity-summary");
        Button editIdentity = new Button("Edit details");
        editIdentity.getStyleClass().add("secondary-button");
        Runnable refreshIdentity = () -> identitySummary.setText(name.getText() + "  ·  Cycle " + cycle.getValue().label()
                + "  ·  " + workflow.getValue().label() + "  ·  " + pause.getValue() + " min pause");
        refreshIdentity.run();
        editIdentity.setOnAction(event -> showIdentityPopup(editIdentity, name, cycle, workflow, pause, icon, refreshIdentity));
        HBox identityBody = new HBox(12, identitySummary, editIdentity);
        identityBody.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(identitySummary, Priority.ALWAYS);
        Button deleteTemplate = new Button("Delete template");
        deleteTemplate.getStyleClass().add("danger-button");
        deleteTemplate.setOnAction(remove.getOnAction());
        identityBody.getChildren().add(deleteTemplate);
        VBox detailsHeading = new VBox(2, detailsTitle, detailsHint);
        VBox detailsSection = new VBox(8, detailsHeading, identityBody);
        detailsSection.getStyleClass().add("manager-section");

        VBox content = new VBox(16, pageHeader, librarySection, detailsSection, blockSection, scope, actions);
        content.getStyleClass().add("manager-panel");
        content.setFillWidth(true);
        VBox.setVgrow(form, Priority.ALWAYS);
        content.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        ScrollPane managerScroll = new ScrollPane(content);
        managerScroll.setFitToWidth(true);
        managerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        managerScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        managerScroll.getStyleClass().add("manager-scroll");
        mainViewHost.getChildren().setAll(managerScroll);
        content.setOpacity(0);
        content.setTranslateY(10);
        FadeTransition fade = new FadeTransition(Duration.millis(180), content);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(180), content);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private StackPane popupRoot(Node panel) {
        StackPane root = new StackPane(panel);
        root.getStyleClass().add("popup-root");
        root.setPadding(Insets.EMPTY);
        return root;
    }

    private void stylePopupScene(Popup popup) {
        if (popup.getScene() == null) {
            return;
        }
        popup.getScene().setFill(Color.TRANSPARENT);
        String stylesheet = Objects.requireNonNull(getClass().getResource("/style.css"),
                "style.css resource is missing").toExternalForm();
        if (!popup.getScene().getStylesheets().contains(stylesheet)) {
            popup.getScene().getStylesheets().add(stylesheet);
        }
    }

    private void showIdentityPopup(Node anchor, TextField name, ComboBox<Cycle> cycle,
                                   ComboBox<WorkflowTemplate> workflow, Spinner<Integer> pause,
                                   ComboBox<String> icon, Runnable refresh) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("editor-popup");
        Label title = new Label("Edit template identity");
        title.getStyleClass().add("popup-title");
        TextField nameCopy = new TextField(name.getText());
        nameCopy.setPromptText("Template name");
        ComboBox<Cycle> cycleCopy = new ComboBox<>();
        cycleCopy.getItems().addAll(Cycle.values());
        cycleCopy.setValue(cycle.getValue());
        ComboBox<WorkflowTemplate> workflowCopy = new ComboBox<>();
        workflowCopy.getItems().addAll(WorkflowTemplate.values());
        workflowCopy.setValue(workflow.getValue());
        Spinner<Integer> pauseCopy = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 60, pause.getValue(), 15));
        ComboBox<String> iconCopy = new ComboBox<>();
        iconCopy.getItems().addAll(icon.getItems());
        iconCopy.setValue(icon.getValue());
        for (Node node : List.of(nameCopy, cycleCopy, workflowCopy, pauseCopy, iconCopy)) {
            node.getStyleClass().add("popup-field");
            if (node instanceof Control control) control.setMaxWidth(Double.MAX_VALUE);
        }
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        Button apply = new Button("Apply details");
        apply.getStyleClass().add("primary-button");
        HBox actions = new HBox(8, cancel, apply);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().setAll(title, new Label("Name"), nameCopy, new Label("Cycle"), cycleCopy,
                new Label("Workflow"), workflowCopy, new Label("Pause"), pauseCopy, new Label("Icon"), iconCopy, actions);
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(popupRoot(panel));
        cancel.setOnAction(event -> popup.hide());
        apply.setOnAction(event -> {
            name.setText(nameCopy.getText());
            cycle.setValue(cycleCopy.getValue());
            workflow.setValue(workflowCopy.getValue());
            pause.getValueFactory().setValue(pauseCopy.getValue());
            icon.setValue(iconCopy.getValue());
            refresh.run();
            popup.hide();
        });
        popup.setOnShown(event -> {
            stylePopupScene(popup);
            javafx.geometry.Point2D point = anchor.localToScreen(0, anchor.getLayoutBounds().getHeight() + 8);
            popup.setX(point.getX());
            popup.setY(point.getY());
        });
        popup.show(anchor.getScene().getWindow(), 0, 0);
    }

    private void showBlockEditorPopup(BlockEditorRow row) {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("editor-popup");
        Label title = new Label("Edit study block");
        title.getStyleClass().add("popup-title");
        Button apply = new Button("Apply block");
        apply.getStyleClass().add("primary-button");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        HBox actions = new HBox(8, cancel, apply);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().setAll(title, new Label("Focus"), row.focus, new Label("Topic"), row.topic,
                new Label("Focus minutes"), row.duration, new Label("Break after"), row.pause, actions);
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(popupRoot(panel));
        cancel.setOnAction(event -> popup.hide());
        apply.setOnAction(event -> {
            row.refreshSummary();
            popup.hide();
        });
        popup.setOnShown(event -> {
            if (popup.getScene() != null) popup.getScene().setFill(Color.TRANSPARENT);
            javafx.geometry.Point2D point = row.row.localToScreen(
                    Math.max(18, row.row.getLayoutBounds().getWidth() / 2 - 165), 26);
            popup.setX(point.getX());
            popup.setY(point.getY());
        });
        popup.show(row.row.getScene().getWindow(), 0, 0);
    }

    private void showAddBlockPopup(VBox editor, List<BlockEditorRow> rows, StudyBlock defaults) {
        BlockEditorRow draft = new BlockEditorRow(rows.size() + 1, defaults, () -> { });
        VBox panel = new VBox(10);
        panel.getStyleClass().add("editor-popup");
        Label title = new Label("Add study block");
        title.getStyleClass().add("popup-title");
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("secondary-button");
        Button add = new Button("Add block");
        add.getStyleClass().add("primary-button");
        HBox actions = new HBox(8, cancel, add);
        actions.setAlignment(Pos.CENTER);
        actions.setMaxWidth(Double.MAX_VALUE);
        panel.getChildren().setAll(title, new Label("Focus"), draft.focus, new Label("Topic"), draft.topic,
                new Label("Focus minutes"), draft.duration, new Label("Break after"), draft.pause, actions);
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(popupRoot(panel));
        cancel.setOnAction(event -> popup.hide());
        add.setOnAction(event -> {
            draft.refreshSummary();
            draft.setEditAction(() -> showBlockEditorPopup(draft));
            rows.add(draft);
            editor.getChildren().add(draft.row);
            popup.hide();
        });
        popup.setOnShown(event -> {
            if (popup.getScene() != null) popup.getScene().setFill(Color.TRANSPARENT);
            javafx.geometry.Point2D point = add.localToScreen(0, add.getLayoutBounds().getHeight() + 8);
            popup.setX(point.getX());
            popup.setY(point.getY());
        });
        popup.show(add.getScene().getWindow(), 0, 0);
    }

    private void addTemplateCard(FlowPane library, List<VBox> cards, String name, String description,
                                 SVGPath icon, boolean selected, Runnable onSelected) {
        Label title = new Label(name);
        title.getStyleClass().add("template-card-title");
        Label hint = new Label(description);
        hint.getStyleClass().add("template-card-hint");
        icon.getStyleClass().add("template-card-icon");
        VBox card = new VBox(8, icon, title, hint);
        card.setPrefWidth(168);
        card.setMinHeight(112);
        card.getStyleClass().add("template-card");
        if (selected) {
            card.getStyleClass().add("selected");
        }
        card.setOnMouseClicked(event -> {
            for (VBox candidate : cards) {
                candidate.getStyleClass().remove("selected");
            }
            card.getStyleClass().add("selected");
            card.setScaleX(0.98);
            card.setScaleY(0.98);
            ScaleTransition snap = new ScaleTransition(Duration.millis(150), card);
            snap.setToX(1);
            snap.setToY(1);
            snap.setInterpolator(Interpolator.EASE_OUT);
            snap.play();
            onSelected.run();
        });
        cards.add(card);
        library.getChildren().add(card);
    }

    private void addBlockEditorRow(VBox editor, List<BlockEditorRow> rows, int order, StudyBlock block) {
        final BlockEditorRow[] holder = new BlockEditorRow[1];
        holder[0] = new BlockEditorRow(order, block, () -> {
            rows.remove(holder[0]);
            editor.getChildren().remove(holder[0].row);
        });
        holder[0].setEditAction(() -> showBlockEditorPopup(holder[0]));
        rows.add(holder[0]);
        editor.getChildren().add(holder[0].row);
    }

    private ScheduleTemplate rebuildEditedTemplate(String name, Cycle cycle, WorkflowTemplate workflow,
                                                   int pauseMinutes, String iconId, List<BlockEditorRow> rows) {
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("A template needs at least one study block");
        }
        List<StudyBlock> sequence = new ArrayList<>();
        int order = 1;
        for (BlockEditorRow row : rows) {
            row.setOrder(order);
            sequence.add(row.toBlock(order++));
        }
        return service.editTemplate(current, name, cycle, workflow, pauseMinutes, iconId, sequence);
    }

    private void showDashboardPage() {
        mainViewHost.getChildren().setAll(dashboardPage);
        dashboardPage.setOpacity(0);
        dashboardPage.setTranslateY(-8);
        FadeTransition fade = new FadeTransition(Duration.millis(160), dashboardPage);
        fade.setToValue(1);
        javafx.animation.TranslateTransition slide = new javafx.animation.TranslateTransition(Duration.millis(160), dashboardPage);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();
    }

    private void addManagerField(GridPane form, int row, String labelText, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("manager-label");
        form.add(label, 0, row);
        form.add(control, 1, row);
        GridPane.setHgrow(control, Priority.ALWAYS);
    }

    private void startNewTemplate() {
        ScheduleTemplate draft = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING)
                .withIdentity("New study template", "x");
        library = library.add(draft);
        usingLegacyProgressPath = false;
        activateSelectedTemplate();
        persistSchedule();
        persistProgress();
        expandedItemId = null;
        showTemplatePage();
    }

    private void selectTemplate(String id) {
        try {
            library = library.select(id);
            activateSelectedTemplate();
            persistSchedule();
            renderSession();
            showTemplatePage();
        } catch (RuntimeException exception) {
            showError("Unable to select template", exception.getMessage());
        }
    }

    private void restoreLastSavedTemplate() {
        if (lastSavedLibrary == null) {
            return;
        }
        library = lastSavedLibrary;
        activateSelectedTemplate();
        expandedItemId = null;
        showTemplatePage();
        statusLabel.setText("Last saved template restored.");
    }

    private HBox buildFooter() {
        statusLabel.getStyleClass().add("status");
        HBox footer = new HBox(statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("status-bar");
        return footer;
    }

    private void updateResponsiveLayout(double width) {
        boolean wide = width >= WIDE_BREAKPOINT;
        compactLayout = !wide;
        if (menuIcon != null) {
            double baseScale = compactLayout ? 0.78 : 1.0;
            menuIcon.setScaleX(baseScale);
            menuIcon.setScaleY(baseScale);
        }
        dashboardGrid.getStyleClass().removeAll("layout-wide", "layout-compact");
        dashboardGrid.getStyleClass().add(wide ? "layout-wide" : "layout-compact");
        dashboardGrid.getColumnConstraints().clear();
        dashboardGrid.getRowConstraints().clear();
        javafx.scene.layout.RowConstraints overviewRow = new javafx.scene.layout.RowConstraints();
        overviewRow.setVgrow(Priority.NEVER);
        overviewRow.setFillHeight(true);
        dashboardGrid.getRowConstraints().add(overviewRow);
        javafx.scene.layout.RowConstraints contentRow = new javafx.scene.layout.RowConstraints();
        contentRow.setVgrow(Priority.ALWAYS);
        contentRow.setFillHeight(true);
        dashboardGrid.getRowConstraints().add(contentRow);
        ColumnConstraints primary = new ColumnConstraints();
        primary.setHgrow(Priority.ALWAYS);
        primary.setFillWidth(true);
        dashboardGrid.getColumnConstraints().add(primary);
        if (wide) {
            ColumnConstraints secondary = new ColumnConstraints();
            secondary.setMinWidth(260);
            secondary.setPrefWidth(290);
            secondary.setMaxWidth(340);
            secondary.setHgrow(Priority.SOMETIMES);
            secondary.setFillWidth(true);
            dashboardGrid.getColumnConstraints().add(secondary);
            if (dashboardGrid.getChildren().size() > 2) {
                Node flow = dashboardGrid.getChildren().get(1);
                GridPane.setColumnIndex(flow, 0);
                GridPane.setColumnSpan(flow, 1);
                GridPane.setColumnIndex(sideColumn, 1);
                GridPane.setRowIndex(sideColumn, 1);
            }
        } else {
            javafx.scene.layout.RowConstraints sideRow = new javafx.scene.layout.RowConstraints();
            sideRow.setVgrow(Priority.NEVER);
            sideRow.setFillHeight(true);
            dashboardGrid.getRowConstraints().add(sideRow);
            if (dashboardGrid.getChildren().size() > 2) {
                Node flow = dashboardGrid.getChildren().get(1);
                GridPane.setColumnIndex(flow, 0);
                GridPane.setColumnSpan(flow, 1);
                GridPane.setColumnIndex(sideColumn, 0);
                GridPane.setRowIndex(sideColumn, 2);
            }
        }
    }

    private void renderSession() {
        if (animateSessionRefresh && !sessionList.getChildren().isEmpty()) {
            animateSessionRefresh = false;
            FadeTransition fadeOut = new FadeTransition(Duration.millis(120), sessionList);
            fadeOut.setToValue(0.35);
            fadeOut.setInterpolator(Interpolator.EASE_BOTH);
            fadeOut.setOnFinished(event -> {
                renderSession();
                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), sessionList);
                fadeIn.setFromValue(0.35);
                fadeIn.setToValue(1.0);
                fadeIn.setInterpolator(Interpolator.EASE_BOTH);
                fadeIn.play();
            });
            fadeOut.play();
            return;
        }
        sessionItems = sessionItems(current);
        if (expandedItemId == null || sessionItems.stream().noneMatch(item -> item.id().equals(expandedItemId))) {
            expandedItemId = nextPendingExpandableItemId();
        }
        sessionList.getChildren().clear();
        for (int index = 0; index < sessionItems.size(); index++) {
            StudySessionItem item = sessionItems.get(index);
            Node card = item.pause() ? pauseCard(item, index) : studyCard(item, index);
            card.setManaged(true);
            sessionList.getChildren().add(card);
        }
        animateNextExpansion = false;

        long totalStudy = sessionItems.stream().filter(item -> !item.pause()).count();
        long completedStudy = sessionItems.stream()
                .filter(item -> !item.pause() && progress.isCompleted(item.id()))
                .count();
        long completedItems = sessionItems.stream().filter(item -> progress.isCompleted(item.id())).count();
        long remainingItems = sessionItems.size() - completedItems;
        double ratio = totalStudy == 0 ? 0 : (double) completedStudy / totalStudy;
        activeMetric.setText(nextPendingIndex() < 0 ? "Complete" : "Block " + (nextPendingIndex() + 1));
        progressLabel.setText(completedStudy + " / " + totalStudy);
        rhythmMetric.setText("60 / 15");
        remainingMetric.setText(remainingItems + " items remaining");
        rotationValue.setText("Cycle " + current.cycle().label() + "  ·  " + current.cycle().subjects());
        completedCyclesValue.setText("0");
        completionSummaryValue.setText(completedStudy + " / " + totalStudy + " focus blocks");
        progressBar.setProgress(ratio);
        progressRingArc(ratio);
        progressRingValue.setText(completedStudy + " / " + totalStudy);
        cycleLabel.setText(current.workflowTemplate().label() + "  ·  Cycle " + current.cycle().label());
        focusDescription.setText(current.workflowTemplate().description());
        flowSummary.setText(totalStudy + " focus blocks · " + (sessionItems.size() - totalStudy) + " recovery pauses");
        statusLabel.setText("");
    }

    private VBox studyCard(StudySessionItem item, int index) {
        CircularCheckBox done = checkBox(item, "item-check", 10, 6.2, 3.1);
        Label title = new Label(item.title());
        title.getStyleClass().add("item-title");
        Label subtitle = new Label(item.subtitle());
        subtitle.getStyleClass().add("item-subtitle");
        subtitle.setWrapText(true);
        VBox copy = new VBox(3, title, subtitle);
        copy.getStyleClass().add("item-copy");
        HBox.setHgrow(copy, Priority.ALWAYS);

        Label duration = new Label(item.durationMinutes() + " min");
        duration.getStyleClass().add("duration-chip");
        HBox header = new HBox(10, done, copy, duration);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("session-header");

        VBox detail = buildStudyDetails(item);
        VBox card = new VBox(0, header, detail);
        card.getStyleClass().addAll("session-card", "study-card");
        configureCardState(card, detail, item, index);
        return card;
    }

    private VBox pauseCard(StudySessionItem item, int index) {
        CircularCheckBox done = checkBox(item, "item-check", 10, 6.2, 3.1);
        Label title = new Label("Recovery pause");
        title.getStyleClass().add("pause-title");
        Label subtitle = new Label("Step away, hydrate, and return with intention");
        subtitle.getStyleClass().add("pause-subtitle");
        VBox copy = new VBox(2, title, subtitle);
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label duration = new Label(item.durationMinutes() + " min");
        duration.getStyleClass().add("duration-chip");
        HBox header = new HBox(10, done, copy, duration);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("session-header");
        VBox card = new VBox(header);
        card.getStyleClass().addAll("session-card", "pause-card");
        configureCardState(card, null, item, index);
        return card;
    }

    private CircularCheckBox checkBox(StudySessionItem item, String cssClass,
                                      double outerRadius, double subRadius, double coreRadius) {
        CircularCheckBox done = new CircularCheckBox(cssClass, outerRadius, subRadius, coreRadius);
        done.setSelected(progress.isCompleted(item.id()));
        done.setOnAction(event -> {
            pulse(done.visual());
            toggleItem(item);
        });
        return done;
    }

    private VBox buildStudyDetails(StudySessionItem item) {
        Label focusChip = new Label("60 min study");
        Label pauseChip = new Label("15 min pause");
        focusChip.getStyleClass().add("detail-chip");
        pauseChip.getStyleClass().addAll("detail-chip", "detail-chip-secondary");
        HBox chips = new HBox(7, focusChip, pauseChip);
        chips.setFillHeight(true);
        chips.setMaxWidth(Region.USE_PREF_SIZE);
        chips.getStyleClass().add("detail-chips");

        Label callout = new Label("Strategy  ·  " + item.focus().label() + " practice");
        callout.setWrapText(true);
        callout.setMaxWidth(Region.USE_PREF_SIZE);
        callout.getStyleClass().add("strategy-callout");
        chips.prefWidthProperty().bind(callout.widthProperty());
        chips.maxWidthProperty().bind(callout.widthProperty());
        HBox.setHgrow(focusChip, Priority.ALWAYS);
        HBox.setHgrow(pauseChip, Priority.ALWAYS);

        List<StudyTask> taskDefinitions = studyTasks();
        VBox tasks = new VBox(0);
        for (int taskIndex = 0; taskIndex < taskDefinitions.size(); taskIndex++) {
            StudyTask task = taskDefinitions.get(taskIndex);
            tasks.getChildren().add(taskRow(item, task));
        }
        tasks.getStyleClass().add("task-list");
        Separator separator = new Separator();
        separator.getStyleClass().add("detail-separator");
        VBox detailContent = new VBox(10, chips, callout, tasks);
        detailContent.getStyleClass().add("detail-content");
        VBox detail = new VBox(10, separator, detailContent);
        detail.getStyleClass().add("item-detail");
        detail.setVisible(false);
        detail.setManaged(false);
        return detail;
    }

    private HBox taskRow(StudySessionItem item, StudyTask task) {
        String taskId = taskId(item, task);
        CircularCheckBox done = checkBox(item, "task-check", 7.5, 4.7, 2.2);
        done.setSelected(progress.isCompleted(taskId) || progress.isCompleted(item.id()));
        done.setOnAction(event -> {
            pulse(done.visual());
            toggleTask(item, task, done.isSelected());
        });

        Label taskTitle = new Label(task.title());
        taskTitle.getStyleClass().add("task-title");
        Label taskText = new Label(task.description());
        taskText.setWrapText(true);
        taskText.getStyleClass().add("task-text");
        VBox copy = new VBox(2, taskTitle, taskText);
        HBox.setHgrow(copy, Priority.ALWAYS);
        HBox row = new HBox(9, done, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("task-row");
        if (progress.isCompleted(taskId) || progress.isCompleted(item.id())) {
            row.getStyleClass().add("completed");
        }
        return row;
    }

    private void configureCardState(VBox card, Node detail, StudySessionItem item, int index) {
        boolean completed = progress.isCompleted(item.id());
        if (completed) {
            card.getStyleClass().add("completed");
        } else {
            card.getStyleClass().add("pending");
        }
        if (isActive(index)) {
            card.getStyleClass().add("active");
        }
        if (detail != null) {
            boolean expanded = !completed && item.id().equals(expandedItemId);
            boolean animateExpansion = expanded && animateNextExpansion;
            detail.setVisible(expanded && !animateExpansion);
            detail.setManaged(expanded && !animateExpansion);
            if (expanded) {
                card.getStyleClass().add("expanded");
            }
            if (animateExpansion) {
                Platform.runLater(() -> animateDetails(detail, true));
            }
            card.setOnMouseClicked(event -> {
                if (!(event.getTarget() instanceof CircularCheckBox)) {
                    boolean visible = !detail.isVisible();
                    animateDetails(detail, visible);
                    card.getStyleClass().remove("expanded");
                    if (visible) {
                        expandedItemId = item.id();
                        card.getStyleClass().add("expanded");
                    } else if (item.id().equals(expandedItemId)) {
                        expandedItemId = null;
                    }
                }
            });
        }
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.NEVER);
    }

    private void progressRingArc(double ratio) {
        double clamped = Math.max(0, Math.min(1, ratio));
        double targetOffset = PROGRESS_RING_CIRCUMFERENCE * (1 - clamped);
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(progressArc.strokeDashOffsetProperty(), targetOffset, Interpolator.EASE_BOTH)));
        timeline.play();
    }

    private void animateScale(Node node, double scale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(140), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.setInterpolator(Interpolator.EASE_BOTH);
        transition.play();
    }

    private void pulse(Node node) {
        if (node == null) {
            return;
        }
        ScaleTransition pulse = new ScaleTransition(Duration.millis(180), node);
        pulse.setFromX(0.82);
        pulse.setFromY(0.82);
        pulse.setToX(1.0);
        pulse.setToY(1.0);
        pulse.setInterpolator(Interpolator.EASE_OUT);
        pulse.play();
    }

    private void animateDetails(Node detail, boolean visible) {
        if (visible) {
            detail.setManaged(true);
            detail.setVisible(true);
            detail.setOpacity(0);
            detail.setScaleY(0.96);
            FadeTransition fade = new FadeTransition(Duration.millis(180), detail);
            fade.setToValue(1);
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), detail);
            scale.setToY(1);
            new ParallelTransition(fade, scale).play();
        } else {
            FadeTransition fade = new FadeTransition(Duration.millis(130), detail);
            fade.setToValue(0);
            fade.setOnFinished(event -> {
                detail.setVisible(false);
                detail.setManaged(false);
                detail.setOpacity(1);
                detail.setScaleY(1);
            });
            fade.play();
        }
    }

    private boolean isActive(int index) {
        return index == nextPendingIndex();
    }

    private int nextPendingIndex() {
        for (int index = 0; index < sessionItems.size(); index++) {
            if (!progress.isCompleted(sessionItems.get(index).id())) {
                return index;
            }
        }
        return -1;
    }
    private String nextPendingItemId() {
        int index = nextPendingIndex();
        return index < 0 ? null : sessionItems.get(index).id();
    }

    private String nextPendingExpandableItemId() {
        return sessionItems.stream()
                .filter(item -> !item.pause())
                .filter(item -> !progress.isCompleted(item.id()))
                .map(StudySessionItem::id)
                .findFirst()
                .orElse(null);
    }

    private void toggleItem(StudySessionItem item) {
        boolean completed = !progress.isCompleted(item.id());
        progress = item.pause()
                ? progress.withCompleted(item.id(), completed)
                : setStudyBlockCompleted(item, completed);
        progress = progress.withActiveItemIndex(nextPendingIndex());
        if (allSessionItemsCompleted()) {
            advanceCycleAutomatically();
            return;
        }
        expandedItemId = completed ? nextPendingExpandableItemId() : item.id();
        animateNextExpansion = expandedItemId != null;
        animateSessionRefresh = true;
        persistProgress();
        renderSession();
    }
    private void toggleTask(StudySessionItem item, StudyTask task, boolean completed) {
        progress = progress.withCompleted(taskId(item, task), completed);
        boolean allTasksCompleted = studyTasks().stream()
                .allMatch(candidate -> progress.isCompleted(taskId(item, candidate)));
        progress = progress.withCompleted(item.id(), allTasksCompleted)
                .withActiveItemIndex(nextPendingIndex());
        if (allSessionItemsCompleted()) {
            advanceCycleAutomatically();
            return;
        }
        expandedItemId = completed ? nextPendingExpandableItemId() : item.id();
        animateNextExpansion = expandedItemId != null;
        animateSessionRefresh = true;
        persistProgress();
        renderSession();
    }

    private ProgressState setStudyBlockCompleted(StudySessionItem item, boolean completed) {
        ProgressState updated = progress.withCompleted(item.id(), completed);
        for (StudyTask task : studyTasks()) {
            updated = updated.withCompleted(taskId(item, task), completed);
        }
        return updated;
    }

    private static List<StudyTask> studyTasks() {
        return List.of(
                new StudyTask("study", "Study", "Work through the core material with deliberate focus."),
                new StudyTask("annotation", "Annotation", "Capture the essential ideas, questions, and connections."),
                new StudyTask("practice", "Practice", "Work through one representative problem or implementation and record the result."));
    }

    private static String taskId(StudySessionItem item, StudyTask task) {
        return item.id() + "." + task.id();
    }

    private boolean allSessionItemsCompleted() {
        return !sessionItems.isEmpty()
                && sessionItems.stream().allMatch(item -> progress.isCompleted(item.id()));
    }

    private void setCycle(Cycle cycle) {
        if (current.cycle() == cycle) {
            statusLabel.setText("Cycle " + cycle.label() + " is already active.");
            return;
        }
        library = library.updateSelected(current.withCycle(cycle));
        activateSelectedTemplate();
        persistSchedule();
        persistProgress();
        renderSession();
    }

    private void advanceCycleAutomatically() {
        try {
            ScheduleTemplate advanced = service.advanceCycle(current);
            library = library.updateSelected(advanced);
            activateSelectedTemplate();
            persistSchedule();
            persistProgress();
            renderSession();
            statusLabel.setText("Session complete. Cycle " + current.cycle().label() + " is active.");
        } catch (RuntimeException exception) {
            showError("Unable to change cycle", exception.getMessage());
        }
    }

    private boolean isLegacyDefaultLibrary() {
        return library.entries().size() == 1 && library.selectedTemplateId().equals("default");
    }

    private Path legacyProgressPath() {
        return schedulePath.resolveSibling(schedulePath.getFileName() + ".progress.properties");
    }

    private Path progressPathFor(String templateId) {
        if (usingLegacyProgressPath && templateId.equals("default")) {
            return legacyProgressPath();
        }
        return schedulePath.resolveSibling(schedulePath.getFileName() + "." + templateId + ".progress.properties");
    }

    private void activateSelectedTemplate() {
        current = library.selected();
        progressPath = progressPathFor(library.selectedTemplateId());
        try {
            progress = progressStore.loadOrEmpty(progressPath, current.cycle(), current.workflowTemplate());
        } catch (IOException | RuntimeException exception) {
            progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
            statusLabel.setText("Progress reset for the selected template: " + exception.getMessage());
        }
    }

    private void persistSchedule() {
        try {
            libraryStore.save(schedulePath, library);
            lastSavedLibrary = library;
        } catch (IOException | RuntimeException exception) {
            showError("Unable to save template library", exception.getMessage());
        }
    }

    private void persistProgress() {
        try {
            progressStore.save(progressPath, progress);
        } catch (IOException | RuntimeException exception) {
            showError("Unable to save progress", exception.getMessage());
        }
    }

    private void reloadSchedule() {
        try {
            library = libraryStore.load(schedulePath);
            current = library.selected();
            usingLegacyProgressPath = isLegacyDefaultLibrary();
            activateSelectedTemplate();
            lastSavedLibrary = library;
            renderSession();
            statusLabel.setText("Template library reloaded.");
        } catch (IOException | RuntimeException exception) {
            showError("Unable to reload template library", exception.getMessage());
        }
    }

    private void resetProgress() {
        progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        persistProgress();
        renderSession();
        statusLabel.setText("Session progress reset.");
    }

    private void validateCurrent() {
        List<String> errors = service.validate(current);
        if (errors.isEmpty()) {
            showInfo("Schedule is valid", "The schedule follows the 60-minute focus and 15-minute pause contract.");
        } else {
            showError("Schedule needs review", String.join("\n", errors));
        }
    }

    private void openVaultInNvim() {
        openVaultFolderInNvim("");
    }

    private void openVaultFolderInNvim(String relativePath) {
        Path vault = Path.of(System.getenv().getOrDefault("LIVARA_VAULT_ROOT",
                System.getProperty("user.home") + "/Vault"));
        Path target = relativePath.isBlank() ? vault : vault.resolve(relativePath);
        if (!Files.isDirectory(target)) {
            statusLabel.setText("Vault folder not found: " + target);
            return;
        }
        String oilCommand = "Oil " + vimEscape(target.toString());
        IOException failure = null;
        for (List<String> command : List.of(
                List.of("footclient", "nvim", "-c", oilCommand),
                List.of("wezterm", "start", "--", "nvim", "-c", oilCommand),
                List.of("nvim", "-c", oilCommand))) {
            try {
                new ProcessBuilder(command).start();
                statusLabel.setText("Neovim opened in Oil: " + target);
                return;
            } catch (IOException exception) {
                failure = exception;
            }
        }
        statusLabel.setText("Unable to open Neovim: "
                + (failure == null ? "no terminal available" : failure.getMessage()));
    }

    private static String vimEscape(String value) {
        return value.replace("\\\\", "\\\\\\\\")
                .replace(" ", "\\\\ ")
                .replace("|", "\\\\|")
                .replace("\"", "\\\\\"");
    }

    private void applyMatugenPalette(Pane root) {
        Path palette = Path.of(System.getenv().getOrDefault(
                "LIVARA_THEME_ROOT", System.getProperty("user.home") + "/.local/state/livara/theme")
        ).resolve("palette.dark.json");
        String base = paletteColor(palette, "base", "#17181d");
        String text = paletteColor(palette, "text", "#e5e1e9");
        String primary = paletteColor(palette, "primary", "#b8c8ff");
        String surface = paletteColor(palette, "surface1", "#23252d");
        root.setStyle("-fx-base: " + base + "; -fx-accent: " + primary
                + "; -fx-focus-color: " + primary + "; -fx-faint-focus-color: transparent"
                + "; -fx-text-base-color: " + text + "; -fx-control-inner-background: " + surface + ";");
    }

    private String paletteColor(Path path, String key, String fallback) {
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile(String.format(COLOR.pattern(), Pattern.quote(key))).matcher(json);
            return matcher.find() ? matcher.group(1) : fallback;
        } catch (IOException | RuntimeException ignored) {
            return fallback;
        }
    }

    private void styleDialogScene(Alert alert) {
        if (alert.getDialogPane().getScene() == null) {
            return;
        }
        String stylesheet = Objects.requireNonNull(getClass().getResource("/style.css"),
                "style.css resource is missing").toExternalForm();
        if (!alert.getDialogPane().getScene().getStylesheets().contains(stylesheet)) {
            alert.getDialogPane().getScene().getStylesheets().add(stylesheet);
        }
        alert.getDialogPane().getScene().setFill(Color.TRANSPARENT);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        styleDialogScene(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message);
        styleDialogScene(alert);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static List<StudySessionItem> sessionItems(ScheduleTemplate schedule) {
        List<StudySessionItem> items = new ArrayList<>();
        int studyOrder = 1;
        for (DayOfWeek day : DayOfWeek.values()) {
            for (StudyBlock block : schedule.blocks(day)) {
                items.add(StudySessionItem.study(studyOrder, block.focus(), displayTopic(block, studyOrder),
                        (int) block.duration().toMinutes()));
                if (block.breakAfterMinutes() > 0) {
                    items.add(StudySessionItem.breakItem(studyOrder, block.breakAfterMinutes()));
                }
                studyOrder++;
            }
        }
        return List.copyOf(items);
    }

    private static String displayTopic(StudyBlock block, int order) {
        String topic = block.topic();
        if (topic != null && !topic.matches(".*[ãáâéêíóôõúçÃÁÂÉÊÍÓÔÕÚÇ].*")) {
            return topic;
        }
        return block.focus().label() + " · Focus block " + String.format("%02d", order);
    }

    private static Path defaultPath() {
        String stateHome = System.getenv("XDG_STATE_HOME");
        if (stateHome == null || stateHome.isBlank()) {
            stateHome = System.getProperty("user.home") + "/.local/state";
        }
        return Path.of(stateHome, "livara", "study-schedule.json");
    }

}
