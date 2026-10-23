package sg.edu.ntu.taskflowapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.service.TaskSummaryHelper;

class TaskSummaryHelperTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 23);

    @Test
    void completedTaskIsDoneEvenWhenItsDueDateHasPassed() {
        Task task = task("Publish release", "Deployment completed", true, TODAY.minusDays(2));

        assertThat(TaskSummaryHelper.formatTasks(task, TODAY))
                .contains("Title: Publish release")
                .contains("Note: Deployment completed")
                .contains("Status: DONE")
                .contains("Due: 2026-10-21");
    }

    @Test
    void pendingTaskWithPastDueDateIsOverdue() {
        Task task = task("Resolve alert", null, false, TODAY.minusDays(1));

        assertThat(TaskSummaryHelper.formatTasks(task, TODAY))
                .contains("Note: No note")
                .contains("Status: OVERDUE")
                .contains("Due: 2026-10-22");
    }

    @Test
    void taskDueTodayIsPendingRatherThanOverdue() {
        Task task = task("Submit report", "   ", false, TODAY);

        assertThat(TaskSummaryHelper.formatTasks(task, TODAY))
                .contains("Note: No note")
                .contains("Status: PENDING")
                .contains("Due: 2026-10-23");
    }

    @Test
    void taskWithoutDueDateIsPending() {
        Task task = task("Refine backlog", null, false, null);

        assertThat(TaskSummaryHelper.formatTasks(task, TODAY))
                .contains("Status: PENDING")
                .contains("Due: No due date");
    }

    @Test
    void userPromptWrapsTheFormattedTaskList() {
        assertThat(TaskSummaryHelper.formatUserPrompt("- Title: Write tests"))
                .contains("Summarize the following tasks:")
                .contains("- Title: Write tests");
    }

    private Task task(String title, String note, boolean completed, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setNote(note);
        task.setCompleted(completed);
        task.setDueDate(dueDate);
        return task;
    }
}
