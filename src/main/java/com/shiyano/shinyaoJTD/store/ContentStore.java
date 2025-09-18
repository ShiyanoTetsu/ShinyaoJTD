package com.shiyano.shinyaoJTD.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shiyano.shinyaoJTD.core.Item;
import com.shiyano.shinyaoJTD.core.Topic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class ContentStore {

    private final Path root;
    private final ObjectMapper mapper;

    public ContentStore(Path projectRootOrContent) {
        Path p = projectRootOrContent.getFileName().toString().equals("content")
                ? projectRootOrContent
                : projectRootOrContent.resolve("content");
        this.root = p.toAbsolutePath().normalize();
        this.mapper = new ObjectMapper();
        if (!Files.isDirectory(this.root)) {
            throw new IllegalStateException("content/ folder not found at: " + this.root);
        }
    }

    public List<Topic> loadTopics() throws IOException {
        Path path = safeResolve("topics.json");
        List<Topic> topics = mapper.readValue(Files.readString(path), new TypeReference<>() {});
        // проверка уникальности Topic.code
        Set<String> dups = findDuplicates(topics.stream().map(Topic::code).toList());
        if (!dups.isEmpty()) {
            throw new IllegalStateException("Duplicate Topic.code: " + dups);
        }
        return List.copyOf(topics);
    }

    public List<Item> loadItemsFor(String topicCode) throws IOException {
        Objects.requireNonNull(topicCode, "topicCode");
        String file = "items-" + topicCode.strip() + ".json";
        Path path = safeResolve(file);
        List<Item> items = mapper.readValue(Files.readString(path), new TypeReference<>() {});
        // проверка уникальности Item.sid
        Set<String> dups = findDuplicates(items.stream().map(Item::sid).toList());
        if (!dups.isEmpty()) {
            throw new IllegalStateException("Duplicate Item.sid in " + file + ": " + dups);
        }
        return List.copyOf(items);
    }

    private Path safeResolve(String relative) {
        Path candidate = root.resolve(relative).normalize();
        if (!candidate.startsWith(root)) {
            throw new SecurityException("Access outside content/ is not allowed: " + relative);
        }
        return candidate;
    }

    private static <T> Set<T> findDuplicates(List<T> list) {
        Set<T> seen = new HashSet<>();
        return list.stream()
                .filter(e -> !seen.add(e))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Path getRoot() {
        return root;
    }
}
