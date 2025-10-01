package QuizApp.example.QuizApp.Dao;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionDao {
    private String questionText;
    private String correctOption;
    private List<String> options;
    private int timeLimit;
}
