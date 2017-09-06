package me.zeroeightsix.namefinder;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by 086 on 3/09/2017.
 */
public class ProxyChecker extends Thread {

    private final ProgressBar progressBar;
    private final Label progressLabel;
    ArrayList<Namefinder.SimpleProxy> proxies = new ArrayList<>();
    Runnable callback;
    int timeout;
    public double progress;

    public ProxyChecker(ArrayList<Namefinder.SimpleProxy> proxies, Runnable callback, int timeout, ProgressBar progressBar, Label label) {
        this.proxies = proxies;
        this.callback = callback;
        this.timeout = timeout;
        this.progressBar = progressBar;
        this.progressLabel = label;
    }

    @Override
    public void run() {
        Iterator<Namefinder.SimpleProxy> proxyIterator = proxies.iterator();
        int tested = 0;
        int size = proxies.size();
        while (proxyIterator.hasNext()){
            Namefinder.SimpleProxy proxy = proxyIterator.next();
            if (!proxy.testConnection(timeout))
                proxyIterator.remove();
            tested++;
            progress = (double) tested / size;
            int finalTested = tested;
            Platform.runLater(() -> {
                progressBar.setProgress(progress);
                progressLabel.setText(finalTested + "/" + size);
            });
        }

        callback.run();
    }
}
