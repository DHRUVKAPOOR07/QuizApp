package QuizApp.example.QuizApp.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import QuizApp.example.QuizApp.Model.Quiz;

public interface QuizRepository extends MongoRepository<Quiz,String>{
    Optional<Quiz> findById(String id);
    List<Quiz> findAllByCreatedBy(String createdBy);
}
