package QuizApp.example.QuizApp.Dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizDao {
    private String quizName;
    private double duration;
    private int totalQues;
    private int totalMarks;
    private int passingScore;
}
