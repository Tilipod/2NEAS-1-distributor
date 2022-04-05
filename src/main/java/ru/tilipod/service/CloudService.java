package ru.tilipod.service;

import ru.tilipod.controller.dto.CloudImagesDownloadRequest;

public interface CloudService {

    void downloadImagesFromCloud(Integer taskId, CloudImagesDownloadRequest request);

}
