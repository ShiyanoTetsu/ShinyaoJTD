package com.shiyano.shinyaoJTD.core;

import java.util.List;
import java.util.Objects;

public record Topic(
        String code,
        String title,
        List<String> particles,
        Lesson lesson
) {

    public Topic {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(particles, "particles");
        Objects.requireNonNull(lesson, "lesson");

        code = code.strip();
        title = title.strip();

        if (code.isEmpty()) {
            throw new IllegalArgumentException("Topic.code is blank");
        }
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Topic.title is blank");
        }
        if (particles.isEmpty()) {
            throw new IllegalArgumentException("Topic.particles is empty");
        }

        if (particles.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("Topic.particles must not contain blank entries");
        }

        particles = List.copyOf(particles);
    }

    public record Lesson(String book, int unit) {
        public Lesson {
            Objects.requireNonNull(book, "book");
            book = book.strip();
            if (book.isEmpty()) {
                throw new IllegalArgumentException("Lesson.book is blank");
            }
            if (unit <= 0) {
                throw new IllegalArgumentException("Lesson.unit must be > 0");
            }
        }
    }
}
