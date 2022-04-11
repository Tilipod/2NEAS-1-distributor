package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public abstract class DistributeResultMessage {

    private Integer taskId;
}
