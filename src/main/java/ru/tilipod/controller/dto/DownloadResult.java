package ru.tilipod.controller.dto;

import lombok.Data;

@Data
public class DownloadResult {

    private Long totalOffset;

    private Long downloadCount;

    public DownloadResult(Long totalOffset, Long downloadCount) {
        this.totalOffset = totalOffset;
        this.downloadCount = downloadCount;
    }
}
