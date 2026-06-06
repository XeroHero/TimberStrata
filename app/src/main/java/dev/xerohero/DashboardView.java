package dev.xerohero;

import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import static javafx.geometry.Pos.CENTER_LEFT;
import static javafx.scene.layout.Priority.ALWAYS;

public class DashboardView extends BorderPane {

    private Button startBtn, stopBtn, chooseFolderBtn, toggleScrollBtn, manualInspectBtn, addTagBtn;
    private Label engineStatusLabel, errorCountLabel, activeFileLabel;
    private TextField searchField, customTagField;
    private TableView<LogEntry> table;
    private VBox sidebarCardContainer;

    public void initializeLayout(SortedList<LogEntry> sortedData) {
        // --- Top Control Ribbon Header Layout ---
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.setAlignment(CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #2c3e50;");

        Label titleLabel = new Label("TimberStrata Engine");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        startBtn = new Button("▶ Start Engine");
        stopBtn = new Button("■ Stop Engine");
        engineStatusLabel = new Label("Status: Active");
        engineStatusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        activeFileLabel = new Label("Monitoring: No folder selected");
        activeFileLabel.setStyle("-fx-text-fill: #f1c40f; -fx-font-style: italic;");
        chooseFolderBtn = new Button("📁 Watch Folder...");
        toggleScrollBtn = new Button("🔄 Auto-Follow: ON");
        toggleScrollBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        topBar.getChildren().addAll(titleLabel, startBtn, stopBtn, engineStatusLabel, chooseFolderBtn, toggleScrollBtn, activeFileLabel);

        // --- Sidebar Summary Panel ---
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color: #ecf0f1;");

        sidebarCardContainer = new VBox(10);
        VBox errorCard = new VBox(5);
        errorCard.setPadding(new Insets(10));
        errorCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        errorCountLabel = new Label("🚨 Errors: 0");
        errorCountLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");
        errorCard.getChildren().add(errorCountLabel);
        sidebarCardContainer.getChildren().add(errorCard);

        VBox customTagBox = new VBox(8);
        customTagField = new TextField();
        customTagField.setPromptText("e.g., FATAL, WARN");
        addTagBtn = new Button("➕ Add Metric Card");
        addTagBtn.setMaxWidth(Double.MAX_VALUE);
        customTagBox.getChildren().addAll(new Label("Track Custom Tag:"), customTagField, addTagBtn);

        VBox quickInspectBox = new VBox(5);
        manualInspectBtn = new Button("🔍 Inspect Selected Row");
        manualInspectBtn.setMaxWidth(Double.MAX_VALUE);
        manualInspectBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        Label backupHint = new Label("Shortcut: Select line + Press SPACE");
        backupHint.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        quickInspectBox.getChildren().addAll(manualInspectBtn, backupHint);

        sidebar.getChildren().addAll(sidebarCardContainer, new Separator(), customTagBox, new Separator(), quickInspectBox);

        // --- Center Table Stream Grid Layout ---
        table = new TableView<>(sortedData);
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        TableColumn<LogEntry, Boolean> colMarked = new TableColumn<>("📌");
        colMarked.setCellValueFactory(d -> d.getValue().markedProperty());
        colMarked.setCellFactory(CheckBoxTableCell.forTableColumn(colMarked));
        colMarked.setEditable(true);
        colMarked.setSortable(true);

        TableColumn<LogEntry, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(d -> d.getValue().timestampProperty());
        colTime.setSortable(true);

        TableColumn<LogEntry, String> colService = new TableColumn<>("Level");
        colService.setCellValueFactory(d -> d.getValue().levelProperty());
        colService.setSortable(true);

        TableColumn<LogEntry, String> colMsg = new TableColumn<>("Message");
        colMsg.setCellValueFactory(d -> d.getValue().messageProperty());
        colMsg.setPrefWidth(550);
        colMsg.setSortable(true);
        colMsg.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.contains("\n") ? item.split("\n")[0] + "  [🔍 Multiline]" : item);
                }
            }
        });

        table.getColumns().addAll(colMarked, colTime, colService, colMsg);

        searchField = new TextField();
        searchField.setPromptText("🔍 Filter visible lines...");

        VBox centerLayout = new VBox(10, searchField, table);
        VBox.setVgrow(table, ALWAYS);
        centerLayout.setPadding(new Insets(10));

        this.setTop(topBar);
        this.setLeft(sidebar);
        this.setCenter(centerLayout);
    }

    public void renderCustomMetricCard(String tag, int initialValue, javafx.beans.value.ChangeListener<Number> listener) {
        VBox newCard = new VBox(5);
        newCard.setPadding(new Insets(10));
        newCard.setStyle("-fx-background-color: white; -fx-background-radius: 6; -fx-border-color: #bdc3c7; -fx-border-radius: 6;");
        Label newLabel = new Label("🏷️ " + tag + ": " + initialValue);
        newLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
        newCard.getChildren().add(newLabel);

        sidebarCardContainer.getChildren().add(newCard);
        customTagField.clear();
    }

    // --- Clean Getters for Event Mapping ---
    public TableView<LogEntry> getTable() { return table; }
    public Button getChooseFolderBtn() { return chooseFolderBtn; }
    public Button getToggleScrollBtn() { return toggleScrollBtn; }
    public Button getManualInspectBtn() { return manualInspectBtn; }
    public Button getAddTagBtn() { return addTagBtn; }
    public TextField getSearchField() { return searchField; }
    public TextField getCustomTagField() { return customTagField; }
    public Label getErrorCountLabel() { return errorCountLabel; }
    public Label getActiveFileLabel() { return activeFileLabel; }
    public Label getEngineStatusLabel() { return engineStatusLabel; }
}