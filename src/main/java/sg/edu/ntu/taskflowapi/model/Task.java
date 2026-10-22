package sg.edu.ntu.taskflowapi.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.CascadeType;
import java.util.List;
import java.time.LocalDateTime;


@NoArgsConstructor
@Data
@JsonPropertyOrder({ "id", "title", "description", "completed", "dueAt" })
@Entity
@Table(name = "task")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    @NotBlank(message = "Title is mandatory")
    private String title;

    @Column(name = "note")
    private String note;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "due_at")
    @FutureOrPresent 
    private LocalDateTime dueAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Step> steps;

}
