package ru.tilipod.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import ru.tilipod.feign.model.YandexRefResponse;
import ru.tilipod.feign.model.YandexResourceResponse;

@FeignClient(name = "yandex-cloud", url = "${cloudUrls.yandex}")
public interface YandexFeignClient {

    @GetMapping("/disk/resources/download")
    ResponseEntity<YandexRefResponse> getRefForDownload(@RequestHeader String authorization,
                                                        @RequestParam String path);

    @GetMapping("/disk/resources")
    ResponseEntity<YandexResourceResponse> getMetadataForFiles(@RequestHeader String authorization,
                                                               @RequestParam String path);
}
