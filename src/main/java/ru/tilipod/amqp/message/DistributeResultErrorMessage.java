package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public class DistributeResultErrorMessage extends DistributeResultMessage  {

    private String className;

    private String message;

    public static DistributeResultErrorMessage createMessage(Integer taskId, Exception e) {
        DistributeResultErrorMessage model = new DistributeResultErrorMessage();

        model.setMessage(e.getMessage());
        model.setClassName(e.getClass().getName());
        model.setTaskId(taskId);

        return model;
    }
}
