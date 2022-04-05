package ru.tilipod.service;

import org.springframework.amqp.core.Message;
import ru.tilipod.amqp.message.DistributeResultMessage;

public interface RabbitSender {

    void sendToScheduler(DistributeResultMessage model);

}
