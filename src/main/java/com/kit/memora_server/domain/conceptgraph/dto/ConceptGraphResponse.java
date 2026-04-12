package com.kit.memora_server.domain.conceptgraph.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "강의 자료 기반 개념 지식 그래프")
public class ConceptGraphResponse {

    private List<Node> nodes;
    private List<Edge> edges;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Node {
        private String id;
        private String label;
        private Integer importance;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Edge {
        private String source;
        private String target;
        private String label;
    }
}
