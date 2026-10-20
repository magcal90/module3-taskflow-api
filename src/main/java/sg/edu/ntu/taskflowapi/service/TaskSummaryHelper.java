package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public final class TaskSummaryHelper {

    public static final String SYSTEM_PROMPT = """
            You summarize task lists.

            Give a short plain-English summary.

            Clearly state:
            - what is completed
            - what is still pending
            - what is overdue
            - which upcoming tasks should be prioritized based on due date

            Do not invent information.
            Do not change task titles or due dates.
            Keep the response concise.
            """;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TaskSummaryHelper() {
    }

    public static String formatUserPrompt(String taskList) {
        return """
                Summarize the following tasks:

                %s
                """.formatted(taskList);
    }

    public static String formatTasks(Task task, LocalDateTime now) {
        String dueAt = task.getDueAt() != null
                ? task.getDueAt().format(DATE_FORMATTER)
                : "No due date";

        String description = task.getDescription() != null
                && !task.getDescription().isBlank()
                        ? task.getDescription()
                        : "No description";

        return """
                - Title: %s
                  Description: %s
                  Status: %s
                  Due: %s
                """.formatted(
                task.getTitle(),
                description,
                resolveStatus(task, now),
                dueAt);
    }

    private static String resolveStatus(Task task, LocalDateTime now) {
        if (task.isCompleted()) {
            return "DONE";
        }
        if (task.getDueAt() != null && task.getDueAt().isBefore(now)) {
            return "OVERDUE";
        }
        return "PENDING";
    }
}