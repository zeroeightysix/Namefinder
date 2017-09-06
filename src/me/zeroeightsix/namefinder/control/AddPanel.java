package me.zeroeightsix.namefinder.control;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import me.zeroeightsix.namefinder.Namefinder;

/**
 * Created by 086 on 3/09/2017.
 */
public class AddPanel extends Stage {

    public AddPanel() {
        setTitle("Add");
        VBox content = new VBox();
        HBox top = new HBox();

        TextArea nameBox = new TextArea();

        Label information = new Label();
        information.setText("Add names");
        information.setFont(new Font(information.getFont().getName(), information.getFont().getSize()+3));
        top.getChildren().add(information);
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.SOMETIMES);
        Button addButton = new Button("Add");
        addButton.setOnAction(event -> {
            String[] lines = nameBox.getText().split("[\n, ]");
            for (String s : lines) {
                if (s.isEmpty()) continue;
                if (s.length() > 16) s = s.substring(0, 16);
                Namefinder.nameView.getItems().add(s);
            }
            nameBox.clear();
        });
        top.getChildren().add(spacer);
        top.getChildren().add(addButton);

        content.getChildren().add(top);
        content.getChildren().add(nameBox);
        content.setPadding(new Insets(10,10,10,10));
        content.setSpacing(10);

        setOnShowing(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                nameBox.requestFocus();
            }
        });

        nameBox.setOnKeyPressed(event -> {
            if (event.getCode().getName().equalsIgnoreCase("ESC")){
                close();
            }
        });

        setScene(new Scene(content, 200,150));
    }

}
