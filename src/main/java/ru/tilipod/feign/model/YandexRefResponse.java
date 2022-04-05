package ru.tilipod.feign.model;

import lombok.Data;

@Data
public class YandexRefResponse {

    private String href;

    private String method;

    private Boolean templated;

}
