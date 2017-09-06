package me.zeroeightsix.namefinder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import me.zeroeightsix.namefinder.control.AddPanel;
import me.zeroeightsix.namefinder.control.CharacterEditor;
import me.zeroeightsix.namefinder.control.ProxyPanel;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 086 on 2/09/2017.
 */
public class Namefinder extends Application {

    public static final String NAME = "Namefinder";
    public static boolean TIMEOUT;

    public static Namefinder INSTANCE;

    CharacterEditor editor;
    public static SpinnerValueFactory.IntegerSpinnerValueFactory minFactory;

    public static SpinnerValueFactory.IntegerSpinnerValueFactory maxFactory;
    public static ListView<String> nameView;

    public static Label possibilityLabel;

    public static ProgressBar progressBar;
    public static Label statusLabel;
    static VBox topContentPane;

    static ArrayList<String> freeNames = new ArrayList<>();
    static List<String> currentNames = new ArrayList<>();

    NameChecker nameChecker;
    AddPanel addPanel;
    ProxyPanel proxyPanel;
    public static Stage STAGE;

    public static void main(String[] args) {
        INSTANCE = new Namefinder();
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        STAGE = primaryStage;
        primaryStage.setTitle(NAME);

        VBox topPane = new VBox();

        topContentPane = new VBox();
        topContentPane.setFillWidth(true);
        topContentPane.setPadding(new Insets(10, 25, 0, 25));

        MenuBar menuBar = new MenuBar();
        Menu menu = new Menu("File");
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
            chooser.getExtensionFilters().add(extFilter);
            chooser.setTitle("Save file");
            File file = chooser.showSaveDialog(primaryStage);
            if (file != null) {
                new Thread(){
                    @Override
                    public void run() {
                        setStatus("Saving file");
                        ArrayList<String> list = new ArrayList<>(nameView.getItems());
                        Path out = Paths.get(file.toURI());
                        try {
                            Files.write(out, list, Charset.defaultCharset());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Saved file to " + file.getAbsolutePath());
                        setStatus("File saved!");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        clearStatus();
                    }
                }.start();
            }
        });
        menu.getItems().add(saveItem);
        menuBar.getMenus().add(menu);

        menu = new Menu("Control");

        MenuItem clearItem = new MenuItem("Clear");
        clearItem.setOnAction(event -> nameView.getItems().clear());
        menu.getItems().add(clearItem);

        MenuItem addItem = new MenuItem("Add");
        addItem.setOnAction(event -> addPanel.show());
        addItem.setAccelerator(KeyCombination.keyCombination("CTRL+INSERT"));
        menu.getItems().add(addItem);

        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(event -> {
            if (nameView.getSelectionModel().getSelectedItems().isEmpty()){
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid selection");
                alert.setHeaderText("Invalid selection");
                alert.setContentText("To remove items, you must select them first.");
                alert.show();
            }else{
                nameView.getItems().removeAll(nameView.getSelectionModel().getSelectedItems());
            }
        });
        removeItem.setAccelerator(KeyCombination.keyCombination("CTRL+DELETE"));
        menu.getItems().add(removeItem);

        menuBar.getMenus().add(menu);

        VBox settingPane = new VBox();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);

        Label userName = new Label("Amount");
        grid.add(userName, 0, 1);
        Spinner<Integer> amountSpinner = new Spinner<>();
        amountSpinner.setEditable(true);
        amountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1,100000));
        amountSpinner.getValueFactory().setValue(50);
        grid.add(amountSpinner, 1, 1);

        Button proxyButton = new Button("Proxies");
        grid.add(proxyButton, 2, 1);
        proxyButton.setOnAction(event -> {
            proxyPanel.show();
        });

        Label min = new Label("Min length");
        grid.add(min, 0, 2);
        Spinner<Integer> minSpinner = new Spinner<>();
        minSpinner.setEditable(true);
        minSpinner.setValueFactory((minFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,16)));
        minSpinner.getValueFactory().setValue(3);
        grid.add(minSpinner, 1, 2);

        Label max = new Label("Max length");
        grid.add(max, 2, 2);
        Spinner<Integer> maxSpinner = new Spinner<>();
        maxSpinner.setEditable(true);
        maxSpinner.setValueFactory((maxFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1,16)));
        maxSpinner.getValueFactory().setValue(6);
        grid.add(maxSpinner, 3, 2);

        minSpinner.valueProperty().addListener(event -> {
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) maxSpinner.getValueFactory()).setMin(minSpinner.getValue());
            CharacterEditor.calculateAndSetPossibilities();
        });
        maxSpinner.valueProperty().addListener(event -> {
            ((SpinnerValueFactory.IntegerSpinnerValueFactory) minSpinner.getValueFactory()).setMax(maxSpinner.getValue());
            CharacterEditor.calculateAndSetPossibilities();
        });

        grid.add(new Label("Characters"), 0, 3);
        Button editButton = new Button("Edit");
        editButton.setOnAction(event ->
            editor.show()
        );
        grid.add(editButton, 1, 3);

        Button generateButton = new Button("Generate");
        generateButton.setOnAction(event -> {
            showProgressBar();
            new Thread(() -> {
                CharacterEditor.flushNames(amountSpinner.getValue());
                hideProgressBar();
            }).start();
        });
        grid.add(generateButton, 3, 3);
        GridPane.setHalignment(generateButton, HPos.CENTER);
        Button startButton = new Button("Start");
        startButton.setOnAction(event -> {

            if (startButton.getText().equals("Start")){
                nameChecker = new NameChecker(new ArrayList<>(nameView.getItems()), new Runnable() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> startButton.setText("Start"));
                    }
                });
                nameChecker.start();
                startButton.setText("Stop");
            }else{
                nameChecker.running = false;
                startButton.setText("Start");
            }

        });
        grid.add(startButton, 3, 3);
        GridPane.setHalignment(startButton, HPos.RIGHT);
        settingPane.getChildren().add(grid);
        possibilityLabel = new Label("");
        settingPane.getChildren().add(possibilityLabel);
        statusLabel = new Label("");

        settingPane.getChildren().add(statusLabel);

        topContentPane.getChildren().add(settingPane);
        topPane.getChildren().addAll(menuBar, topContentPane);
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
//        topPane.getChildren().add(progressBar);

        StackPane centerPane = new StackPane();
        nameView = new ListView<>();
        nameView.setEditable(true);
        nameView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        nameView.setCellFactory(list -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                if (currentNames.contains(item))
                    if (TIMEOUT)
                        setStyle("-fx-background-color:red;");
                    else
                        setStyle("-fx-background-color:orange;");
                else if (freeNames.contains(item))
                    setStyle("-fx-background-color:lightgreen;");
                else
                    setStyle(null);
            }
        });

        centerPane.getChildren().add(nameView);
        centerPane.setPadding(new Insets(0,25,15,25));

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topPane);
        borderPane.setCenter(centerPane);

        Scene scene = new Scene(borderPane, 500, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaxWidth(515);
        primaryStage.setMinHeight(400);

        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Closing namefinder!");
            primaryStage.close();
            System.exit(0);
        });

        editor = new CharacterEditor();
        addPanel = new AddPanel();
        proxyPanel = new ProxyPanel();

        System.out.println("Window initiated");
    }

    public static void showProgressBar() {
        Platform.runLater(() -> {
            if (!topContentPane.getChildren().contains(progressBar))
                topContentPane.getChildren().add(progressBar);
        });
    }

    public static void hideProgressBar() {
        Platform.runLater(() -> {
            if (topContentPane.getChildren().contains(progressBar))
                topContentPane.getChildren().remove(progressBar);
        });
    }

    public static void setProgress(double progress) {
        progressBar.setProgress(progress);
    }

    public static void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    public static void clearStatus() {
        setStatus("");
    }

    public static class SimpleProxy {
        public String ip;
        public int port;

        public SimpleProxy(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        public boolean testConnection(int timeout) {
            try {
                InetAddress addr = InetAddress.getByName(ip);
                try {
                    return addr.isReachable(timeout);
                } catch (IOException e) {
                    return false;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
