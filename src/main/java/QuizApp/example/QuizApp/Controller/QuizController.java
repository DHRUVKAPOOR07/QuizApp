package QuizApp.example.QuizApp.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
            Map<String, Object> map = new HashMap<>();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Quiz quiz2 = new Quiz();
            quiz2.setQuizName(quiz.getQuizName());
            quiz2.setDuration(quiz.getDuration());
            quiz2.setPassingScore(quiz.getPassingScore());
            quiz2.setTotalMarks(quiz.getTotalMarks());
            quiz2.setTotalQues(quiz.getTotalQues());
            quiz2.setActive(true);
            quiz2.setCreatedAt(LocalDateTime.now());
            quiz2.setCreatedBy(email);
            quizRepository.save(quiz2);
            map.put("Message","Quiz created successfully");
            return ResponseEntity.ok().body(map);

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
        Map<String, Object> map = new HashMap<>();

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
        map.put("Message","Question added successfully");
        return ResponseEntity.ok().body(map);

    } catch (Exception e) {
        log.error("Error occurred : " + e.getMessage());
        return ResponseEntity.badRequest().body("Error occurred : " + e.getMessage());
    }
}
    @DeleteMapping("/deleteques")
    public ResponseEntity<?> deleteQues(@RequestParam String quizId,@RequestParam String quesId){
        try {
            Optional<Quiz> quiz = quizRepository.findById(quizId);
            Map<String, Object> map = new HashMap<>();
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
            map.put("Message", "Question deleted successfully");
            return ResponseEntity.ok().body(map);

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
        Optional<Quiz> quiz = quizRepository.findById(quizId);
        List<String> attemptedUsers = quiz.get().getAttempedUsersId();
        if (attemptedUsers == null) {
            attemptedUsers = new ArrayList<>(); 
            attemptedUsers.add(userId);
        }
        else{
            attemptedUsers.add(userId);
        }
        quiz.get().setAttempedUsersId(attemptedUsers);
        QuizAttempt savedAttempt = quizAttemptRepository.save(quizAttempt);
        quizRepository.save(quiz.get());
        return ResponseEntity.ok(savedAttempt);

    } catch (Exception e) {
        log.error("Error occurred: " + e.getMessage());
        return ResponseEntity.badRequest().body("Something went wrong: " + e.getMessage());
    }
}
   @PostMapping("/saveanswer")
public ResponseEntity<Map<String, Object>> saveAnswer(
        @RequestParam String attemptId,
        @RequestParam String questionId,
        @RequestParam String selectedOption) {

    Map<String, Object> response = new HashMap<>();

    try {
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Quiz attempt not found"));

        List<QuestionAttempt> qAttempts = attempt.getAttemptedQuestions();
        if (qAttempts == null) {
            qAttempts = new ArrayList<>();
        }

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
        QuizAttempt updatedAttempt = quizAttemptRepository.save(attempt);

        response.put("status", "success");
        response.put("message", "Answer submitted successfully");
        response.put("attempt", updatedAttempt);

        return ResponseEntity.ok(response);

    } catch (Exception e) {
        response.put("status", "error");
        response.put("message", "Something went wrong: " + e.getMessage());
        return ResponseEntity.status(500).body(response);
    }
}
    @PostMapping("/disturbanceDetected")
    public ResponseEntity<?> disturbanceDetected(@RequestParam String attemptId){
        try {
            Map<String, Object> map = new HashMap<>();
            Optional<QuizAttempt> attempt = quizAttemptRepository.findById(attemptId);
            int totalDisturbance = attempt.get().getTotalDisturbance();
            int updatedDisturbance = totalDisturbance+=1;
            attempt.get().setTotalDisturbance(updatedDisturbance);
            if(updatedDisturbance>=5){
                quizAttemptRepository.save(attempt.get());
                completeQuiz(attemptId);
                // map.put("DisturbanceMessage", "Quiz completed")
                return ResponseEntity.ok("Quiz completed successfully");
            }
            quizAttemptRepository.save(attempt.get());

            map.put("Message", "You have only : " + (5 - totalDisturbance) + "attempts.");
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }

   @PostMapping("/completequiz")
public ResponseEntity<?> completeQuiz(@RequestParam String attemptId) {
    try {
        Map<String, Object> map = new HashMap<>();
    QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new RuntimeException("Attempt not found"));

   
    if ("COMPLETED".equalsIgnoreCase(attempt.getStatus())) {
        return ResponseEntity.badRequest().body("Quiz already completed");
    }

    String quizId = attempt.getQuizId();
    String userId = attempt.getUserId();
    Optional<User> user = userRepository.findById(userId);
    Optional<Quiz> quiz = quizRepository.findById(quizId);

    if (!quiz.isPresent()) {
        return ResponseEntity.badRequest().body("Quiz not found");
    }

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
    if(marks>=quiz.get().getPassingScore()){
        attempt.setResult("PASS");
    }
    else{
        attempt.setResult("FAIL");
    }
    attempt.setMarksObtained(marks);
    attempt.setStatus("COMPLETED");
    attempt.setSubmittedAt(LocalDateTime.now());
    List<String> quizes = user.get().getAttemptedQuiz();
    if(quizes==null){
        quizes=new ArrayList<>();
    }
    quizes.add(quizId);
    user.get().setAttemptedQuiz(quizes);
    userRepository.save(user.get());
    quizAttemptRepository.save(attempt);
    map.put("Message", attempt);
    return ResponseEntity.ok(map);
    } catch (Exception e) {
        log.error("Error occured : ", e.getMessage());
        return ResponseEntity.badRequest().body("Error occured : "+e.getMessage());
    }
}

    @DeleteMapping("/deleteQuiz")
    public ResponseEntity<?> deleteQuiz(@RequestParam String quizId){
        try {
            Optional<Quiz> quiz = quizRepository.findById(quizId);
            Map<String, Object> map = new HashMap<>();
            if(!quiz.isPresent()){
                return ResponseEntity.badRequest().body("Quiz not found");
            }
            quizRepository.deleteById(quizId);
            map.put("Message", "Quiz deleted successfully");
            return ResponseEntity.ok().body(map);

        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }
    @GetMapping("/attemptedQuiz")
    public ResponseEntity<?> attemptedQuiz(@RequestParam String userId) {
    try {
        Optional<User> userOpt = userRepository.findById(userId);
        Map<String, Object> map = new HashMap<>();
        if (!userOpt.isPresent()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        List<String> quizIds = user.getAttemptedQuiz();

        if (quizIds == null || quizIds.isEmpty()) {
            map.put("Message", "No attempted quizzes found");
            return ResponseEntity.ok().body(map);
        }

        List<Quiz> attemptedQuizzes = quizRepository.findAllById(quizIds);
        map.put("Message", attemptedQuizzes);
        return ResponseEntity.ok(map);

    } catch (Exception e) {
        log.error("Error occurred : " + e.getMessage());
        return ResponseEntity.badRequest().body("Something went wrong : " + e.getMessage());
    }
}
    @PutMapping("/updateQuiz")
    public ResponseEntity<?> updateQuiz(@RequestParam String quizId, @RequestBody QuizDao updatedQuiz){
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();
            Map<String, Object> map = new HashMap<>();
            Optional<Quiz> quiz = quizRepository.findById(quizId);
            if(!quiz.isPresent()){
                map.put("Message", "Quiz not found");
                return ResponseEntity.badRequest().body(map);
            }
            if(!email.equals(quiz.get().getCreatedBy())){
                map.put("Message", "You cannot update this quiz because you are not its creator.");
                return ResponseEntity.badRequest().body(map);
            }
            quiz.get().setQuizName(updatedQuiz.getQuizName());
            quiz.get().setDuration(updatedQuiz.getDuration());
            quiz.get().setPassingScore(updatedQuiz.getPassingScore());
            quiz.get().setTotalMarks(updatedQuiz.getTotalMarks());
            quiz.get().setTotalQues(updatedQuiz.getTotalQues());
            quiz.get().setUpdatedAt(LocalDateTime.now());
            quizRepository.save(quiz.get());
            
            map.put("Message", "Quiz updated successfully");
            return ResponseEntity.ok().body(map);
            
        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }
    @GetMapping("/findQuiz")
    public ResponseEntity<?> findQuiz(@RequestParam String quizId){
        try {
            Map<String, Object> map = new HashMap<>();
            Optional<Quiz> quiz = quizRepository.findById(quizId);
            if(!quiz.isPresent()){
                map.put("Message", "Quiz not found");
                return ResponseEntity.badRequest().body(map);
            }
            map.put("Message", quiz.get());
            return ResponseEntity.ok().body(map);
        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }

}
