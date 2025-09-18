package com.shiyano.shinyaoJTD.ui;

import com.shiyano.shinyaoJTD.core.Item;
import com.shiyano.shinyaoJTD.core.Topic;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TrainingView {

    private static final double MIN_W = 720;
    private static final double MIN_H = 520;

    private final BorderPane root = new BorderPane();

    private final Topic topic;
    private final List<Item> items;
    private final Stage stage;

    private final FuriganaService furigana = new FuriganaService(); // сервис фуриганы

    private int index = 0;
    private int remaining;

    private final Label titleLbl   = new Label();
    private final Label counterLbl = new Label();

    // БЫЛО: Label jpLbl; → СТАЛО: TextFlow с тултипами-фуриганы
    private final TextFlow jpFlow  = new TextFlow();

    private final Label glossLbl   = new Label();
    private final Label feedbackLbl= new Label();
    private final Label hintLbl    = new Label("Подсказка: клавиши 1–9 выбирают варианты");
    private final FlowPane optionsPane = new FlowPane();
    private final Button nextBtn   = new Button("Далее");

    public TrainingView(Stage stage, Topic topic, List<Item> items) {
        this.stage = stage;
        this.topic = topic;
        this.items = List.copyOf(items);
        this.remaining = this.items.size();

        // фиксируем минимальный размер окна
        this.stage.setMinWidth(MIN_W);
        this.stage.setMinHeight(MIN_H);

        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom, -fx-base, derive(-fx-base, -5%));
        """);
        root.setPadding(new Insets(16));

        var leftHeader  = new VBox(titleLbl);
        var rightHeader = new VBox(counterLbl);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        rightHeader.setAlignment(Pos.CENTER_RIGHT);
        var header = new HBox(10, leftHeader, new Region(), rightHeader);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        counterLbl.setStyle("-fx-opacity: 0.8;");

        var card = new VBox(14);
        card.setPadding(new Insets(18));
        card.setStyle("""
            -fx-background-color: -fx-control-inner-background;
            -fx-background-radius: 16;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0.2, 0, 4);
        """);

        // jpFlow — крупнее, перенос строк включён
        jpFlow.setLineSpacing(4);
        jpFlow.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        glossLbl.setWrapText(true);
        glossLbl.setStyle("-fx-opacity: 0.8;");

        optionsPane.setHgap(10);
        optionsPane.setVgap(10);
        optionsPane.setPrefWrapLength(560);

        feedbackLbl.setWrapText(true);
        feedbackLbl.setStyle("-fx-font-weight: bold;");
        hintLbl.setStyle("-fx-opacity: 0.7; -fx-font-size: 12px;");

        var topBox    = new VBox(6, jpFlow, glossLbl);
        var midBox    = new VBox(8, optionsPane);
        var bottomBox = new VBox(4, feedbackLbl, hintLbl);

        var footer = new HBox(nextBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        nextBtn.setStyle(primaryButton());
        nextBtn.setDisable(true);
        nextBtn.setOnAction(e -> onNextClicked());

        card.getChildren().addAll(topBox, midBox, bottomBox, footer);

        StackPane center = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);

        root.setTop(header);
        root.setCenter(center);

        titleLbl.setText("Тема: %s — тренировка".formatted(topic.title()));

        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            int digit = keyToDigit(e.getCode());
            if (digit >= 1 && digit <= 9) {
                selectByIndex(digit - 1);
                e.consume();
            }
        });

        showCurrent();
    }

    public Parent getRoot() { return root; }

    // ===== логика =====

    private void onNextClicked() {
        if (index >= items.size() - 1) {
            goBackToMenu();
            return;
        }
        index++;
        remaining = Math.max(0, remaining - 1);
        showCurrent();
    }

    private void showCurrent() {
        Item it = items.get(index);

        // меняем текст на последнем шаге
        nextBtn.setText(index == items.size() - 1 ? "Завершить" : "Далее");

        counterLbl.setText("Осталось: " + remaining);

        // РЕНДЕР ПРЕДЛОЖЕНИЯ С ФУРИГАНОЙ:
        // если захочешь ручные чтения — передай map вместо null
        TextFlow built = furigana.createTextFlow(it.jp(), null);
        jpFlow.getChildren().setAll(built.getChildren());

        glossLbl.setText(it.gloss() == null ? "" : it.gloss());
        feedbackLbl.setText("");
        feedbackLbl.setStyle("-fx-text-fill: -fx-text-inner-color; -fx-font-weight: bold;");
        nextBtn.setDisable(true);

        optionsPane.getChildren().clear();
        List<Button> buttons = new ArrayList<>();
        int i = 1;
        for (String opt : it.options()) {
            Button b = new Button("%d) %s".formatted(i++, opt));
            b.setStyle(choiceButton());
            b.setMinWidth(120);
            b.setPrefHeight(40);
            b.setFocusTraversable(false);
            b.setOnAction(e -> onAnswer(it, opt, buttons, b));
            buttons.add(b);
        }
        optionsPane.getChildren().addAll(buttons);
    }

    private void onAnswer(Item it, String chosen, List<Button> allButtons, Button clicked) {
        for (Button b : allButtons) b.setDisable(true);
        nextBtn.setDisable(false);

        Set<String> correct = it.correctSet();
        if (correct.contains(chosen)) {
            clicked.setStyle(choiceButtonSuccess());
            feedbackLbl.setText("Верно. " + (it.whyCorrect() == null ? "" : it.whyCorrect()));
        } else {
            clicked.setStyle(choiceButtonDanger());
            Map<String,String> ww = it.whyWrong();
            String reason = ww != null ? ww.getOrDefault(chosen, "") : "";
            feedbackLbl.setText(("Неверно. " + reason).trim());
            for (Button b : allButtons) {
                String text = b.getText();
                String opt = text.substring(text.indexOf(')') + 2);
                if (correct.contains(opt)) b.setStyle(choiceButtonSuccess());
            }
        }
    }

    private void selectByIndex(int zeroBased) {
        if (zeroBased < 0 || zeroBased >= optionsPane.getChildren().size()) return;
        var node = optionsPane.getChildren().get(zeroBased);
        if (node instanceof Button b && !b.isDisabled()) b.fire();
    }

    private void goBackToMenu() {
        Platform.runLater(() -> {
            double contentW = Math.max(stage.getScene().getWidth(),  MIN_W);
            double contentH = Math.max(stage.getScene().getHeight(), MIN_H);
            TopicSelectionView menu = new TopicSelectionView(java.nio.file.Path.of(""));
            Scene scene = new Scene(menu.getRoot(), contentW, contentH);
            stage.setTitle("JP Trainer — выбор темы");
            stage.setMinWidth(MIN_W);
            stage.setMinHeight(MIN_H);
            stage.setScene(scene);
        });
    }

    private static int keyToDigit(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 1;
            case DIGIT2, NUMPAD2 -> 2;
            case DIGIT3, NUMPAD3 -> 3;
            case DIGIT4, NUMPAD4 -> 4;
            case DIGIT5, NUMPAD5 -> 5;
            case DIGIT6, NUMPAD6 -> 6;
            case DIGIT7, NUMPAD7 -> 7;
            case DIGIT8, NUMPAD8 -> 8;
            case DIGIT9, NUMPAD9 -> 9;
            default -> -1;
        };
    }

    // ===== стили =====

    private static String primaryButton() {
        return """
            -fx-background-color: -fx-accent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
            -fx-background-radius: 10;
            -fx-padding: 8 16 8 16;
        """;
    }

    private static String choiceButton() {
        return """
            -fx-background-color: derive(-fx-control-inner-background, -4%);
            -fx-background-radius: 10;
            -fx-padding: 6 12 6 12;
            -fx-font-size: 14px;
        """;
    }

    private static String choiceButtonSuccess() {
        return """
            -fx-background-color: #2ecc71;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-font-weight: bold;
        """;
    }

    private static String choiceButtonDanger() {
        return """
            -fx-background-color: #e74c3c;
            -fx-text-fill: white;
            -fx-background-radius: 10;
            -fx-font-weight: bold;
        """;
    }
}
