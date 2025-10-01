package QuizApp.example.QuizApp.Model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionAttempt {
    private String questionId;
    private Integer selectedOption; 
    private Boolean correct;
}
