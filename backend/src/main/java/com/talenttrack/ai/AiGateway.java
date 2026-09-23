package com.talenttrack.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Facade the rest of the backend talks to. Calls the Python AI microservice and
 * falls back to {@link LocalMatcher} if the service is down or times out.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGateway {

    private final AiServiceClient client;

    public Optional<AiModels.ParsedResume> parseResume(byte[] content, String filename) {
        try {
            return Optional.ofNullable(client.parseResume(content, filename));
        } catch (RestClientException e) {
            log.warn("AI resume parsing unavailable: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public AiModels.MatchResult match(AiModels.MatchRequest request) {
        try {
            AiModels.MatchResult result = client.match(request);
            if (result != null) {
                return result;
            }
        } catch (RestClientException e) {
            log.warn("AI matching unavailable, using local matcher: {}", e.getMessage());
        }
        AiModels.MatchResult local = LocalMatcher.match(request);
        return new AiModels.MatchResult(local.overall(), local.skillScore(), local.semanticScore(),
                local.experienceScore(), local.matchedSkills(), local.missingSkills(),
                "Estimated with the built-in matcher (AI service unavailable). " + local.summary());
    }

    public List<AiModels.Recommendation> recommend(AiModels.RecommendRequest request) {
        if (request.jobs().isEmpty()) {
            return List.of();
        }
        try {
            AiModels.RecommendResponse response = client.recommend(request);
            if (response != null && response.results() != null) {
                return response.results();
            }
        } catch (RestClientException e) {
            log.warn("AI recommendations unavailable, using local matcher: {}", e.getMessage());
        }
        return request.jobs().stream()
                .map(job -> {
                    var r = LocalMatcher.match(new AiModels.MatchRequest(request.candidateText(),
                            request.candidateSkills(), request.candidateExperience(), job.text(), job.skills(),
                            job.minExperience()));
                    return new AiModels.Recommendation(job.id(), r.overall(), r.matchedSkills(), r.missingSkills());
                })
                .sorted(Comparator.comparingInt(AiModels.Recommendation::score).reversed())
                .limit(request.topK())
                .toList();
    }
}
