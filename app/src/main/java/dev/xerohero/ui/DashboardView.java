package dev.xerohero.ui;

import javafx.beans.value.ChangeListener;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import dev.xerohero.log.LogEntry;

/**
 * The Layout Engine for the TimberStrata UI.
 * Purely instantiates visual Nodes and UI layout hierarchies. Contains zero business logic.
 */
public class DashboardView extends BorderPane {

    private TableView<LogEntry> table;
    private TableColumn<LogEntry, String> colTimestamp;
    private TableColumn<LogEntry, String> colLevel;
    private TableColumn<LogEntry, String> colMsg;

    private Button chooseFolderBtn;
    private Button toggleScrollBtn;
    private Button manualInspectBtn;
    private Button addTagBtn;
    private ToggleButton themeToggleBtn;

    private Button restartContainerBtn;
    private Button stopContainerBtn;
    private Label dockerStatusLabel;

    private TextField searchField;
    private CheckBox regexToggle;
    private TextField customTagField;

    private Label activeFileLabel;
    private Label errorCountLabel;
    private FlowPane metricsFlowPane;

    public DashboardView() {
        this.getStyleClass().add("root");
    }

    public void initializeLayout(SortedList<LogEntry> sortedData) {
        // 1. Top Panel Action Layout
        chooseFolderBtn = new Button("📁 Choose Folder");
        chooseFolderBtn.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold;");

        activeFileLabel = new Label("Monitoring: No active stream");
        activeFileLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        searchField = new TextField();
        searchField.setPromptText("🔍 Filter visible lines...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        regexToggle = new CheckBox("Use RegEx");
        regexToggle.setStyle("-fx-font-weight: bold;");

        themeToggleBtn = new ToggleButton("🌙 Dark Mode");
        themeToggleBtn.setStyle("-fx-font-weight: bold;");

        HBox topActionBar = new HBox(15, chooseFolderBtn, activeFileLabel, searchField, regexToggle, themeToggleBtn);
        topActionBar.setPadding(new Insets(15));
        topActionBar.setAlignment(Pos.CENTER_LEFT);
        topActionBar.getStyleClass().add("action-bar");
        topActionBar.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");
        this.setTop(topActionBar);

        // 2. Central Table View Configuration
        table = new TableView<>();
        table.setPlaceholder(new Label("No logs streaming into pipeline buffer. Select a target workspace directory."));
        table.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px;");

        colTimestamp = new TableColumn<>("Timestamp");
        colTimestamp.setCellValueFactory(cellData -> cellData.getValue().timestampProperty());
        colTimestamp.setPrefWidth(160);

        colLevel = new TableColumn<>("Level");
        colLevel.setCellValueFactory(cellData -> cellData.getValue().levelProperty());
        colLevel.setPrefWidth(90);

        colMsg = new TableColumn<>("Message Pipeline Frame");
        colMsg.setCellValueFactory(cellData -> cellData.getValue().messageProperty());
        colMsg.setPrefWidth(650);

        colMsg.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                } else {
                    String lineToDisplay = item.contains("\n") ? item.split("\n")[0] + "  [🔍 Multiline]" : item;
                    TextFlow colorizedLine = LogStyleEngine.tokenizeAndColorize(lineToDisplay);

                    colorizedLine.setMaxHeight(24);
                    colorizedLine.setPrefHeight(24);

                    this.setMaxHeight(28);
                    this.setPrefHeight(28);

                    setGraphic(colorizedLine);
                    setText(null);
                }
            }
        });

        table.getColumns().addAll(colTimestamp, colLevel, colMsg);
        table.setItems(sortedData);
        VBox.setVgrow(table, Priority.ALWAYS);

        // 3. Central Control Bar
        toggleScrollBtn = new Button("🔄 Auto-Follow: ON");
        toggleScrollBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");

        manualInspectBtn = new Button("🔍 Inspect Frame (SPACE)");
        manualInspectBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox centralControlStrip = new HBox(10, toggleScrollBtn, manualInspectBtn);
        centralControlStrip.setPadding(new Insets(10, 0, 0, 0));
        centralControlStrip.setAlignment(Pos.CENTER_LEFT);

        VBox centralContainer = new VBox(5, table, centralControlStrip);
        centralContainer.setPadding(new Insets(15));
        this.setCenter(centralContainer);

        // 4. Right Side Telemetry Panel
        VBox rightSidebar = new VBox(15);
        rightSidebar.setPadding(new Insets(15));
        rightSidebar.setPrefWidth(240);
        rightSidebar.getStyleClass().add("sidebar");
        rightSidebar.setStyle("-fx-border-color: #eaeded; -fx-border-width: 0 0 0 1;");

        Label metricsHeader = new Label("SYSTEM TELEMETRY");
        metricsHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        errorCountLabel = new Label("🚨 Errors: 0");
        errorCountLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");

        Separator sep1 = new Separator();

        Label tagHeader = new Label("CUSTOM LOG WATCHER");
        tagHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        customTagField = new TextField();
        customTagField.setPromptText("Enter custom token (e.g. KAFKA)");

        addTagBtn = new Button("➕ Register Metric Tag");
        addTagBtn.setMaxWidth(Double.MAX_VALUE);
        addTagBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");

        metricsFlowPane = new FlowPane();
        metricsFlowPane.setHgap(8);
        metricsFlowPane.setVgap(8);
        metricsFlowPane.setPrefWidth(210);

        Separator sep2 = new Separator();

        Label dockerHeader = new Label("ENGINE LIFECYCLE");
        dockerHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-letter-spacing: 1px;");

        dockerStatusLabel = new Label("🟢 RUNNING");
        dockerStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");

        restartContainerBtn = new Button("🔄 Restart Engine Context");
        restartContainerBtn.setMaxWidth(Double.MAX_VALUE);
        restartContainerBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");

        stopContainerBtn = new Button("🛑 Kill Local Cluster");
        stopContainerBtn.setMaxWidth(Double.MAX_VALUE);
        stopContainerBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;");

        rightSidebar.getChildren().addAll(
                metricsHeader, errorCountLabel, sep1,
                tagHeader, customTagField, addTagBtn, metricsFlowPane,
                sep2, dockerHeader, dockerStatusLabel, restartContainerBtn, stopContainerBtn
        );
        this.setRight(rightSidebar);
    }

    public void renderCustomMetricCard(String tag, int initialValue, ChangeListener<Number> dynamicListener) {
        Label metricBadge = new Label("🏷️ " + tag + ": " + initialValue);
        metricBadge.setStyle("-fx-background-color: #ebdef0; -fx-text-fill: #5b2c6f; -fx-font-weight: bold; -fx-padding: 6 12; -fx-background-radius: 4; -fx-font-size: 12px;");
        metricBadge.setId("metric-tag-" + tag);
        metricsFlowPane.getChildren().add(metricBadge);
    }

    // Accessors
    public TableView<LogEntry> getTable() { return table; }
    public Button getChooseFolderBtn() { return chooseFolderBtn; }
    public Button getToggleScrollBtn() { return toggleScrollBtn; }
    public Button getManualInspectBtn() { return manualInspectBtn; }
    public Button getAddTagBtn() { return addTagBtn; }

    // 🏆 FIXED: Explicit theme toggle access hook restored to public boundary map
    public ToggleButton getThemeToggleBtn() { return themeToggleBtn; }

    public Button getRestartContainerBtn() { return restartContainerBtn; }
    public Button getStopContainerBtn() { return stopContainerBtn; }
    public Label getDockerStatusLabel() { return dockerStatusLabel; }
    public TextField getSearchField() { return searchField; }
    public CheckBox getRegexToggle() { return regexToggle; }
    public TextField getCustomTagField() { return customTagField; }
    public Label getActiveFileLabel() { return activeFileLabel; }
    public Label getErrorCountLabel() { return errorCountLabel; }
    public FlowPane getMetricsFlowPane() { return metricsFlowPane; }
}