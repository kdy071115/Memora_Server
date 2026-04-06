package com.kit.memora_server.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공지사항 작성/수정 요청")
public class NoticeRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200)
    @Schema(description = "공지 제목", example = "중간고사 일정 안내", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "공지 본문", example = "중간고사는 4월 20일 오후 2시에 진행됩니다.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "상단 고정 여부", example = "true", defaultValue = "false")
    private boolean pinned = false;
}
