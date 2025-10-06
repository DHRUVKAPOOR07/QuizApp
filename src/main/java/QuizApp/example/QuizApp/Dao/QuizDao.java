package QuizApp.example.QuizApp.Dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizDao {
    private String quizName;
    private double duration;
    private String description;
    private int totalQuestions;          // input from user
    private double passingPercentage;    // input from user
    private LocalDateTime quizDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
