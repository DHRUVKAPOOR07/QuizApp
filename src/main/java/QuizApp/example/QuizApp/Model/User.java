package QuizApp.example.QuizApp.Model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private List<String> top5Quiz;        // Store IDs of top 5 quizzes
    private List<String> attemptedQuiz;   // Store IDs of attempted quizzes
}
