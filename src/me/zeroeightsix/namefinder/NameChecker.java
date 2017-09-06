package me.zeroeightsix.namefinder;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by 086 on 3/09/2017.
 */
public class NameChecker extends Thread {

    ArrayList<List<String>> names;
    Runnable callback;
    boolean running = true;

    public NameChecker(ArrayList<String> names, Runnable callback) {
        for (int i = 0; i < names.size(); i++) // Make sure everything is lowercase!
            names.set(i, names.get(i).toLowerCase());

        this.names = splitArray(new ArrayList<>(),names, 100);
        this.callback = callback;
    }

    private ArrayList<List<String>> splitArray(ArrayList<List<String>> buffer, List<String> names, int length) {
        if (names.size() <= length){
            buffer.add(names);
            return buffer;
        }else{
            buffer.add(names.subList(0,100));
            return splitArray(buffer, names.subList(101, names.size()), length);
        }
    }

    int index = 0;

    private boolean hasNext() {
        if (!running) return false;
        return index < names.size();
    }

    private List<String> next() {
        List<String> s = names.get(index);
        index++;
        return s;
    }

    @Override
    public void run() {
        Namefinder.setProgress(0);
        Namefinder.showProgressBar();
        Namefinder.setStatus("Checking names");
        Namefinder.freeNames.clear();

        while (hasNext()) {
            if (!running) return;
            List<String> toCheck = next();
            Namefinder.currentNames = toCheck;
            String[] taken = filterTakenNames(toCheck);
            if (taken == null) { // An exception occured! We'll try again in a minute
                Namefinder.TIMEOUT = true;
                Namefinder.nameView.refresh();
                index--;

                int secondsLeft = 60;

                while (secondsLeft > 0) {
                    int hours = secondsLeft / 3600;
                    int minutes = (secondsLeft % 3600) / 60;
                    int seconds = secondsLeft % 60;

                    Platform.runLater(() -> Namefinder.STAGE.setTitle(Namefinder.NAME + " | On timeout: " + hours + ":" + minutes + ":" + seconds));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    secondsLeft--;
                }

                Namefinder.TIMEOUT = false;
                Namefinder.nameView.refresh();

                Platform.runLater(() -> Namefinder.STAGE.setTitle(Namefinder.NAME));
                continue;
            }

            final List<String> takenList = Arrays.asList(taken);
            final List<String> toCheckFinal = new ArrayList<>(toCheck);
            toCheckFinal.removeAll(takenList);
            Namefinder.freeNames.addAll(toCheckFinal);
            Platform.runLater(() -> {
                double progress = (double)index/(double)names.size();
                Namefinder.progressBar.setProgress(progress);
                Namefinder.nameView.scrollTo(toCheck.get(0));
                Namefinder.nameView.refresh();
            });
        }

        // Clean up whites
        ObservableList<String> n = FXCollections.observableArrayList();
        n.addAll(Namefinder.freeNames);
        Platform.runLater(() -> Namefinder.nameView.setItems(n));

        Namefinder.currentNames = new ArrayList<>();
        Namefinder.hideProgressBar();
        Namefinder.clearStatus();

        callback.run();
        /*Namefinder.setProgress(0);
        Namefinder.showProgressBar();

        ArrayList<String> buffer = new ArrayList<>();

        while (hasNext()) {
            Namefinder.setProgress((double)index/names.size());
            String name = next();
            buffer.add(name);

            Namefinder.currentName = name;

            if (Namefinder.freeNames.contains(name))
                continue;

            Namefinder.NameResult result = Namefinder.checkName(name);
            if (result == Namefinder.NameResult.FREE) {
                Namefinder.freeNames.add(name);
            }else if (result == Namefinder.NameResult.TAKEN) {
                Platform.runLater(() -> Namefinder.nameView.getItems().remove(name));
            }else if (result == Namefinder.NameResult.TIMEOUT) {
                Namefinder.nameView.refresh();
                index--;

                int secondsLeft = Namefinder.TIMEOUT/1000;

                while (secondsLeft > 0) {
                    int hours = secondsLeft / 3600;
                    int minutes = (secondsLeft % 3600) / 60;
                    int seconds = secondsLeft % 60;

                    Platform.runLater(() -> Namefinder.STAGE.setTitle(Namefinder.NAME + " | On timeout: " + hours + ":" + minutes + ":" + seconds));

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    secondsLeft--;
                }

                Platform.runLater(() -> Namefinder.STAGE.setTitle(Namefinder.NAME));
            }

            Platform.runLater(() -> Namefinder.nameView.scrollTo(name));
            Namefinder.nameView.refresh();
        }
        Namefinder.currentName = "";
        Namefinder.hideProgressBar();

        callback.run();*/
    }

    private static String authenticateEndpoint = "https://api.mojang.com/profiles/minecraft";

    public static String[] filterTakenNames(List<String> strings) {
        return jsonToArray(requestIDs(listToJson(strings)));
    }

    private static String[] jsonToArray(String json) {
        if (json == null) return null;
        json = json.replaceAll("}," + Pattern.quote("{") + "\"id\":\".*?,\"name\":\"", "");
        json = json.replaceAll(Pattern.quote("[{") + "\"id\":\".*?\",\"name\":", "");
        json = json.replaceAll("\"legacy\":true", ""); // Get rid of extra tags if present
        json = json.replaceAll("\"demo\":true", "");
        json = json.replace(",", ""); // Get rid of leftover commas
        try{
            json = json.substring(1, json.length()-2);
        }catch (Exception e){}
        json = json.toLowerCase();
        return json.split("\"");
    }

    private static String listToJson(List<String> list) {
        String s = "[";
        for (String name : list) {
            s += "\"" + name + "\",";
        }
        s = s.substring(0, s.length()-1) + "]";
        return s;
    }

    private static String requestIDs(String data) {
        try{
            String query = authenticateEndpoint;
            String json = data;

            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");

            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.close();

            // read the response
            InputStream in = new BufferedInputStream(conn.getInputStream());
            String res = convertStreamToString(in);
            in.close();
            conn.disconnect();

            return res;
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        String r = s.hasNext() ? s.next() : "/";
        return r;
    }
}
