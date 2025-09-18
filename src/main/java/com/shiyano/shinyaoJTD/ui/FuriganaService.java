package com.shiyano.shinyaoJTD.ui;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import javafx.geometry.Point2D;
import javafx.scene.control.Tooltip;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.util.*;

/**
 * FuriganaService на базе Atilika Kuromoji (kuromoji-ipadic).
 * Делает TextFlow, где фуригана показывается по наведению — вручную (show/hide),
 * чтобы избежать проблем с «узкой зоной» Text в TextFlow.
 */
public final class FuriganaService {

    public static final class TokenInfo {
        public final String surface;
        public final String readingHira;
        public final String partOfSpeech;

        public TokenInfo(String surface, String readingHira, String partOfSpeech) {
            this.surface = surface;
            this.readingHira = readingHira;
            this.partOfSpeech = partOfSpeech;
        }
    }

    private final Tokenizer tokenizer = new Tokenizer();

    public List<TokenInfo> tokenize(String jp) {
        Objects.requireNonNull(jp, "jp");
        List<Token> toks = tokenizer.tokenize(jp);
        List<TokenInfo> out = new ArrayList<>(toks.size());
        for (Token t : toks) {
            String surface = t.getSurface();
            String readingKatakana = t.getReading();              // может быть null
            String readingHira = readingKatakana != null ? kataToHira(readingKatakana) : null;
            String pos = String.join("-", Arrays.asList(
                    t.getPartOfSpeechLevel1(),
                    t.getPartOfSpeechLevel2(),
                    t.getPartOfSpeechLevel3(),
                    t.getPartOfSpeechLevel4()
            ));
            out.add(new TokenInfo(surface, readingHira, pos));
        }
        return out;
    }

    /**
     * Собирает TextFlow, навешивая «ручные» тултипы на кандзи/катакану.
     * manualReadings: переопределения чтений (ひらがна/カタカナ); можно null.
     */
    public TextFlow createTextFlow(String jp, Map<String, String> manualReadings) {
        List<TokenInfo> tokens = tokenize(jp);

        Map<String, String> manual = new HashMap<>();
        if (manualReadings != null) {
            manualReadings.forEach((k, v) -> manual.put(k, v == null ? null : normalizeReading(v)));
        }

        TextFlow flow = new TextFlow();
        flow.setLineSpacing(4);

        for (TokenInfo t : tokens) {
            String surface = t.surface;

            // Символ пропуска — как есть
            if ("＿".equals(surface)) {
                flow.getChildren().add(new Text(surface));
                continue;
            }

            String reading = manual.containsKey(surface) ? manual.get(surface) : t.readingHira;

            Text node = new Text(surface);
            // Чуть увеличим «зону наведения»: включаем попадание по прямоугольнику
            node.setPickOnBounds(true);
            // Пара пикселей внутреннего отступа, чтобы попасть было легче
            node.setStyle("-fx-padding: 0 2 0 2;");

            if (reading != null && shouldShowTooltip(surface, reading)) {
                Tooltip tip = new Tooltip(reading);
                tip.setShowDelay(Duration.millis(150));
                tip.setShowDuration(Duration.seconds(15));
                tip.setHideDelay(Duration.millis(100));

                // Показ/скрытие вручную — стабильнее, чем Tooltip.install для Text в TextFlow
                node.setOnMouseEntered(e -> {
                    Point2D p = node.localToScreen(e.getX() + 8, e.getY() + 12);
                    if (p != null) tip.show(node, p.getX(), p.getY());
                });
                node.setOnMouseExited(e -> tip.hide());
            }

            flow.getChildren().add(node);
        }

        return flow;
    }

    /** カタカナ → ひらがな. */
    private static String kataToHira(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 'ァ' && ch <= 'ン') {
                sb.append((char) (ch - 'ァ' + 'ぁ'));
            } else if (ch == 'ヴ') {
                sb.append('ゔ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /** Нормализация: катакану переводим в хирагану. */
    private static String normalizeReading(String reading) {
        boolean hasKatakana = reading.codePoints().anyMatch(c -> (c >= 'ァ' && c <= 'ン') || c == 'ヴ');
        return hasKatakana ? kataToHira(reading) : reading;
    }

    /** Решаем, нужен ли тултип. */
    private static boolean shouldShowTooltip(String surface, String readingHira) {
        if (readingHira == null || readingHira.isBlank()) return false;
        boolean hasKanji = surface.codePoints().anyMatch(FuriganaService::isCJKIdeograph);
        boolean hasKatakana = surface.codePoints().anyMatch(c -> (c >= 'ァ' && c <= 'ン') || c == 'ヴ');
        if (hasKanji || hasKatakana) return true;
        return !surface.equals(readingHira);
    }

    private static boolean isCJKIdeograph(int cp) {
        return (cp >= 0x4E00 && cp <= 0x9FFF)
                || (cp >= 0x3400 && cp <= 0x4DBF)
                || (cp >= 0xF900 && cp <= 0xFAFF);
    }
}
