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
@Document(collection = "Questions")
public class Questions {
    @Id
    private String id;
    private String questionText;
    private List<String> options;
    private String correctOption;
    private int timeLimit;
    
}
