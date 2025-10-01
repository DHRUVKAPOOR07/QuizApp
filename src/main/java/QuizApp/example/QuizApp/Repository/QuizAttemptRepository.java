package QuizApp.example.QuizApp.Repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import QuizApp.example.QuizApp.Model.QuizAttempt;

public interface QuizAttemptRepository extends MongoRepository<QuizAttempt,String> {

    Optional<QuizAttempt> findByUserIdAndQuizIdAndStatus(String userId, String quizId, String string);

    Optional<QuizAttempt> findByUserIdAndQuizId(String userId, String quizId);
    
}
