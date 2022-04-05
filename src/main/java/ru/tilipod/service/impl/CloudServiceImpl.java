package ru.tilipod.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.DistributeResultMessage;
import ru.tilipod.controller.dto.CloudImagesDownloadRequest;
import ru.tilipod.exception.CloudException;
import ru.tilipod.feign.YandexFeignClient;
import ru.tilipod.feign.model.YandexItem;
import ru.tilipod.feign.model.YandexResourceResponse;
import ru.tilipod.service.CloudService;
import ru.tilipod.service.RabbitSender;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudServiceImpl implements CloudService {

    @Value("${storage.pathToSave}")
    private String pathToSave;

    private final YandexFeignClient yandexFeignClient;

    private final RabbitSender rabbitSender;

    @Override
    @Async
    public void downloadImagesFromCloud(Integer taskId, CloudImagesDownloadRequest request) {
        ResponseEntity<YandexResourceResponse> response;
        try {
            response = yandexFeignClient.getMetadataForFiles(request.getToken(), request.getPathFrom());
            if (response == null || response.getBody() == null) {
                throw new CloudException("Пустой ответ от облака");
            }
        } catch (Exception e) {
            log.error("Ошибка подключения к облаку: {}", e.getMessage());
            rabbitSender.sendToScheduler(DistributeResultMessage.createErrorMessage(taskId, e));
            return;
        }

        // Создаем корневую директорию
        String pathTo = pathToSave.concat(UUID.randomUUID().toString());
        try {
            downloadAllFileFromDirCloud(request.getToken(), request.getPathFrom(), pathTo, response.getBody());
        } catch (Exception e) {
            log.error("Ошибка скачивания файла из облака: {}", e.getMessage());
            rabbitSender.sendToScheduler(DistributeResultMessage.createErrorMessage(taskId, e));
            return;
        }

        rabbitSender.sendToScheduler(DistributeResultMessage.createSuccessMessage(taskId, pathTo));
    }

    private void downloadAllFileFromDirCloud(String token, String pathFrom,
                                             String pathTo, YandexResourceResponse response) throws Exception {
        File file = new File(pathTo);
        if (!file.mkdir()) {
            throw new CloudException("Не удалось создать директорию на диске");
        }

        for (YandexItem item : response.getEmbedded().getItems()) {
            String newPathFrom = pathFrom.concat("/").concat(item.getName());
            String newPathTo = pathTo.concat("/").concat(item.getName());

            if ("dir".equals(item.getType())) {
                downloadAllFileFromDirCloud(token, newPathFrom, newPathTo,
                        yandexFeignClient.getMetadataForFiles(token, newPathFrom).getBody());
            } else if ("file".equals(item.getType())) {
                downloadFileFromCloud(item.getFile(), newPathTo, token);
            }
        }
    }

    private void downloadFileFromCloud(String href, String pathTo, String token) throws Exception {
        URL url = new URL(href);
        File file = new File(pathTo);
        if (!file.createNewFile()) {
            throw new CloudException("Не удалось создать файл на диске");
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "OAuth ".concat(token));

        FileUtils.copyInputStreamToFile(connection.getInputStream(), file);
    }
}
