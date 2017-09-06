package me.zeroeightsix.namefinder.control;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import me.zeroeightsix.namefinder.Namefinder;
import me.zeroeightsix.namefinder.ProxyChecker;

import java.util.ArrayList;

/**
 * Created by 086 on 3/09/2017.
 */
public class ProxyPanel extends Stage {

    public static ArrayList<Namefinder.SimpleProxy> proxyArrayList = new ArrayList<>();

    private static Label information;
    private static TextArea area;

    public ProxyPanel() {
        VBox contentPane = new VBox();

        HBox topBox = new HBox();
        information = new Label();
        information.setText("Proxies (0 loaded)");
        information.setFont(new Font(information.getFont().getName(), information.getFont().getSize()+3));
        topBox.getChildren().add(information);

        contentPane.getChildren().add(topBox);

        HBox bottomBox = new HBox();
        area = new TextArea();
        VBox.setVgrow(area, Priority.SOMETIMES);
        area.setMaxWidth(Double.MAX_VALUE);
        area.setMaxHeight(Double.MAX_VALUE);
        bottomBox.getChildren().add(area);
        bottomBox.getChildren().add(new Separator(Orientation.VERTICAL));

        VBox rightBottomBox = new VBox(5);
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(event -> {
            proxyArrayList.clear();
            String[] proxiesRaw = area.getText().split("\n");
            for (String proxyRaw : proxiesRaw){
                String[] proxyParts = proxyRaw.split(":");
                if (proxyParts.length != 2) {
                    System.out.println("Skipping " + proxyRaw + "; doesn't split in 2");
                    continue;
                }
                int port;
                try {
                    port = Integer.parseInt(proxyParts[1]);
                }catch (NumberFormatException e) {
                    System.out.println("Skipping " + proxyRaw + "; non-numerical port");
                    continue;
                }
                Namefinder.SimpleProxy proxy = new Namefinder.SimpleProxy(proxyParts[0], port);
                proxyArrayList.add(proxy);
            }
            information.setText("Proxies (" + proxyArrayList.size() + " loaded)");
        });
        rightBottomBox.getChildren().add(submitButton);

        Spinner<Integer> timeoutSpinner = new Spinner<>();
        timeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 60000));
        timeoutSpinner.getValueFactory().setValue(1000);
        timeoutSpinner.setEditable(true);
        Button testButton = new Button("Test");
        testButton.setOnAction(event -> {
            new TestingProxiesPane(timeoutSpinner.getValue());
        });
        rightBottomBox.getChildren().add(testButton);
        rightBottomBox.getChildren().add(new Label("Timeout:"));
        rightBottomBox.getChildren().add(timeoutSpinner);
        rightBottomBox.setAlignment(Pos.CENTER_RIGHT);
        rightBottomBox.setMinWidth(100);
        bottomBox.getChildren().add(rightBottomBox);

        contentPane.getChildren().add(bottomBox);
        contentPane.setPadding(new Insets(10,10,10,10));

        Scene scene = new Scene(contentPane, 400, 250);
        setScene(scene);
        setMinHeight(200);
        setMinWidth(200);
    }

    public static class TestingProxiesPane extends Alert {

        public TestingProxiesPane(int timeout) {
            super(AlertType.INFORMATION);
            setTitle("Testing proxies");
            setHeaderText("Testing proxies");
            setContentText("Namefinder is currently testing your proxies!\nTimeout: " + timeout);

            VBox extra = new VBox();
            ProgressBar bar = new ProgressBar(0);
            Label label = new Label("0/0");
            bar.setMaxWidth(Double.MAX_VALUE);
            extra.setFillWidth(true);
            extra.getChildren().add(bar);
            extra.getChildren().add(label);
            int osize = proxyArrayList.size();
            new ProxyChecker(proxyArrayList, new Runnable() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        close();
                        Alert alert = new Alert(AlertType.INFORMATION);
                        alert.setTitle("Proxies");
                        alert.setHeaderText("Proxies tested!");
                        alert.setContentText("Done testing proxies! Total working: " + proxyArrayList.size() + ", Total failed: " + (osize-proxyArrayList.size()));
                        alert.show();
                        information.setText("Proxies (" + proxyArrayList.size() + " loaded)");
                        area.clear();
                        for (Namefinder.SimpleProxy proxy : proxyArrayList) {
                            area.setText(area.getText() + "\n" + proxy.ip + ":" + proxy.port);
                        }
                    });
                }
            }, timeout, bar, label).start();
            getDialogPane().setExpandableContent(extra);
            show();
        }

    }

}
