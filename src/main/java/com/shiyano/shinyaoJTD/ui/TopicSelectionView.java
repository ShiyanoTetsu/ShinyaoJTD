package com.shiyano.shinyaoJTD.ui;

import com.shiyano.shinyaoJTD.core.Item;
import com.shiyano.shinyaoJTD.core.Topic;
import com.shiyano.shinyaoJTD.store.ContentStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;

public final class TopicSelectionView {

    private static final double MIN_W = 720;
    private static final double MIN_H = 520;

    private final BorderPane root = new BorderPane();

    private final ListView<Topic> listView = new ListView<>();
    private final Button startBtn = new Button("Начать тренировку");
    private final Button reloadBtn = new Button("Обновить");

    private final ContentStore store;

    public TopicSelectionView(Path projectRootOrContent) {
        this.store = new ContentStore(projectRootOrContent);

        root.setPadding(new Insets(16));
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom, -fx-base, derive(-fx-base, -5%));
        """);

        var card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle("""
            -fx-background-color: -fx-control-inner-background;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0.2, 0, 4);
        """);

        var title = new Label("Выберите тему для тренировки");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        listView.setPlaceholder(new Label("Нет тем. Проверь content/topics.json"));
        listView.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Topic t, boolean empty) {
                super.updateItem(t, empty);
                setText(empty || t == null ? null : "%s — %s".formatted(t.code(), t.title()));
            }
        });
        listView.setPrefSize(520, 320);

        reloadBtn.setOnAction(e -> loadTopics());
        startBtn.setOnAction(e -> onStart());
        startBtn.setDefaultButton(true);
        startBtn.disableProperty().bind(listView.getSelectionModel().selectedItemProperty().isNull());
        stylePrimary(startBtn);

        var buttons = new HBox(10, reloadBtn, startBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(title, listView, buttons);

        StackPane center = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        root.setCenter(center);

        loadTopics();
    }

    public Parent getRoot() { return root; }

    private void loadTopics() {
        try {
            List<Topic> topics = store.loadTopics();
            listView.getItems().setAll(topics);
            if (!topics.isEmpty()) listView.getSelectionModel().selectFirst();
        } catch (Exception ex) {
            showError("Не удалось загрузить topics.json", ex);
        }
    }

    private void onStart() {
        Topic selected = listView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            List<Item> items = store.loadItemsFor(selected.code());

            Stage stage = (Stage) root.getScene().getWindow();
            stage.setMinWidth(MIN_W);
            stage.setMinHeight(MIN_H);

            // ВАЖНО: берём размеры КОНТЕНТА текущей сцены, а не окна!
            Scene current = stage.getScene();
            double contentW = Math.max(current.getWidth(),  MIN_W);
            double contentH = Math.max(current.getHeight(), MIN_H);

            TrainingView training = new TrainingView(stage, selected, items);
            Scene newScene = new Scene(training.getRoot(), contentW, contentH);

            stage.setTitle("JP Trainer — тренировка: " + selected.title());
            stage.setScene(newScene);
            // при желании можно «подогнать» окно к новой сцене: stage.sizeToScene();
        } catch (Exception ex) {
            showError("Ошибка загрузки items-" + selected.code() + ".json", ex);
        }
    }

    private static void stylePrimary(Button b) {
        b.setStyle("""
            -fx-background-color: -fx-accent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 10;
            -fx-padding: 8 16 8 16;
        """);
    }

    private static void showError(String message, Exception ex) {
        var a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Ошибка");
        a.setHeaderText(message);
        a.setContentText(ex.getMessage());
        a.showAndWait();
    }
}
