package QuizApp.example.QuizApp.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import QuizApp.example.QuizApp.Dao.QuestionDao;
import QuizApp.example.QuizApp.Dao.QuizDao;
import QuizApp.example.QuizApp.Model.Questions;
import QuizApp.example.QuizApp.Model.Quiz;
import QuizApp.example.QuizApp.Repository.QuizRepository;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/quiz")
@Slf4j
public class QuizController {
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

}
