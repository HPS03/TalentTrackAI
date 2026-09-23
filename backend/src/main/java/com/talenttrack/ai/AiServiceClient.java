package com.talenttrack.ai;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Thin HTTP client for the Python FastAPI ai-service. */
@Component
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(AiProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));
        this.restClient = builder.baseUrl(props.baseUrl()).requestFactory(factory).build();
    }

    public AiModels.ParsedResume parseResume(byte[] content, String filename) {
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        return restClient.post().uri("/api/v1/resume/parse")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(AiModels.ParsedResume.class);
    }

    public AiModels.MatchResult match(AiModels.MatchRequest request) {
        return restClient.post().uri("/api/v1/match")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiModels.MatchResult.class);
    }

    public AiModels.RecommendResponse recommend(AiModels.RecommendRequest request) {
        return restClient.post().uri("/api/v1/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AiModels.RecommendResponse.class);
    }
}
