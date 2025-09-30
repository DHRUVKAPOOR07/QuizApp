package QuizApp.example.QuizApp.Model;

import java.time.LocalDateTime;

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
    private LocalDateTime attemptedAt = LocalDateTime.now();
}
