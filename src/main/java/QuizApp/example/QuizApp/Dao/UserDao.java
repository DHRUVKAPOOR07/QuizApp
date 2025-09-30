package QuizApp.example.QuizApp.Dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDao {
    private String name;
    private String email;
    private String phoneNumber;
    private String password;
    
}
