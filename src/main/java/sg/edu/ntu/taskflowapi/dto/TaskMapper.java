package sg.edu.ntu.taskflowapi.dto;

import sg.edu.ntu.taskflowapi.model.Task;
import java.util.List;
import java.util.Optional;

public final class TaskMapper {

    private TaskMapper() {}

    public static Task toTask(TaskRequest taskRequest) {
        Task task = new Task();
        task.setTitle(taskRequest.title());
        task.setNote(taskRequest.note());
        task.setCompleted(taskRequest.completed());
        task.setDueDate(taskRequest.dueDate());
        
        return task;
    }

    public static TaskResponse toTaskResponse(Task task) {
        List<StepResponse> steps = Optional.ofNullable(task.getSteps())
                .map(stepList -> stepList.stream()
                        .map(StepMapper::toStepResponse)
                        .toList())
                .orElseGet(List::of);

        return new TaskResponse(task.getId(), task.getTitle(), task.getNote(), task.isCompleted(), task.getDueDate(),
                steps);

    }

}