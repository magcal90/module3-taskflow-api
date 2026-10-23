package sg.edu.ntu.taskflowapi.model;

import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
import java.time.LocalDate;


/**
 * A unit of work with an optional due date and ordered child steps.
 */
@NoArgsConstructor
@Getter 
@Setter
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

    @Column(name = "due_date")
    @FutureOrPresent 
    private LocalDate dueDate;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Step> steps;

}
