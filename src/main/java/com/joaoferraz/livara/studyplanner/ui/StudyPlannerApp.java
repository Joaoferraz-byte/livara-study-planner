package com.joaoferraz.livara.studyplanner.ui;

import com.joaoferraz.livara.studyplanner.domain.Cycle;
import com.joaoferraz.livara.studyplanner.domain.DefaultScheduleFactory;
import com.joaoferraz.livara.studyplanner.domain.FocusArea;
import com.joaoferraz.livara.studyplanner.domain.ScheduleService;
import com.joaoferraz.livara.studyplanner.domain.ScheduleTemplate;
import com.joaoferraz.livara.studyplanner.domain.StudyBlock;
import com.joaoferraz.livara.studyplanner.io.ScheduleStore;
import javafx.application.Application;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StudyPlannerApp extends Application {
    private static final Pattern COLOR = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"(#[0-9a-fA-F]{6})\\\"");
    private static Path requestedPath;

    private final ScheduleStore store = new ScheduleStore();
    private final ScheduleService service = new ScheduleService(store);
    private final ObservableList<ScheduleRow> rows = FXCollections.observableArrayList();
    private final TableView<ScheduleRow> table = new TableView<>(rows);
    private final Label cycleLabel = new Label();
    private final Label statusLabel = new Label();
    private ScheduleTemplate current;
    private Path schedulePath;

    public static void launchWithPath(Path path) {
        requestedPath = Objects.requireNonNull(path, "path").toAbsolutePath();
        launch();
    }

    @Override
    public void start(Stage stage) {
        schedulePath = requestedPath == null ? defaultPath() : requestedPath;
        try {
            current = service.loadOrCreate(schedulePath);
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível carregar o cronograma", exception.getMessage());
            current = DefaultScheduleFactory.create(Cycle.A);
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        applyMatugenPalette(root);
        root.setPadding(new Insets(22));

        Label title = new Label("Livara Study Planner");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Cronograma reutilizável · blocos de 60 min · pausas de 15 min");
        subtitle.getStyleClass().add("subtitle");
        cycleLabel.getStyleClass().add("cycle");
        VBox heading = new VBox(5, title, subtitle, cycleLabel);
        heading.setPadding(new Insets(0, 0, 15, 0));

        configureTable();
        reloadRows();

        Button add = new Button("Adicionar bloco");
        add.setOnAction(event -> addBlock());
        Button remove = new Button("Remover selecionado");
        remove.setOnAction(event -> removeSelected());
        Button validate = new Button("Validar");
        validate.setOnAction(event -> validateCurrent());
        Button save = new Button("Salvar");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> saveCurrent());
        Button next = new Button("Avançar ciclo A/B");
        next.setOnAction(event -> advanceCycle());
        HBox actions = new HBox(8, add, remove, validate, save, next);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(15, 0, 8, 0));

        statusLabel.getStyleClass().add("status");
        statusLabel.setText(schedulePath.toString());
        VBox footer = new VBox(4, actions, statusLabel);

        root.setTop(heading);
        root.setCenter(table);
        root.setBottom(footer);

        Scene scene = new Scene(root, 1120, 720);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/style.css"), "style.css resource is missing").toExternalForm());
        stage.setTitle("Livara Study Planner");
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    private void configureTable() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("Nenhum bloco configurado. Use Adicionar bloco."));
        table.getStyleClass().add("schedule-table");

        TableColumn<ScheduleRow, String> day = new TableColumn<>("Dia");
        day.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().day().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.forLanguageTag("pt-BR"))));
        day.setPrefWidth(145);
        day.setSortable(false);

        TableColumn<ScheduleRow, String> order = new TableColumn<>("#");
        order.setCellValueFactory(cell -> new SimpleStringProperty(Integer.toString(cell.getValue().order())));
        order.setMaxWidth(55);
        order.setSortable(false);

        TableColumn<ScheduleRow, String> focus = new TableColumn<>("Área");
        focus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().focus().label()));
        focus.setPrefWidth(220);
        focus.setSortable(false);

        TableColumn<ScheduleRow, String> topic = new TableColumn<>("Tópico / atividade");
        topic.setCellValueFactory(cell -> cell.getValue().topicProperty());
        topic.setCellFactory(TextFieldTableCell.forTableColumn());
        topic.setOnEditCommit(event -> event.getRowValue().setTopic(event.getNewValue()));
        topic.setSortable(false);

        TableColumn<ScheduleRow, String> duration = new TableColumn<>("Duração");
        duration.setCellValueFactory(cell -> new SimpleStringProperty("60 min"));
        duration.setMaxWidth(95);
        duration.setSortable(false);

        TableColumn<ScheduleRow, String> pause = new TableColumn<>("Pausa depois");
        pause.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().breakAfterMinutes() == 0 ? "—" : cell.getValue().breakAfterMinutes() + " min"));
        pause.setMaxWidth(125);
        pause.setSortable(false);

        table.getColumns().setAll(day, order, focus, topic, duration, pause);
    }

    private void reloadRows() {
        rows.clear();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (StudyBlock block : current.blocks(day)) {
                rows.add(new ScheduleRow(day, block.order(), block.focus(), block.topic(), block.breakAfterMinutes()));
            }
        }
        cycleLabel.setText(current.cycle().label() + " · " + current.cycle().subjects() + " · " + current.totalBlocks() + " blocos");
        statusLabel.setText("Arquivo: " + schedulePath + " · alterações são salvas explicitamente");
    }

    private ScheduleTemplate buildTemplateFromRows() {
        EnumMap<DayOfWeek, List<StudyBlock>> days = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            List<ScheduleRow> dayRows = rows.stream().filter(row -> row.day() == day).toList();
            List<StudyBlock> blocks = new ArrayList<>();
            for (int index = 0; index < dayRows.size(); index++) {
                ScheduleRow row = dayRows.get(index);
                int pause = index + 1 < dayRows.size() ? current.pauseMinutes() : 0;
                blocks.add(new StudyBlock(index + 1, row.focus(), row.topic(), java.time.Duration.ofHours(1), pause));
            }
            days.put(day, List.copyOf(blocks));
        }
        return new ScheduleTemplate(current.schemaVersion(), current.name(), current.cycle(), current.pauseMinutes(), days);
    }

    private void saveCurrent() {
        try {
            current = buildTemplateFromRows();
            service.save(schedulePath, current);
            reloadRows();
            statusLabel.setText("Salvo e validado: " + schedulePath);
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível salvar o cronograma", exception.getMessage());
        }
    }

    private void validateCurrent() {
        try {
            current = buildTemplateFromRows();
            List<String> errors = service.validate(current);
            if (errors.isEmpty()) {
                showInfo("Cronograma válido", "Todos os blocos seguem o contrato de 60 minutos e a pausa de 15 minutos.");
            } else {
                showError("Cronograma precisa de revisão", String.join("\n", errors));
            }
        } catch (RuntimeException exception) {
            showError("Cronograma inválido", exception.getMessage());
        }
    }

    private void advanceCycle() {
        try {
            current = service.advanceCycle(buildTemplateFromRows());
            service.save(schedulePath, current);
            reloadRows();
            showInfo("Ciclo avançado", current.cycle().label() + " selecionado e salvo.");
        } catch (IOException | RuntimeException exception) {
            showError("Não foi possível avançar o ciclo", exception.getMessage());
        }
    }

    private void addBlock() {
        Dialog<ScheduleRow> dialog = new Dialog<>();
        dialog.setTitle("Adicionar bloco de estudo");
        dialog.setHeaderText("O bloco será salvo como uma hora; a pausa é recalculada ao salvar.");
        ButtonType addType = new ButtonType("Adicionar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addType, ButtonType.CANCEL);

        ComboBox<DayOfWeek> day = new ComboBox<>(FXCollections.observableArrayList(DayOfWeek.values()));
        day.setValue(DayOfWeek.MONDAY);
        ComboBox<FocusArea> focus = new ComboBox<>(FXCollections.observableArrayList(FocusArea.values()));
        focus.setValue(FocusArea.MARKET_PROGRAMMING);
        TextField topic = new TextField();
        topic.setPromptText("Ex.: Java empresarial");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        form.addRow(0, new Label("Dia"), day);
        form.addRow(1, new Label("Área"), focus);
        form.addRow(2, new Label("Tópico"), topic);
        dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == addType && !topic.getText().isBlank()
                ? new ScheduleRow(day.getValue(), 0, focus.getValue(), topic.getText().trim(), 0) : null);

        Optional<ScheduleRow> result = dialog.showAndWait();
        result.ifPresent(row -> {
            int insertAt = rows.size();
            for (int index = 0; index < rows.size(); index++) {
                if (rows.get(index).day().getValue() > row.day().getValue()) {
                    insertAt = index;
                    break;
                }
            }
            rows.add(insertAt, row);
            statusLabel.setText("Bloco adicionado; clique em Salvar para persistir.");
        });
    }

    private void removeSelected() {
        ScheduleRow selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Nenhum bloco selecionado", "Selecione um bloco na tabela antes de remover.");
            return;
        }
        rows.remove(selected);
        statusLabel.setText("Bloco removido; clique em Salvar para persistir.");
    }

    private void applyMatugenPalette(BorderPane root) {
        Path palette = Path.of(System.getenv().getOrDefault(
                "LIVARA_THEME_ROOT", System.getProperty("user.home") + "/.local/state/livara/theme")
        ).resolve("palette.dark.json");
        String base = paletteColor(palette, "base", "#17181d");
        String text = paletteColor(palette, "text", "#e5e1e9");
        String primary = paletteColor(palette, "primary", "#b8c8ff");
        String surface = paletteColor(palette, "surface0", "#24252c");
        root.setStyle("-fx-base: " + base + "; -fx-accent: " + primary + "; -fx-focus-color: " + primary + "; -fx-faint-focus-color: transparent; -fx-text-base-color: " + text + "; -planner-surface: " + surface + ";");
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
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK) {{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private void showError(String title, String message) {
        new Alert(Alert.AlertType.ERROR, message == null ? "Erro sem detalhes" : message, ButtonType.OK) {{ setTitle(title); setHeaderText(null); }}.showAndWait();
    }

    private static Path defaultPath() {
        String stateHome = System.getenv("XDG_STATE_HOME");
        if (stateHome == null || stateHome.isBlank()) {
            stateHome = System.getProperty("user.home") + "/.local/state";
        }
        return Path.of(stateHome, "livara", "study-schedule.json");
    }

    private static final class ScheduleRow {
        private final DayOfWeek day;
        private final int order;
        private final FocusArea focus;
        private final SimpleStringProperty topic;
        private final int breakAfterMinutes;

        private ScheduleRow(DayOfWeek day, int order, FocusArea focus, String topic, int breakAfterMinutes) {
            this.day = Objects.requireNonNull(day, "day");
            this.order = order;
            this.focus = Objects.requireNonNull(focus, "focus");
            this.topic = new SimpleStringProperty(topic);
            this.breakAfterMinutes = breakAfterMinutes;
        }

        private DayOfWeek day() { return day; }
        private int order() { return order; }
        private FocusArea focus() { return focus; }
        private SimpleStringProperty topicProperty() { return topic; }
        private String topic() { return topic.get(); }
        private void setTopic(String value) { topic.set(value == null ? "" : value.trim()); }
        private int breakAfterMinutes() { return breakAfterMinutes; }
    }
}
