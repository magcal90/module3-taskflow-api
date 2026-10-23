package sg.edu.ntu.taskflowapi;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.ntu.taskflowapi.model.Task;
import sg.edu.ntu.taskflowapi.repository.TaskRepository;
import sg.edu.ntu.taskflowapi.service.TaskService;
import sg.edu.ntu.taskflowapi.service.TaskServiceImpl;
import org.springframework.ai.chat.client.ChatClient;

import java.lang.reflect.Proxy;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "test", "jpa" })
@Transactional
@Import(TaskApiIntegrationTest.DeterministicAiConfiguration.class)
class TaskApiIntegrationTest {

    private static final long MISSING_ID = 999_999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskService taskService;

    @Test
    void jpaProfileSelectsJpaTaskService() {
        org.assertj.core.api.Assertions.assertThat(taskService).isInstanceOf(TaskServiceImpl.class);
    }

    @Test
    void createTaskReturnsCreatedTaskWithGeneratedId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Prepare integration demo", "Cover the HTTP flow", false,
                                LocalDate.now().plusDays(5))))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Prepare integration demo"))
                .andExpect(jsonPath("$.note").value("Cover the HTTP flow"))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.dueDate").value(LocalDate.now().plusDays(5).toString()))
                .andReturn();
        printResponse(result);

        long taskId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Task persisted = taskRepository.findById(taskId).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getTitle()).isEqualTo("Prepare integration demo");
        org.assertj.core.api.Assertions.assertThat(persisted.getNote()).isEqualTo("Cover the HTTP flow");
        org.assertj.core.api.Assertions.assertThat(persisted.isCompleted()).isFalse();
        org.assertj.core.api.Assertions.assertThat(persisted.getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
    }

    @Test
    void createTaskRejectsBlankTitle() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("   ", "Invalid request", false, LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is mandatory")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();
        printResponse(result);
    }

    @Test
    void createTaskRejectsPastDueDate() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Already late", null, false, LocalDate.now().minusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn();
        printResponse(result);
    }

    @Test
    void listTasksIncludesCreatedRecords() throws Exception {
        persistTask("Write report", "Draft the findings", false, LocalDate.now().plusDays(2));
        persistTask("Archive notes", null, true, null);

        MvcResult result = mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItem("Write report")))
                .andExpect(jsonPath("$[*].title", hasItem("Archive notes")))
                .andReturn();
        printResponse(result);
    }

    @Test
    void getTaskReturnsExistingTask() throws Exception {
        Task task = persistTask("Review pull request", "Check edge cases", false, null);

        MvcResult result = mockMvc.perform(get("/api/tasks/{id}", task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Review pull request"))
                .andReturn();
        printResponse(result);
    }

    @Test
    void getTaskReturnsNotFoundForUnknownId() throws Exception {
        assertTaskNotFound(mockMvc.perform(get("/api/tasks/{id}", MISSING_ID)).andReturn(), MISSING_ID);
    }

    @Test
    void updateTaskReplacesEditableFields() throws Exception {
        Task task = persistTask("Initial title", "Initial note", false, LocalDate.now().plusDays(2));

        MvcResult result = mockMvc.perform(put("/api/tasks/{id}", task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Revised title", "Revised note", true, LocalDate.now().plusDays(8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Revised title"))
                .andExpect(jsonPath("$.note").value("Revised note"))
                .andExpect(jsonPath("$.completed").value(true))
                .andReturn();
        printResponse(result);

        Task persisted = taskRepository.findById(task.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(persisted.getTitle()).isEqualTo("Revised title");
        org.assertj.core.api.Assertions.assertThat(persisted.getNote()).isEqualTo("Revised note");
        org.assertj.core.api.Assertions.assertThat(persisted.isCompleted()).isTrue();
        org.assertj.core.api.Assertions.assertThat(persisted.getDueDate()).isEqualTo(LocalDate.now().plusDays(8));
    }

    @Test
    void updateTaskReturnsNotFoundForUnknownId() throws Exception {
        MvcResult result = mockMvc.perform(put("/api/tasks/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskJson("Valid replacement", null, false, null)))
                .andReturn();
        assertTaskNotFound(result, MISSING_ID);
    }

    @Test
    void deleteTaskRemovesExistingTask() throws Exception {
        Task task = persistTask("Disposable task", null, false, null);

        MvcResult deleteResult = mockMvc.perform(delete("/api/tasks/{id}", task.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn();
        printResponse(deleteResult);

        MvcResult getResult = mockMvc.perform(get("/api/tasks/{id}", task.getId())).andReturn();
        assertTaskNotFound(getResult, task.getId());
    }

    @Test
    void deleteTaskReturnsNotFoundForUnknownId() throws Exception {
        assertTaskNotFound(mockMvc.perform(delete("/api/tasks/{id}", MISSING_ID)).andReturn(), MISSING_ID);
    }

    @Test
    void completeTaskMarksExistingTaskCompleted() throws Exception {
        Task task = persistTask("Submit assignment", null, false, LocalDate.now().plusDays(1));

        MvcResult result = mockMvc.perform(put("/api/tasks/{id}/complete", task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andReturn();
        printResponse(result);

        org.assertj.core.api.Assertions.assertThat(
                taskRepository.findById(task.getId()).orElseThrow().isCompleted()).isTrue();
    }

    @Test
    void completeTaskReturnsNotFoundForUnknownId() throws Exception {
        assertTaskNotFound(mockMvc.perform(put("/api/tasks/{id}/complete", MISSING_ID)).andReturn(), MISSING_ID);
    }

    @Test
    void keywordSearchMatchesTitleAndNoteAndReturnsEmptyForNoMatch() throws Exception {
        persistTask("Book dentist", "Annual check-up", false, null);
        persistTask("Buy groceries", "Include dental floss", false, null);
        persistTask("Plan holiday", "Compare flights", false, null);

        MvcResult matches = mockMvc.perform(get("/api/tasks/search").param("keyword", "dent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItem("Book dentist")))
                .andExpect(jsonPath("$[*].title", hasItem("Buy groceries")))
                .andReturn();
        printResponse(matches);

        MvcResult noMatches = mockMvc.perform(get("/api/tasks/search").param("keyword", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", empty()))
                .andReturn();
        printResponse(noMatches);
    }

    @Test
    void completedSearchSeparatesCompletedAndPendingTasks() throws Exception {
        persistTask("Finished migration", null, true, null);
        persistTask("Pending migration", null, false, LocalDate.now().plusDays(4));

        MvcResult completed = mockMvc.perform(get("/api/tasks/completed").param("completed", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Finished migration"))
                .andReturn();
        printResponse(completed);

        MvcResult pending = mockMvc.perform(get("/api/tasks/completed").param("completed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Pending migration"))
                .andReturn();
        printResponse(pending);
    }

    @Test
    void dueBeforeReturnsOnlyPendingTasksBeforeCutoffInDateOrder() throws Exception {
        LocalDate cutoff = LocalDate.now();
        persistTask("Older overdue", null, false, cutoff.minusDays(5));
        persistTask("Recent overdue", null, false, cutoff.minusDays(1));
        persistTask("Future pending", null, false, cutoff.plusDays(3));
        persistTask("Completed but overdue", null, true, cutoff.minusDays(2));
        persistTask("Pending without due date", null, false, null);

        MvcResult result = mockMvc.perform(get("/api/tasks/due-before").param("dueDate", cutoff.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Older overdue"))
                .andExpect(jsonPath("$[1].title").value("Recent overdue"))
                .andReturn();
        printResponse(result);
    }

    @Test
    void summaryReturnsFixedMessageWhenThereAreNoTasks() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("There are no tasks."))
                .andReturn();
        printResponse(result);
    }

    @Test
    void summaryReturnsDeterministicAiResponseForMeaningfulTaskMixture() throws Exception {
        LocalDate today = LocalDate.now();
        persistTask("Publish release", "Deployment completed", true, today.minusDays(2));
        persistTask("Resolve production alert", "Investigate elevated errors", false, today.minusDays(1));
        persistTask("Prepare sprint review", "Collect team highlights", false, today.plusDays(2));
        persistTask("Refine backlog", null, false, null);

        MvcResult result = mockMvc.perform(get("/api/tasks/summary"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("One task is done; prioritize the overdue production alert."))
                .andReturn();
        printResponse(result);
    }

    private Task persistTask(String title, String note, boolean completed, LocalDate dueDate) {
        Task task = new Task();
        task.setTitle(title);
        task.setNote(note);
        task.setCompleted(completed);
        task.setDueDate(dueDate);
        return taskRepository.saveAndFlush(task);
    }

    private String taskJson(String title, String note, boolean completed, LocalDate dueDate) {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "note", note == null ? "" : note,
                "completed", completed,
                "dueDate", dueDate == null ? "" : dueDate.toString()));
    }

    private void assertTaskNotFound(MvcResult result, long expectedId) throws Exception {
        printResponse(result);
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getStatus()).isEqualTo(404);
        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                .contains("Task not found: " + expectedId)
                .contains("timestamp");
    }

    private void printResponse(MvcResult result) throws Exception {
        System.out.println("Response Code: " + result.getResponse().getStatus());
        System.out.println("Response Body: " + result.getResponse().getContentAsString());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DeterministicAiConfiguration {
        private static final String SUMMARY =
                "One task is done; prioritize the overdue production alert.";

        @Bean
        @Primary
        ChatClient deterministicChatClient() {
            return proxyFor(ChatClient.class);
        }

        private static <T> T proxyFor(Class<T> interfaceType) {
            Object proxy = Proxy.newProxyInstance(
                    interfaceType.getClassLoader(),
                    new Class<?>[] { interfaceType },
                    (ignored, method, arguments) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "hashCode" -> System.identityHashCode(ignored);
                                case "equals" -> ignored == arguments[0];
                                case "toString" -> "DeterministicChatClient";
                                default -> null;
                            };
                        }
                        if (method.getName().equals("content") && method.getReturnType() == String.class) {
                            return SUMMARY;
                        }
                        if (method.getReturnType().isInterface()) {
                            return proxyFor(method.getReturnType());
                        }
                        return null;
                    });
            return interfaceType.cast(proxy);
        }
    }
}
