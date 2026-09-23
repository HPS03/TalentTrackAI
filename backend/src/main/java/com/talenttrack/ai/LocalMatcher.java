package com.talenttrack.ai;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Deterministic keyword-based matcher used when the Python ai-service is unreachable,
 * so applying for jobs never fails because of an AI outage (graceful degradation).
 */
public final class LocalMatcher {

    private static final Set<String> STOP_WORDS = Set.of("the", "and", "for", "with", "you", "are", "our", "will",
            "have", "this", "that", "from", "your", "who", "can", "all", "not", "but", "has", "was", "work");

    private LocalMatcher() {
    }

    public static AiModels.MatchResult match(AiModels.MatchRequest req) {
        String resume = Objects.toString(req.resumeText(), "").toLowerCase(Locale.ROOT);
        Set<String> candidateSkills = normalise(req.candidateSkills());

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : req.jobSkills()) {
            String s = skill.toLowerCase(Locale.ROOT).trim();
            if (candidateSkills.contains(s) || (!resume.isEmpty() && resume.contains(s))) {
                matched.add(skill);
            } else {
                missing.add(skill);
            }
        }
        int skillScore = req.jobSkills().isEmpty() ? 50 : Math.round(100f * matched.size() / req.jobSkills().size());

        Set<String> jdTokens = tokens(req.jobDescription());
        Set<String> cvTokens = tokens(resume + " " + String.join(" ", candidateSkills));
        int semantic = 0;
        if (!jdTokens.isEmpty() && !cvTokens.isEmpty()) {
            long common = jdTokens.stream().filter(cvTokens::contains).count();
            semantic = (int) Math.min(100, Math.round(100.0 * common / Math.sqrt((double) jdTokens.size() * cvTokens.size()) * 1.5));
        }

        int expScore = experienceScore(req.candidateExperience(), req.minExperience());
        int overall = Math.round(skillScore * 0.55f + semantic * 0.30f + expScore * 0.15f);
        String summary = "Keyword match: " + matched.size() + "/" + req.jobSkills().size() + " required skills found."
                + (missing.isEmpty() ? "" : " Consider highlighting: " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))) + ".");
        return new AiModels.MatchResult(overall, skillScore, semantic, expScore, matched, missing, summary);
    }

    static int experienceScore(int candidate, int required) {
        if (required <= 0 || candidate >= required) {
            return 100;
        }
        return Math.max(0, Math.round(100f * candidate / required));
    }

    private static Set<String> normalise(Collection<String> skills) {
        return skills == null ? Set.of() : skills.stream().map(s -> s.toLowerCase(Locale.ROOT).trim())
                .collect(Collectors.toSet());
    }

    private static Set<String> tokens(String text) {
        if (text == null) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+"))
                .filter(t -> t.length() > 2 && !STOP_WORDS.contains(t))
                .collect(Collectors.toSet());
    }
}
