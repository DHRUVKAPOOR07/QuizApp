package QuizApp.example.QuizApp.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizAttempt {
    @Id
    private String id;
    private String userId;
    private String quizId;
    private int marksObtained;
    private List<QuestionAttempt> attemptedQuestions = new ArrayList<>();
    private LocalDateTime attemptedAt = LocalDateTime.now();
    private LocalDateTime submittedAt;
    private String status;
}
