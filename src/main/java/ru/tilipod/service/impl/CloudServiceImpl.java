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
import ru.tilipod.controller.dto.DownloadResult;
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

    private static final int PAGE_ITEMS_SIZE = 10;

    private final YandexFeignClient yandexFeignClient;

    private final RabbitSender rabbitSender;

    @Override
    @Async
    public void downloadImagesFromCloud(CloudImagesDownloadRequest request) {
        ResponseEntity<YandexResourceResponse> response;
        try {
            response = yandexFeignClient.getMetadataForFiles(request.getToken(), request.getPathFrom(), PAGE_ITEMS_SIZE, 0L);
            if (response == null || response.getBody() == null) {
                throw new CloudException("Пустой ответ от облака");
            }
        } catch (Exception e) {
            log.error("Ошибка подключения к облаку: {}", e.getMessage());
            rabbitSender.sendErrorToScheduler(DistributeResultErrorMessage.createMessage(request.getTaskId(), e));
            return;
        }

        DownloadResult result;
        log.info("Начинаем скачивание данных по задаче {}. Текущее смещение: {}, доп. выгрузка в кол-ве {} файлов",
                request.getTaskId(), request.getTotalOffset(), request.getMaxCounts());
        try {
            result = downloadAllFileFromDirCloud(request.getToken(), request.getPathFrom(), request.getPathTo(),
                    request.getTotalOffset(), request.getMaxCounts(), response.getBody());
        } catch (Exception e) {
            log.error("Ошибка скачивания файла из облака: {}", e.getMessage());
            rabbitSender.sendErrorToScheduler(DistributeResultErrorMessage.createMessage(request.getTaskId(), e));
            return;
        }
        log.info("Скачивание данных по задаче {} завершено. Скачано {} файлов", request.getTaskId(), result.getDownloadCount());

        rabbitSender.sendSuccessToScheduler(DistributeResultSuccessMessage.createMessage(request.getTaskId(),
                request.getPathTo(), result.getDownloadCount()));
    }

    private DownloadResult downloadAllFileFromDirCloud(String token, String pathFrom,
                                                       String pathTo, Long totalOffset, Long maxCounts,
                                                       YandexResourceResponse response) throws Exception {
        File file = new File(pathTo);
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new CloudException(String.format("Не удалось создать директорию на диске, путь: %s", pathTo));
            }
        }

        long currentOffset = 0;
        // Если в директории все файлы, нужно проверить смещение
        if (response.getEmbedded()
                .getItems()
                .stream()
                .allMatch(i -> YandexItemType.FILE.getName().equals(i.getType()))) {
            if (totalOffset >= response.getEmbedded().getTotal()) {
                return new DownloadResult(totalOffset - response.getEmbedded().getTotal(), 0L);
            } else if (totalOffset != 0) {
                // Иначе выкачивать файлы нужно со смещением
                response = yandexFeignClient.getMetadataForFiles(token, pathFrom, PAGE_ITEMS_SIZE, totalOffset).getBody();
                currentOffset += totalOffset;
            }
        }

        DownloadResult result = new DownloadResult(totalOffset, 0L);
        while (response != null && response.getEmbedded() != null && !response.getEmbedded().getItems().isEmpty()) {
            for (YandexItem item : response.getEmbedded().getItems()) {
                if (result.getDownloadCount() >= maxCounts) {
                    return result;
                }

                String newPathFrom = pathFrom.concat("/").concat(item.getName());
                String newPathTo = pathTo.concat("/").concat(item.getName());

                if (YandexItemType.DIRECTORY.getName().equals(item.getType())) {
                    DownloadResult inResult = downloadAllFileFromDirCloud(token, newPathFrom, newPathTo, result.getTotalOffset(),
                            maxCounts - result.getDownloadCount(), yandexFeignClient.getMetadataForFiles(token, newPathFrom, PAGE_ITEMS_SIZE, 0L).getBody());
                    result.setDownloadCount(result.getDownloadCount() + inResult.getDownloadCount());
                    result.setTotalOffset(inResult.getTotalOffset());
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

                    result.setDownloadCount(result.getDownloadCount() + 1);
                }
            }

            currentOffset += PAGE_ITEMS_SIZE;
            response = yandexFeignClient.getMetadataForFiles(token, pathFrom, PAGE_ITEMS_SIZE, currentOffset).getBody();
        }

        // Если прошерстили всю директорию, значит скачивание в след. нужно начинать с начала
        result.setTotalOffset(0L);

        return result;
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
