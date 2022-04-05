package ru.tilipod.feign.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class YandexResourceResponse {

    @JsonProperty("_embedded")
    private YandexEmbedded embedded;

    private String type;

    private String file;

    private String name;
}
