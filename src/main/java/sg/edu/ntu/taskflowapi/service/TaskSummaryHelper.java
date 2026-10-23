package sg.edu.ntu.taskflowapi.service;

import sg.edu.ntu.taskflowapi.model.Task;

import java.time.LocalDate;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;


/** Formats task data and prompts for AI summary generation. */
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

    private TaskSummaryHelper() {
    }

    /**
     * Wraps formatted task data in the user prompt.
     *
     * @param taskList formatted task entries
     * @return the complete user prompt
     */
    public static String formatUserPrompt(String taskList) {
        return """
                Summarize the following tasks:

                %s
                """.formatted(taskList);
    }

    /**
     * Formats a task with a deterministic status relative to a supplied date.
     *
     * @param task the task to format
     * @param today the date used to determine overdue status
     * @return the formatted task entry
     */
    public static String formatTasks(Task task, LocalDate today) {
        String dueDate = task.getDueDate() != null
                ? task.getDueDate().format(ISO_LOCAL_DATE)
                : "No due date";

        String note = task.getNote() != null
                && !task.getNote().isBlank()
                        ? task.getNote()
                        : "No note";

        return """
                - Title: %s
                  Note: %s
                  Status: %s
                  Due: %s
                """.formatted(
                task.getTitle(),
                note,
                resolveStatus(task, today),
                dueDate);
    }

    private static String resolveStatus(Task task, LocalDate today) {
        if (task.isCompleted()) {
            return "DONE";
        }
        if (task.getDueDate() != null && task.getDueDate().isBefore(today)) {
            return "OVERDUE";
        }
        return "PENDING";
    }
}
