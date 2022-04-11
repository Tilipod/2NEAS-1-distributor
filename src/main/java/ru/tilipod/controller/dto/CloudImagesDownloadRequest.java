package ru.tilipod.controller.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import ru.tilipod.enums.CloudType;

@Data
@ApiModel(value = "CloudImagesDownloadRequest", description = "Запрос на выгрузку изображений из облака на диск")
public class CloudImagesDownloadRequest {

    @ApiModelProperty("Токен для авторизации в облаке")
    private String token;

    @ApiModelProperty("Путь к корневой папке в облаке")
    private String pathFrom;

    @ApiModelProperty("Путь к корневой папке на диске")
    private String pathTo;

    @ApiModelProperty("Тип облака")
    private CloudType cloudType;
}
