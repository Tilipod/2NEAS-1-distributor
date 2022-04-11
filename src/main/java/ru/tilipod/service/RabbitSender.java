package ru.tilipod.service;

import ru.tilipod.amqp.message.DistributeResultErrorMessage;
import ru.tilipod.amqp.message.DistributeResultSuccessMessage;

public interface RabbitSender {

    void sendErrorToScheduler(DistributeResultErrorMessage model);

    void sendSuccessToScheduler(DistributeResultSuccessMessage model);

}
