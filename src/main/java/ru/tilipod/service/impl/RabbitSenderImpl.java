package ru.tilipod.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.DistributeResultErrorMessage;
import ru.tilipod.amqp.message.DistributeResultMessage;
import ru.tilipod.amqp.message.DistributeResultSuccessMessage;
import ru.tilipod.service.RabbitSender;
import ru.tilipod.util.ExchangeNames;
import ru.tilipod.util.RoutingKeys;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitSenderImpl implements RabbitSender {

    private final static String JSON_PROCESSING_ERROR_MESSAGE = "{\"taskId\": %d, \"message\": \"%s\", \"className\": \"JsonProcessingException\"}";

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    private Message createMessage(DistributeResultMessage message) throws JsonProcessingException {
        String text = objectMapper.writeValueAsString(message);
        return MessageBuilder.withBody(text.getBytes(StandardCharsets.UTF_8))
                .build();
    }

    @Override
    public void sendErrorToScheduler(DistributeResultErrorMessage message) {
        try {
            rabbitTemplate.send(ExchangeNames.ERROR, RoutingKeys.DISTRIBUTOR_RESULT_KEY, createMessage(message));
        } catch (JsonProcessingException e1) {
            log.error("Ошибка сериализации {}: {}", message.getClass().getSimpleName(), e1.getMessage());
            rabbitTemplate.send(ExchangeNames.ERROR, RoutingKeys.DISTRIBUTOR_RESULT_KEY,
                    MessageBuilder.withBody(String.format(JSON_PROCESSING_ERROR_MESSAGE, message.getTaskId(), e1.getMessage())
                                    .getBytes(StandardCharsets.UTF_8))
                            .build()
            );
        }
    }

    @Override
    public void sendSuccessToScheduler(DistributeResultSuccessMessage message) {
        try {
            rabbitTemplate.send(ExchangeNames.SUCCESS, RoutingKeys.DISTRIBUTOR_RESULT_KEY, createMessage(message));
        } catch (JsonProcessingException e1) {
            log.error("Ошибка сериализации {}: {}", message.getClass().getSimpleName(), e1.getMessage());
            rabbitTemplate.send(ExchangeNames.ERROR, RoutingKeys.DISTRIBUTOR_RESULT_KEY,
                    MessageBuilder.withBody(String.format(JSON_PROCESSING_ERROR_MESSAGE, message.getTaskId(), e1.getMessage())
                                    .getBytes(StandardCharsets.UTF_8))
                            .build()
            );
        }
    }
}
