package QuizApp.example.QuizApp.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import QuizApp.example.QuizApp.Model.User;

@Repository
public interface UserRepository extends MongoRepository<User,String>{
    Optional<User> findByEmail(String email);
}
