package ru.tilipod.feign.model;

import lombok.Getter;

public enum YandexItemType {
    DIRECTORY("dir"),
    FILE("file");

    @Getter
    private final String name;

    YandexItemType(String name) {
        this.name = name;
    }
}
