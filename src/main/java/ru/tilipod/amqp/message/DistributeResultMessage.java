package ru.tilipod.amqp.message;

import lombok.Data;

@Data
public class DistributeResultMessage {

    private String className;

    private String message;

    private Integer taskId;

    private String pathTo;

    public static DistributeResultMessage createErrorMessage(Integer taskId, Exception e) {
        DistributeResultMessage model = new DistributeResultMessage();

        model.setMessage(e.getMessage());
        model.setClassName(e.getClass().getName());
        model.setTaskId(taskId);

        return model;
    }

    public static DistributeResultMessage createSuccessMessage(Integer taskId, String pathTo) {
        DistributeResultMessage model = new DistributeResultMessage();

        model.setTaskId(taskId);
        model.setPathTo(pathTo);

        return model;
    }
}
