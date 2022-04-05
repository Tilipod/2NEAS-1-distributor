package ru.tilipod.feign.model;

import lombok.Data;

import java.util.List;

@Data
public class YandexEmbedded {

    private List<YandexItem> items;
}
