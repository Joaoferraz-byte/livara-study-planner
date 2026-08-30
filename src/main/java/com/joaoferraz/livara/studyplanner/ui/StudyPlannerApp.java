package com.joaoferraz.livara.studyplanner.ui;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.ProgressState;
import com.joaoferraz.livara.studyplanner.domain.ScheduleService;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.StudyBlock;
import com.joaoferraz.livara.studyplanner.domain.WorkflowTemplate;
import com.joaoferraz.livara.studyplanner.io.ProgressStore;
import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.util.Duration;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudyPlannerApp extends Application {
    private static final Pattern COLOR = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"(#[0-9a-fA-F]{6})\\\"");
    private static final double WIDE_BREAKPOINT = 1040;
    private static final List<String> VAULT_FOLDERS = List.of(
            "Black Box", "Source Notes", "Projects", "Daily Notes", "Xournal++", "References");

    private static Path requestedPath;

    private final ScheduleStore store = new ScheduleStore();
    private final ScheduleService service = new ScheduleService(store);
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
    private final Arc progressArc = new Arc();
    private final Label progressRingValue = new Label();
    private final MenuButton menu = new MenuButton("Menu");
    private SVGPath menuIcon;
    private boolean compactLayout;
    private ScheduleTemplate current;
    private ProgressState progress;
    private List<StudySessionItem> sessionItems = List.of();
    private Path schedulePath;
    private Path progressPath;

    public static void launchWithPath(Path path) {
        requestedPath = Objects.requireNonNull(path, "path").toAbsolutePath();
        launch();
    }

    @Override
    public void start(Stage stage) {
        schedulePath = requestedPath == null ? defaultPath() : requestedPath;
        progressPath = schedulePath.resolveSibling(schedulePath.getFileName() + ".progress.properties");
        try {
            current = service.loadOrCreate(schedulePath);
            progress = progressStore.loadOrEmpty(progressPath, current.cycle(), current.workflowTemplate());
        } catch (IOException | RuntimeException exception) {
            showError("Unable to load schedule", exception.getMessage());
            current = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
            progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        }

        StackPane sceneRoot = new StackPane();
        sceneRoot.getStyleClass().add("root");
        applyMatugenPalette(sceneRoot);
        Pane ambient = buildAmbientLayer();
        BorderPane contentRoot = new BorderPane();
        contentRoot.setPadding(new Insets(22, 26, 16, 26));
        contentRoot.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        StackPane.setAlignment(contentRoot, Pos.TOP_LEFT);
        VBox shell = new VBox(16, buildTopBar(), buildHero(), buildDashboardGrid(), buildFooter());
        shell.getStyleClass().add("dashboard-shell");
        VBox.setVgrow(dashboardGrid, Priority.ALWAYS);
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

        menu.setText("");
        menuIcon = TablerIcon.home();
        menuIcon.getStyleClass().add("menu-icon");
        menu.setGraphic(menuIcon);
        menu.setOnMouseEntered(event -> animateScale(menuIcon, compactLayout ? 0.9 : 1.12));
        menu.setOnMouseExited(event -> animateScale(menuIcon, compactLayout ? 0.78 : 1.0));
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

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(left, spacer, menu);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("app-bar");
        return bar;
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

        Circle track = new Circle(52);
        track.getStyleClass().add("progress-ring-track");
        progressArc.setCenterX(0);
        progressArc.setCenterY(0);
        progressArc.setRadiusX(52);
        progressArc.setRadiusY(52);
        progressArc.setStartAngle(90);
        progressArc.setLength(0);
        progressArc.setType(ArcType.OPEN);
        progressArc.getStyleClass().add("progress-ring-arc");
        progressRingValue.getStyleClass().add("progress-ring-value");
        progressRing.getChildren().setAll(track, progressArc, progressRingValue);
        progressRing.setMinSize(112, 112);
        progressRing.setPrefSize(112, 112);
        progressRing.setMaxSize(112, 112);
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
            case "Black Box" -> TablerIcon.book();
            case "Source Notes" -> TablerIcon.notes();
            case "Projects" -> TablerIcon.project();
            case "Daily Notes" -> TablerIcon.calendar();
            case "Xournal++" -> TablerIcon.palette();
            case "References" -> TablerIcon.book();
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
        sessionItems = sessionItems(current);
        sessionList.getChildren().clear();
        for (int index = 0; index < sessionItems.size(); index++) {
            StudySessionItem item = sessionItems.get(index);
            Node card = item.pause() ? pauseCard(item, index) : studyCard(item, index);
            card.setManaged(true);
            sessionList.getChildren().add(card);
        }

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
        statusLabel.setText("Auto-saved  ·  " + schedulePath);
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
        VBox detail = new VBox(10, chips, callout, tasks);
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
            boolean expanded = !completed && isActive(index);
            detail.setVisible(expanded);
            detail.setManaged(expanded);
            if (expanded) {
                card.getStyleClass().add("expanded");
            }
            card.setOnMouseClicked(event -> {
                if (!(event.getTarget() instanceof CircularCheckBox)) {
                    boolean visible = !detail.isVisible();
                    animateDetails(detail, visible);
                    card.getStyleClass().remove("expanded");
                    if (visible) {
                        card.getStyleClass().add("expanded");
                    }
                }
            });
        }
        card.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(card, Priority.NEVER);
        card.setOnMouseEntered(event -> animateScale(card, 1.006));
        card.setOnMouseExited(event -> animateScale(card, 1.0));
    }

    private void progressRingArc(double ratio) {
        double target = -360 * Math.max(0, Math.min(1, ratio));
        Timeline timeline = new Timeline(new KeyFrame(Duration.millis(360),
                new KeyValue(progressArc.lengthProperty(), target, Interpolator.EASE_BOTH)));
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
                new StudyTask("practice", "Practice", "Work through one representative problem or implementation."),
                new StudyTask("apply", "Apply", "Record one result, question, or decision for the next session."));
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
        current = DefaultScheduleFactory.create(cycle, current.workflowTemplate());
        progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        persistSchedule();
        persistProgress();
        renderSession();
    }

    private void advanceCycleAutomatically() {
        try {
            current = service.advanceCycle(current);
            progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
            persistSchedule();
            persistProgress();
            renderSession();
            statusLabel.setText("Session complete. Cycle " + current.cycle().label() + " is active.");
        } catch (RuntimeException exception) {
            showError("Unable to change cycle", exception.getMessage());
        }
    }

    private void persistSchedule() {
        try {
            service.save(schedulePath, current);
        } catch (IOException | RuntimeException exception) {
            showError("Unable to save schedule", exception.getMessage());
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
            current = service.loadOrCreate(schedulePath);
            progress = progressStore.loadOrEmpty(progressPath, current.cycle(), current.workflowTemplate());
            renderSession();
            statusLabel.setText("Schedule reloaded.");
        } catch (IOException | RuntimeException exception) {
            showError("Unable to reload schedule", exception.getMessage());
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unknown error" : message);
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
