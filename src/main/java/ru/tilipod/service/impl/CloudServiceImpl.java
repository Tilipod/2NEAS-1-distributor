package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.DistributeResultErrorMessage;
import ru.tilipod.amqp.message.DistributeResultSuccessMessage;
import ru.tilipod.controller.dto.CloudImagesDownloadRequest;
import ru.tilipod.exception.CloudException;
import ru.tilipod.feign.YandexFeignClient;
import ru.tilipod.feign.model.YandexItem;
import ru.tilipod.feign.model.YandexItemType;
import ru.tilipod.feign.model.YandexResourceResponse;
import ru.tilipod.service.CloudService;
import ru.tilipod.service.RabbitSender;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudServiceImpl implements CloudService {

    private static final int PAGE_ITEMS_SIZE = 1;

    private final YandexFeignClient yandexFeignClient;

    private final RabbitSender rabbitSender;

    @Override
    @Async
    public void downloadImagesFromCloud(CloudImagesDownloadRequest request) {
        ResponseEntity<YandexResourceResponse> response;
        try {
            response = yandexFeignClient.getMetadataForFiles(request.getToken(), request.getPathFrom(), PAGE_ITEMS_SIZE, 0);
            if (response == null || response.getBody() == null) {
                throw new CloudException("Пустой ответ от облака");
            }
        } catch (Exception e) {
            log.error("Ошибка подключения к облаку: {}", e.getMessage());
            rabbitSender.sendErrorToScheduler(DistributeResultErrorMessage.createMessage(request.getTaskId(), e));
            return;
        }

        try {
            downloadAllFileFromDirCloud(request.getToken(), request.getPathFrom(), request.getPathTo(), response.getBody());
        } catch (Exception e) {
            log.error("Ошибка скачивания файла из облака: {}", e.getMessage());
            rabbitSender.sendErrorToScheduler(DistributeResultErrorMessage.createMessage(request.getTaskId(), e));
            return;
        }

        rabbitSender.sendSuccessToScheduler(DistributeResultSuccessMessage.createMessage(request.getTaskId(), request.getPathTo()));
    }

    private void downloadAllFileFromDirCloud(String token, String pathFrom,
                                             String pathTo, YandexResourceResponse response) throws Exception {
        File file = new File(pathTo);
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new CloudException(String.format("Не удалось создать директорию на диске, путь: %s", pathTo));
            }
        }

        int currentOffset = 0; // Яндекс.Диск присылает метаданные постранично через лимит и смещение
        while (response != null && response.getEmbedded() != null && !response.getEmbedded().getItems().isEmpty()) {
            for (YandexItem item : response.getEmbedded().getItems()) {
                String newPathFrom = pathFrom.concat("/").concat(item.getName());
                String newPathTo = pathTo.concat("/").concat(item.getName());

                if (YandexItemType.DIRECTORY.getName().equals(item.getType())) {
                    downloadAllFileFromDirCloud(token, newPathFrom, newPathTo, yandexFeignClient
                            .getMetadataForFiles(token, newPathFrom, PAGE_ITEMS_SIZE, 0).getBody());
                } else if (YandexItemType.FILE.getName().equals(item.getType())) {
                    try {
                        downloadFileFromCloud(item.getFile(), newPathTo, token);
                    } catch (Exception e) {
                        // При ошибке нужно удалить последний битый файл
                        File fileError = new File(newPathTo);

                        if (fileError.exists() && !fileError.delete()) {
                            log.warn("Файл по пути {} не был удален с диска", newPathTo);
                        }

                        throw e;
                    }
                }
            }

            currentOffset += PAGE_ITEMS_SIZE;
            response = yandexFeignClient.getMetadataForFiles(token, pathFrom, PAGE_ITEMS_SIZE, currentOffset).getBody();
        }
    }

    private void downloadFileFromCloud(String href, String pathTo, String token) throws Exception {
        URL url = new URL(href);
        File file = new File(pathTo);

        // Если файл уже существует, то не скачиваем его снова
        if (file.exists()) {
            return;
        }

        if (!file.createNewFile()) {
            throw new CloudException(String.format("Не удалось создать файл на диске, путь: %s", pathTo));
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth ".concat(token));

        FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
    }
}
