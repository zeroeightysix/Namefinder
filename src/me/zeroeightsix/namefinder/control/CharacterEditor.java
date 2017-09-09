package me.zeroeightsix.namefinder.control;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import me.zeroeightsix.namefinder.Namefinder;
import me.zeroeightsix.namefinder.generator.CustomGenerator;
import me.zeroeightsix.namefinder.generator.NameGenerator;
import me.zeroeightsix.namefinder.generator.PresetGenerator;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Created by 086 on 3/09/2017.
 */
public class CharacterEditor extends Stage {

    static VBox contentPane;

    static VBox presetBox;
    public static CheckBox alphabeticCharacters;
    public static CheckBox digits;
    public static CheckBox underscores;

    static VBox customBox;
    public static TextField characters;

    static long possibilities = 0;

    public CharacterEditor() {
        setTitle("Characters");

        contentPane = new VBox();

        GridPane setPane = new GridPane();
        setPane.setAlignment(Pos.CENTER);
        setPane.setPadding(new Insets(10, 25, 25, 25));
        setPane.setHgap(10);
        setPane.setVgap(10);
        Label label = new Label("Set:");
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll("Preset", "Custom");
        comboBox.setValue("Preset");

        presetBox = createPresetBox();
        presetBox.setVisible(true);
        comboBox.setOnAction(event -> {
            if (comboBox.getValue().equals("Preset"))
                setContentBox(presetBox);
//            presetBox.setVisible(comboBox.getValue().equals("Preset"));
            if (comboBox.getValue().equals("Custom"))
                setContentBox(customBox);

            calculateAndSetPossibilities();
        });
        setPane.add(label, 0, 0);
        setPane.add(comboBox, 1, 0);

        customBox = new VBox();
        characters = new TextField();
        characters.setOnKeyTyped(event -> calculateAndSetPossibilities());
        customBox.setPadding(new Insets(0,10,10,10));
        customBox.getChildren().add(new Label("Characters:"));
        customBox.getChildren().add(characters);

        contentPane.getChildren().add(setPane);
        contentPane.getChildren().add(presetBox);

        Scene scene = new Scene(contentPane, 250,200);
        setScene(scene);
        setMinHeight(180);
        setMinWidth(210);
    }

    private void setContentBox(VBox vBox) { // Set the currently visible panel on the window
        if (contentPane.getChildren().contains(vBox)) return;
        contentPane.getChildren().remove(presetBox);
        contentPane.getChildren().remove(customBox);
        presetBox.setVisible(false);
        customBox.setVisible(false);
        vBox.setVisible(true);
        contentPane.getChildren().add(vBox);
    }

    private VBox createPresetBox() {
        presetBox = new VBox();

        alphabeticCharacters = new CheckBox("Alphabetic Characters (a-z)");
        digits = new CheckBox("Digits (0-9)");
        underscores = new CheckBox("Underscore _");

        alphabeticCharacters.setOnAction(event -> calculateAndSetPossibilities());
        digits.setOnAction(event -> calculateAndSetPossibilities());
        underscores.setOnAction(event -> calculateAndSetPossibilities());

        presetBox.setPadding(new Insets(0, 25, 25, 25));
        presetBox.getChildren().addAll(alphabeticCharacters, digits, underscores);

        return presetBox;
    }

    public static void calculateAndSetPossibilities() { // Calculate & set the text of the possibility label on main window
        NameGenerator generator = getCurrentGenerator();
        if (generator == null || generator.isInvalid()) {
            Namefinder.INSTANCE.possibilityLabel.setText("");
        }else {
            long p = generator.possibilities();
            possibilities = p;
            if (p >= Long.MAX_VALUE)
                Namefinder.INSTANCE.possibilityLabel.setText(">9 quintillion possibilities");
            else
                Namefinder.INSTANCE.possibilityLabel.setText(possibilities + " possibilities");
        }
    }

    public static void flushNames(int amount) {
        NameGenerator generator = getCurrentGenerator();
        if (generator == null){
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Namefinder");
                alert.setHeaderText("No possibilities");
                alert.setContentText("No names can be generated; there aren't any possible combinations to begin with");
                alert.show();
            });
            return;
        }

        // Contact! We've got a numberGenerator
        // Now we'll split the amount of names requested into an array of random integers which the sum of is amount
        // The length of the array = maxlength - minlength

        Namefinder.setStatus("Splitting tasks");
        int maxlength = Namefinder.maxFactory.getValue()+1;
        int minlength = Namefinder.minFactory.getValue();
        int chunks = maxlength - minlength; // Length of our final array

        Random random = new Random();
        int[] randoms = new int[chunks];
        for (int i = 0; i < randoms.length; i++)
            randoms[i] = random.nextInt(amount);

        int sum = IntStream.of(randoms).sum();
        for (int i = 0; i < randoms.length; i++)
            randoms[i] = (int) (((double)randoms[i]/(double)sum)*(double)amount);

        sum = IntStream.of(randoms).sum();
        int point = amount - sum;   // Difference between sum and amount (always small, has to do with int<->double conversion)
        randoms[0] += point;        // Just add it up to the first element in the array. No big difference

        // Hooray! We now have an array of length maxlength-minlength, composed of random integers that all count up to 'amount'!
//        System.out.println("Generating " + amount + " names using " + generator.getClass().getSimpleName());
        System.out.print("Generating names using " + generator.getClass().getSimpleName() + "! ");

        for (int i = minlength; i < maxlength; i++) { // Create names by these lengths
            int index = i - minlength;
            int size = randoms[index];
            int length = i;
            Namefinder.setStatus("Generating " + length + "-character names");
            System.out.print(size + " names of " + length + " characters.. ");
            try{
                flushWithLength(length, size, generator);
            }catch (RuntimeException e){
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Namefinder");
                    alert.setHeaderText("Amount > Possibilities");
                    alert.setContentText("Can't generate more names than the amount of possibilities!");
                    alert.show();
                });
                continue;
            }
            Platform.runLater(() -> Namefinder.progressBar.setProgress((double)index/(double)randoms.length));
        }

        System.out.println("");
        System.out.println("Done generating names!");
        Namefinder.setStatus("");
    }

    private static void flushWithLength(int length, int amount, NameGenerator generator) { // Flush names with fixed length
        List<Long> numbers = new ArrayList<>();

        long maxsize = generator.getPossibilities(length);
        if (amount > maxsize)
            throw new RuntimeException("Amount may not be over range");

        for (int  i = 0; i < amount; i++) {
            long number = ThreadLocalRandom.current().nextLong(maxsize);
            while(numbers.contains(number)) number++;
            numbers.add(number);
        }

        // Sort our indices, they're faster to sort than strings and our mapping function works alphabetically anyways
//        Collections.sort(numbers);
        // nvm that didn't work for some reason

        // Convert our indices to actual names following the generator
        ArrayList<String> names = new ArrayList<>();
        for (Long l : numbers)
            names.add(generator.map(length, l));

        // Sort & remove dupes if present
        Collections.sort(names);
        Set<String> nodupes = new LinkedHashSet<>(names);

        Platform.runLater(() -> Namefinder.nameView.getItems().addAll(nodupes)); // Add our newly-generated names to the list
    }

    private static NameGenerator getCurrentGenerator() {
        NameGenerator generator = null;
        if (presetBox.isVisible())
            generator = new PresetGenerator();
        else if (customBox.isVisible())
            generator = new CustomGenerator();
        if (generator == null || generator.isInvalid()) return null; // No possibilities error
        return generator;
    }
}
