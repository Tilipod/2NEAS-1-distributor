package ru.tilipod.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.tilipod.amqp.message.DistributeResultMessage;
import ru.tilipod.service.RabbitSender;
import ru.tilipod.util.ExchangeNames;
import ru.tilipod.util.RoutingKeys;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitSenderImpl implements RabbitSender {

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void sendToScheduler(DistributeResultMessage model) {
        Message message;
        try {
            String text = objectMapper.writeValueAsString(model);
            message = MessageBuilder.withBody(text.getBytes(StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException e1) {
            log.error("Ошибка сериализации ErrorMessage: {}", e1.getMessage());
            message = MessageBuilder.withBody(String.format("{\"taskId\": %d, \"message\": \"%s\", \"\": \"JsonProcessingException\"}",
                            model.getTaskId(), e1.getMessage()).getBytes(StandardCharsets.UTF_8))
                    .build();
        }

        rabbitTemplate.send(ExchangeNames.DISTRIBUTOR, RoutingKeys.DITRIBUTOR_RESULT_KEY, message);
    }
}
