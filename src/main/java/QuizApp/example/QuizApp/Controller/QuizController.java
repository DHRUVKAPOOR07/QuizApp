package QuizApp.example.QuizApp.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import QuizApp.example.QuizApp.Dao.QuestionDao;
import QuizApp.example.QuizApp.Dao.QuizDao;
import QuizApp.example.QuizApp.Model.QuestionAttempt;
import QuizApp.example.QuizApp.Model.Questions;
import QuizApp.example.QuizApp.Model.Quiz;
import QuizApp.example.QuizApp.Model.QuizAttempt;
import QuizApp.example.QuizApp.Model.User;
import QuizApp.example.QuizApp.Repository.QuizAttemptRepository;
import QuizApp.example.QuizApp.Repository.QuizRepository;
import QuizApp.example.QuizApp.Repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/quiz")
@Slf4j
public class QuizController {
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuizRepository quizRepository;
    @PostMapping("/createQuiz")
    public ResponseEntity<?> createQuiz(@RequestBody QuizDao quiz){
        try {
            Quiz quiz2 = new Quiz();
            quiz2.setQuizName(quiz.getQuizName());
            quiz2.setDuration(quiz.getDuration());
            quiz2.setPassingScore(quiz.getPassingScore());
            quiz2.setTotalMarks(quiz.getTotalMarks());
            quiz2.setTotalQues(quiz.getTotalQues());
            quiz2.setActive(true);
            quiz2.setCreatedAt(LocalDateTime.now());

            quizRepository.save(quiz2);
            return ResponseEntity.ok().body("Quiz created successfully");

        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            System.out.println("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Error occured : "+e.getMessage());
        }
    }
    @PostMapping("/addQues")
    public ResponseEntity<?> addQues(@RequestBody QuestionDao questionDao, @RequestParam String quizId) {
    try {
        if (quizId == null) {
            return ResponseEntity.badRequest().body("Please enter quiz");
        }

        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (!quizOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Either quiz was expired or not found");
        }

        Quiz quiz = quizOpt.get();
        int total = quiz.getTotalQues();
        List<Questions> existingQuestions = quiz.getQuestions();
        if (existingQuestions == null) {
            existingQuestions = new ArrayList<>();
        }
        if (existingQuestions.size() >= total) {
            return ResponseEntity.badRequest().body("You have already reached the max limit of " + total + " questions.");
        }
        Questions questions = new Questions();
        questions.setId(UUID.randomUUID().toString());
        questions.setQuestionText(questionDao.getQuestionText());
        questions.setCorrectOption(questionDao.getCorrectOption());
        questions.setTimeLimit(questionDao.getTimeLimit());
        questions.setOptions(questionDao.getOptions());
        existingQuestions.add(questions);

        quiz.setQuestions(existingQuestions);
        quizRepository.save(quiz);

        return ResponseEntity.ok().body("Question added successfully");

    } catch (Exception e) {
        log.error("Error occurred : " + e.getMessage());
        return ResponseEntity.badRequest().body("Error occurred : " + e.getMessage());
    }
}
    @DeleteMapping("/deleteques")
    public ResponseEntity<?> deleteQues(@RequestParam String quizId,@RequestParam String quesId){
        try {
            Optional<Quiz> quiz = quizRepository.findById(quizId);
            if(!quiz.isPresent()){
                return ResponseEntity.badRequest().body("Either quiz was expired or not found");
            }
            List<Questions> li = quiz.get().getQuestions();
            if (li == null || li.isEmpty()) {
            return ResponseEntity.badRequest().body("No questions found in this quiz");

            }
            boolean removed = li.removeIf(q -> q.getId().equals(quesId));
            if (!removed) {
            return ResponseEntity.badRequest().body("Question not found in this quiz");
            }
            quiz.get().setQuestions(li);
            quizRepository.save(quiz.get());
            return ResponseEntity.ok().body("Question deleted successfully");

        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }
  @PostMapping("/startquiz")
public ResponseEntity<?> startquiz(@RequestParam String userId, @RequestParam String quizId) {
    try {
      
        Optional<QuizAttempt> existingAttempt = 
                quizAttemptRepository.findByUserIdAndQuizId(userId, quizId);

        if (existingAttempt.isPresent()) {
            QuizAttempt attempt = existingAttempt.get();
            if ("IN_PROGRESS".equals(attempt.getStatus())) {
                return ResponseEntity.ok(attempt);
            } else if ("COMPLETED".equals(attempt.getStatus())) {
                return ResponseEntity.badRequest().body("Quiz already submitted. Cannot start again.");
            }
        }
        QuizAttempt quizAttempt = new QuizAttempt();
        quizAttempt.setUserId(userId);
        quizAttempt.setQuizId(quizId);
        quizAttempt.setStatus("IN_PROGRESS");
        quizAttempt.setMarksObtained(0);
        quizAttempt.setAttemptedAt(LocalDateTime.now());

        QuizAttempt savedAttempt = quizAttemptRepository.save(quizAttempt);

        return ResponseEntity.ok(savedAttempt);

    } catch (Exception e) {
        log.error("Error occurred: " + e.getMessage());
        return ResponseEntity.badRequest().body("Something went wrong: " + e.getMessage());
    }
}
    @PostMapping("/saveanswer")
    public QuizAttempt saveAnswer(String attemptId, String questionId, int selectedOption) {
    QuizAttempt attempt = quizAttemptRepository.findById(attemptId).orElseThrow();

    List<QuestionAttempt> qAttempts = attempt.getAttemptedQuestions();
    QuestionAttempt existing = qAttempts.stream()
        .filter(q -> q.getQuestionId().equals(questionId))
        .findFirst()
        .orElse(null);

    if (existing != null) {
        existing.setSelectedOption(selectedOption);

    } else {
        QuestionAttempt newQ = new QuestionAttempt();
        newQ.setQuestionId(questionId);
        newQ.setSelectedOption(selectedOption);
        qAttempts.add(newQ);
    }

    attempt.setAttemptedQuestions(qAttempts);
    return quizAttemptRepository.save(attempt);
}

    @PostMapping("/completequiz")
    public QuizAttempt completeQuiz(@RequestParam String attemptId) {
    QuizAttempt attempt = quizAttemptRepository.findById(attemptId).orElseThrow();
    String quizId = attempt.getQuizId();
    Optional<Quiz> quiz = quizRepository.findById(quizId);
    List<Questions> actualQuestions = quiz.get().getQuestions();
    int marks = 0;
    for (QuestionAttempt qa : attempt.getAttemptedQuestions()) {
      Questions actualQ = actualQuestions.stream()
                        .filter(q -> q.getId().equals(qa.getQuestionId()))
                        .findFirst()
                        .orElse(null);

    if (actualQ != null) {
  
    if (qa.getSelectedOption() != null 
        && qa.getSelectedOption().equals(actualQ.getCorrectOption())) {
        marks++;
        qa.setCorrect(true);
    } else {
        qa.setCorrect(false);
    }
}

    }

    attempt.setMarksObtained(marks);
    attempt.setStatus("COMPLETED");

    return quizAttemptRepository.save(attempt);
}


}
