package QuizApp.example.QuizApp.Repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import QuizApp.example.QuizApp.Model.QuizAttempt;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt,String> {
    
}
