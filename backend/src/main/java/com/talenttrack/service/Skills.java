package com.talenttrack.service;

import java.util.*;

/** Helpers for the comma-separated skill columns. */
public final class Skills {

    private Skills() {
    }

    public static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Trims, removes blanks and case-insensitive duplicates while keeping first spelling and order. */
    public static List<String> clean(Collection<String> skills) {
        if (skills == null) {
            return List.of();
        }
        Map<String, String> unique = new LinkedHashMap<>();
        for (String s : skills) {
            if (s == null) {
                continue;
            }
            String trimmed = s.trim().replace(",", " ");
            if (!trimmed.isEmpty()) {
                unique.putIfAbsent(trimmed.toLowerCase(Locale.ROOT), trimmed);
            }
        }
        return List.copyOf(unique.values());
    }

    public static String join(Collection<String> skills) {
        List<String> cleaned = clean(skills);
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    public static List<String> merge(Collection<String> first, Collection<String> second) {
        List<String> all = new ArrayList<>(first == null ? List.of() : first);
        if (second != null) {
            all.addAll(second);
        }
        return clean(all);
    }
}
