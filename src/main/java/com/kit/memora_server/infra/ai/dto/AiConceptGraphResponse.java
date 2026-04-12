package com.kit.memora_server.infra.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiConceptGraphResponse {

    private List<Node> nodes;
    private List<Edge> edges;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Node {
        private String id;
        private String label;
        private Integer importance;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Edge {
        private String source;
        private String target;
        private String label;
    }
}
