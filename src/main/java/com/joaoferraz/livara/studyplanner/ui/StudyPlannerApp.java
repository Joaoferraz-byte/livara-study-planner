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
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudyPlannerApp extends Application {
    private static final Pattern COLOR = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"(#[0-9a-fA-F]{6})\\\"");
    private static final List<FolderShortcut> VAULT_FOLDERS = List.of(
            new FolderShortcut("󰉋", "Black Box", "00 - Black Box"),
            new FolderShortcut("󰈙", "Source Notes", "01 - Source Notes"),
            new FolderShortcut("󰏗", "Projects", "02 - Projects"),
            new FolderShortcut("󰃭", "Daily Notes", "03 - Daily Notes"),
            new FolderShortcut("󰐕", "Xournal++", "04 - Xournal++"),
            new FolderShortcut("󰂺", "References", "05 - References")
    );

    private static Path requestedPath;

    private final ScheduleStore store = new ScheduleStore();
    private final ScheduleService service = new ScheduleService(store);
    private final ProgressStore progressStore = new ProgressStore();
    private final VBox sessionList = new VBox(10);
    private final Label cycleLabel = new Label();
    private final Label progressLabel = new Label();
    private final Label statusLabel = new Label();
    private final Label focusDescription = new Label();
    private final ProgressBar progressBar = new ProgressBar(0);
    private final ComboBox<WorkflowTemplate> templateSelector = new ComboBox<>();
    private boolean updatingTemplateSelector;
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
            showError("Não foi possível carregar o cronograma", exception.getMessage());
            current = DefaultScheduleFactory.create(Cycle.A, WorkflowTemplate.MARKET_PROGRAMMING);
            progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        applyMatugenPalette(root);
        root.setPadding(new Insets(26, 30, 20, 30));

        root.setTop(buildHeader());
        root.setCenter(buildSessionViewport());
        root.setRight(buildSidePanel());
        root.setBottom(buildFooter());
        renderSession();

        Scene scene = new Scene(root, 1240, 820);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css"), "style.css resource is missing").toExternalForm());
        stage.setTitle("Livara Study Planner");
        stage.setMinWidth(980);
        stage.setMinHeight(660);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildHeader() {
        Label eyebrow = new Label("LIVARA  /  ESTUDO INTENCIONAL");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("Sua próxima sessão, com clareza.");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Um fluxo de foco e pausa que se adapta ao seu ciclo — sem uma tabela para administrar.");
        subtitle.getStyleClass().add("subtitle");

        templateSelector.setItems(FXCollections.observableArrayList(WorkflowTemplate.values()));
        templateSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(WorkflowTemplate template) {
                return template == null ? "" : template.label();
            }

            @Override
            public WorkflowTemplate fromString(String value) {
                return templateSelector.getValue();
            }
        });
        templateSelector.setOnAction(event -> selectWorkflow(templateSelector.getValue()));
        templateSelector.setPrefWidth(260);
        Label templateLabel = new Label("Fluxo");
        templateLabel.getStyleClass().add("field-label");
        HBox templatePicker = new HBox(10, templateLabel, templateSelector);
        templatePicker.setAlignment(Pos.CENTER_LEFT);

        MenuButton menu = new MenuButton("󰍜  Menu");
        menu.getStyleClass().add("secondary-button");
        MenuItem reload = new MenuItem("Recarregar agenda");
        reload.setOnAction(event -> reloadSchedule());
        MenuItem validate = new MenuItem("Validar contrato");
        validate.setOnAction(event -> validateCurrent());
        MenuItem cycleA = new MenuItem("Aplicar ciclo A");
        cycleA.setOnAction(event -> setCycle(Cycle.A));
        MenuItem cycleB = new MenuItem("Aplicar ciclo B");
        cycleB.setOnAction(event -> setCycle(Cycle.B));
        MenuItem reset = new MenuItem("Limpar progresso da sessão");
        reset.setOnAction(event -> resetProgress());
        menu.getItems().addAll(reload, validate, cycleA, cycleB, reset);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(12, templatePicker, spacer, menu);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setPadding(new Insets(18, 0, 22, 0));

        cycleLabel.getStyleClass().add("cycle-label");
        VBox header = new VBox(8, eyebrow, title, subtitle, cycleLabel, controls);
        header.getStyleClass().add("page-header");
        return header;
    }

    private ScrollPane buildSessionViewport() {
        Label sectionTitle = new Label("Fluxo de hoje");
        sectionTitle.getStyleClass().add("section-title");
        Label sectionHint = new Label("Conclua cada foco e pausa; o próximo item fica destacado automaticamente.");
        sectionHint.getStyleClass().add("section-hint");
        VBox sectionHeader = new VBox(4, sectionTitle, sectionHint);

        sessionList.getStyleClass().add("session-list");
        VBox content = new VBox(18, sectionHeader, sessionList);
        content.getStyleClass().add("session-content");
        content.setPadding(new Insets(0, 22, 0, 0));
        VBox.setVgrow(sessionList, Priority.ALWAYS);

        ScrollPane viewport = new ScrollPane(content);
        viewport.setFitToWidth(true);
        viewport.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        viewport.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        viewport.getStyleClass().add("session-viewport");
        return viewport;
    }

    private VBox buildSidePanel() {
        Label today = new Label("HOJE");
        today.getStyleClass().add("panel-eyebrow");
        progressLabel.getStyleClass().add("progress-number");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().add("progress-bar");
        Label rhythm = new Label("Ritmo recomendado");
        rhythm.getStyleClass().add("panel-label");
        Label rhythmValue = new Label("60 min foco  ·  15 min pausa");
        rhythmValue.getStyleClass().add("panel-value");
        focusDescription.setWrapText(true);
        focusDescription.getStyleClass().add("panel-description");

        VBox progressPanel = new VBox(10, today, progressLabel, progressBar, rhythm, rhythmValue, focusDescription);
        progressPanel.getStyleClass().add("side-card");

        Label vaultTitle = new Label("Vault");
        vaultTitle.getStyleClass().add("panel-title");
        Label vaultHint = new Label("Acesse suas notas e projetos no Oil do Neovim.");
        vaultHint.setWrapText(true);
        vaultHint.getStyleClass().add("panel-description");
        Button openVault = new Button("󰈙  Abrir Vault no Neovim");
        openVault.getStyleClass().add("primary-button");
        openVault.setMaxWidth(Double.MAX_VALUE);
        openVault.setOnAction(event -> openVaultInNvim());

        VBox folders = new VBox(5);
        folders.getStyleClass().add("folder-list");
        for (FolderShortcut folder : VAULT_FOLDERS) {
            Button shortcut = new Button(folder.glyph() + "  " + folder.label());
            shortcut.getStyleClass().add("folder-link");
            shortcut.setMaxWidth(Double.MAX_VALUE);
            shortcut.setAlignment(Pos.CENTER_LEFT);
            shortcut.setOnAction(event -> openVaultFolderInNvim(folder.relativePath()));
            folders.getChildren().add(shortcut);
        }
        VBox vaultPanel = new VBox(12, vaultTitle, vaultHint, openVault, new Separator(), folders);
        vaultPanel.getStyleClass().add("side-card");

        VBox side = new VBox(16, progressPanel, vaultPanel);
        side.getStyleClass().add("side-panel");
        side.setPrefWidth(290);
        side.setPadding(new Insets(0, 0, 0, 8));
        return side;
    }

    private HBox buildFooter() {
        statusLabel.getStyleClass().add("status");
        HBox footer = new HBox(statusLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(18, 0, 0, 0));
        return footer;
    }

    private void renderSession() {
        updatingTemplateSelector = true;
        templateSelector.setValue(current.workflowTemplate());
        updatingTemplateSelector = false;
        sessionItems = sessionItems(current);
        sessionList.getChildren().clear();
        for (int index = 0; index < sessionItems.size(); index++) {
            StudySessionItem item = sessionItems.get(index);
            sessionList.getChildren().add(item.pause() ? pauseCard(item, index) : studyCard(item, index));
        }

        long totalStudy = sessionItems.stream().filter(item -> !item.pause()).count();
        long completedStudy = sessionItems.stream()
                .filter(item -> !item.pause() && progress.isCompleted(item.id()))
                .count();
        double ratio = totalStudy == 0 ? 0 : (double) completedStudy / totalStudy;
        progressLabel.setText(completedStudy + " / " + totalStudy + " blocos concluídos");
        progressBar.setProgress(ratio);
        cycleLabel.setText(current.workflowTemplate().label() + "  ·  " + current.cycle().label()
                + "  ·  " + totalStudy + " focos + pausas");
        focusDescription.setText(current.workflowTemplate().description());
        statusLabel.setText("Salvamento automático  ·  " + schedulePath);
    }

    private TitledPane studyCard(StudySessionItem item, int index) {
        CheckBox done = new CheckBox();
        done.setSelected(progress.isCompleted(item.id()));
        done.setOnAction(event -> toggleItem(item));
        done.getStyleClass().add("item-check");

        Label glyph = new Label(item.glyph());
        glyph.getStyleClass().add("item-glyph");
        Label order = new Label(String.format("%02d", item.order()));
        order.getStyleClass().add("item-order");
        VBox indexBox = new VBox(2, order, glyph);
        indexBox.setAlignment(Pos.CENTER);
        indexBox.setMinWidth(42);

        Label title = new Label(item.title());
        title.getStyleClass().add("item-title");
        Label subtitle = new Label(item.subtitle());
        subtitle.getStyleClass().add("item-subtitle");
        VBox copy = new VBox(5, title, subtitle);
        HBox.setHgrow(copy, Priority.ALWAYS);

        Label duration = new Label(item.durationMinutes() + " min");
        duration.getStyleClass().add("duration-chip");
        HBox card = new HBox(14, done, indexBox, copy, duration);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("session-card", "study-card");
        boolean completed = progress.isCompleted(item.id());
        if (completed) {
            card.getStyleClass().add("completed");
        }
        if (isActive(index)) {
            card.getStyleClass().add("active");
        }

        Label detail = new Label("Foco: " + item.focus().label() + "  ·  "
                + item.durationMinutes() + " min  ·  pausa seguinte: 15 min");
        detail.getStyleClass().add("item-detail");
        TitledPane pane = new TitledPane("", detail);
        pane.setGraphic(card);
        pane.setExpanded(!completed);
        pane.setAnimated(false);
        pane.getStyleClass().add("session-expander");
        if (completed) {
            pane.getStyleClass().add("completed");
        }
        return pane;
    }

    private HBox pauseCard(StudySessionItem item, int index) {
        CheckBox done = new CheckBox();
        done.setSelected(progress.isCompleted(item.id()));
        done.setOnAction(event -> toggleItem(item));
        done.getStyleClass().add("item-check");

        Label glyph = new Label(item.glyph());
        glyph.getStyleClass().add("pause-glyph");
        VBox copy = new VBox(3, new Label(item.title()), new Label(item.subtitle()));
        copy.getStyleClass().add("pause-copy");
        HBox.setHgrow(copy, Priority.ALWAYS);
        Label duration = new Label(item.durationMinutes() + " min");
        duration.getStyleClass().add("duration-chip");
        HBox card = new HBox(14, done, glyph, copy, duration);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("session-card", "pause-card");
        if (progress.isCompleted(item.id())) {
            card.getStyleClass().add("completed");
        }
        if (isActive(index)) {
            card.getStyleClass().add("active");
        }
        return card;
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
        progress = progress.toggle(item.id()).withActiveItemIndex(nextPendingIndex());
        if (allSessionItemsCompleted()) {
            advanceCycleAutomatically();
            return;
        }
        persistProgress();
        renderSession();
    }

    private boolean allSessionItemsCompleted() {
        return !sessionItems.isEmpty()
                && sessionItems.stream().allMatch(item -> progress.isCompleted(item.id()));
    }

    private void selectWorkflow(WorkflowTemplate workflowTemplate) {
        if (updatingTemplateSelector || workflowTemplate == null
                || current.workflowTemplate() == workflowTemplate) {
            return;
        }
        current = DefaultScheduleFactory.create(current.cycle(), workflowTemplate);
        progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        persistSchedule();
        persistProgress();
        statusLabel.setText("Fluxo " + workflowTemplate.label() + " aplicado e salvo automaticamente.");
        renderSession();
    }

    private void setCycle(Cycle cycle) {
        if (current.cycle() == cycle) {
            statusLabel.setText("O " + cycle.label() + " já está ativo.");
            return;
        }
        current = DefaultScheduleFactory.create(cycle, current.workflowTemplate());
        progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        persistSchedule();
        persistProgress();
        statusLabel.setText("" + cycle.label() + " aplicado automaticamente.");
        renderSession();
    }

    private void advanceCycleAutomatically() {
        try {
            current = service.advanceCycle(current);
            progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
            persistSchedule();
            persistProgress();
            renderSession();
            statusLabel.setText("Sessão concluída. " + current.cycle().label() + " foi ativado automaticamente.");
        } catch (RuntimeException exception) {
            showError("Não foi possível mudar o ciclo", exception.getMessage());
        }
    }

    private void persistSchedule() {
        try {
            service.save(schedulePath, current);
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível salvar a agenda", exception.getMessage());
        }
    }

    private void persistProgress() {
        try {
            progressStore.save(progressPath, progress);
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível salvar o progresso", exception.getMessage());
        }
    }

    private void reloadSchedule() {
        try {
            current = service.loadOrCreate(schedulePath);
            progress = progressStore.loadOrEmpty(progressPath, current.cycle(), current.workflowTemplate());
            renderSession();
            statusLabel.setText("Agenda recarregada.");
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível recarregar a agenda", exception.getMessage());
        }
    }

    private void resetProgress() {
        progress = ProgressState.empty(current.cycle(), current.workflowTemplate());
        persistProgress();
        renderSession();
        statusLabel.setText("Progresso da sessão limpo.");
    }

    private void validateCurrent() {
        List<String> errors = service.validate(current);
        if (errors.isEmpty()) {
            showInfo("Contrato válido", "A agenda segue o contrato de blocos de 60 minutos e pausas de 15 minutos.");
        } else {
            showError("Agenda precisa de revisão", String.join("\n", errors));
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
            statusLabel.setText("Pasta do Vault não encontrada: " + target);
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
                statusLabel.setText("Neovim aberto no Oil: " + target);
                return;
            } catch (IOException exception) {
                failure = exception;
            }
        }
        statusLabel.setText("Não foi possível abrir o Neovim: "
                + (failure == null ? "nenhum terminal disponível" : failure.getMessage()));
    }

    private static String vimEscape(String value) {
        return value.replace("\\\\", "\\\\\\\\")
                .replace(" ", "\\\\ ")
                .replace("|", "\\\\|")
                .replace("\"", "\\\\\"");
    }

    private void applyMatugenPalette(BorderPane root) {
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
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Erro sem detalhes" : message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static List<StudySessionItem> sessionItems(ScheduleTemplate schedule) {
        List<StudySessionItem> items = new ArrayList<>();
        int studyOrder = 1;
        for (DayOfWeek day : DayOfWeek.values()) {
            for (StudyBlock block : schedule.blocks(day)) {
                items.add(StudySessionItem.study(studyOrder, block.focus(), block.topic(),
                        (int) block.duration().toMinutes()));
                if (block.breakAfterMinutes() > 0) {
                    items.add(StudySessionItem.breakItem(studyOrder, block.breakAfterMinutes()));
                }
                studyOrder++;
            }
        }
        return List.copyOf(items);
    }

    private static Path defaultPath() {
        String stateHome = System.getenv("XDG_STATE_HOME");
        if (stateHome == null || stateHome.isBlank()) {
            stateHome = System.getProperty("user.home") + "/.local/state";
        }
        return Path.of(stateHome, "livara", "study-schedule.json");
    }

    private record FolderShortcut(String glyph, String label, String relativePath) {
    }
}
