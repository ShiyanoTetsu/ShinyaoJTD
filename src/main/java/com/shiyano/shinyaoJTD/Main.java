package com.shiyano.shinyaoJTD;

import com.shiyano.shinyaoJTD.ui.TopicSelectionView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    private static final double MIN_W = 720;
    private static final double MIN_H = 520;

    @Override
    public void start(Stage stage) {
        var view = new TopicSelectionView(AppPaths.contentDir());

        Scene scene = new Scene(view.getRoot(), Math.max(800, MIN_W), Math.max(600, MIN_H));

        stage.setMinWidth(MIN_W);
        stage.setMinHeight(MIN_H);

        stage.setTitle("JP Trainer — выбор темы");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
