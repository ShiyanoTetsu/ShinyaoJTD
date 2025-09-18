package com.shiyano.shinyaoJTD.core;

import java.util.*;
import java.util.stream.Collectors;

public record Item(
        String sid,
        String jp,
        String gloss,
        List<String> options,
        String correct,
        String whyCorrect,
        Map<String, String> whyWrong
) {
    public static final char GAP = '＿';

    public Item {
        Objects.requireNonNull(sid, "sid");
        Objects.requireNonNull(jp, "jp");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(correct, "correct");

        sid = sid.strip();
        jp = jp.strip();
        gloss = gloss == null ? null : gloss.strip();
        correct = correct.strip();
        whyCorrect = whyCorrect == null ? null : whyCorrect.strip();

        if (sid.isEmpty()) {
            throw new IllegalArgumentException("sid is blank");
        }
        if (jp.isEmpty()) {
            throw new IllegalArgumentException("jp is blank");
        }

        long gapCount = jp.chars().filter(c -> c == GAP).count();
        if (gapCount != 1) {
            throw new IllegalArgumentException("jp must contain exactly one GAP symbol ＿ (U+FF3F)");
        }

        List<String> normalizedOptions = options.stream()
                .peek(Objects::requireNonNull)
                .map(String::strip)
                .peek(opt -> {
                    if (opt.isBlank()) throw new IllegalArgumentException("options must not contain blank entries");
                })
                .toList();

        if (normalizedOptions.isEmpty()) {
            throw new IllegalArgumentException("options is empty");
        }
        if (new HashSet<>(normalizedOptions).size() != normalizedOptions.size()) {
            throw new IllegalArgumentException("options must be unique (no duplicates)");
        }

        Set<String> correctSet = Arrays.stream(correct.split("/"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (correctSet.isEmpty()) {
            throw new IllegalArgumentException("correct is blank or contains only separators");
        }
        if (!normalizedOptions.containsAll(correctSet)) {
            throw new IllegalArgumentException("every correct option must be present in options");
        }

        if (whyWrong != null) {
            Map<String, String> normalizedWhyWrong = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : whyWrong.entrySet()) {
                String key = Objects.requireNonNull(e.getKey(), "whyWrong key").strip();
                String val = Objects.requireNonNull(e.getValue(), "whyWrong value").strip();
                if (key.isEmpty()) throw new IllegalArgumentException("whyWrong contains blank key");
                if (val.isEmpty()) throw new IllegalArgumentException("whyWrong explanation must not be blank");
                normalizedWhyWrong.put(key, val);
            }

            Set<String> wrongOptions = new LinkedHashSet<>(normalizedOptions);
            wrongOptions.removeAll(correctSet);

            if (!normalizedWhyWrong.keySet().containsAll(wrongOptions)) {
                throw new IllegalArgumentException("whyWrong must cover all incorrect options");
            }
            for (String k : normalizedWhyWrong.keySet()) {
                if (!normalizedOptions.contains(k)) {
                    throw new IllegalArgumentException("whyWrong contains key not present in options: " + k);
                }
            }

            whyWrong = Map.copyOf(normalizedWhyWrong);
        }

        options = List.copyOf(normalizedOptions);
    }

    public Set<String> correctSet() {
        return Arrays.stream(correct.split("/"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
