package QuizApp.example.QuizApp.Model;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user")
public class User {
    private String id;
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    private List<String> top5Quiz;  //id krani hai store
    private List<String> attemptedQuiz;
}
