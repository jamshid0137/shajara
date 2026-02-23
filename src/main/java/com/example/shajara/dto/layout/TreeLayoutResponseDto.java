package com.example.shajara.dto.layout;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeLayoutResponseDto {
    private List<TreeNodeDto> nodes;
    private List<ConnectionDto> connections;

    private double minX;
    private double maxX;
    private double minY;
    private double maxY;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConnectionDto {
        private Long fromId;
        private Long toId;
        private String type; // PARENT_CHILD, SPOUSE
    }
}
