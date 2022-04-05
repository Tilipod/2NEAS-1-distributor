package ru.tilipod.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.tilipod.controller.dto.CloudImagesDownloadRequest;
import ru.tilipod.service.CloudService;

@RestController
@RequestMapping("/dataset")
@RequiredArgsConstructor
@Api(description = "Контроллер для загрузки датасетов")
public class DatasetController {

    private final CloudService cloudService;

    @PostMapping("/{taskId}/cloud/images/download")
    @ApiOperation(value = "Выгрузить изображения с облака на диск")
    public ResponseEntity<Void> downloadImagesFromCloud(@PathVariable Integer taskId,
                                                        @RequestBody CloudImagesDownloadRequest request) {
        cloudService.downloadImagesFromCloud(taskId, request);
        return ResponseEntity.noContent().build();
    }

}
