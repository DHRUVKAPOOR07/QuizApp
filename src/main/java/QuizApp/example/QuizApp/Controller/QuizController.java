package QuizApp.example.QuizApp.Controller;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import QuizApp.example.QuizApp.Utility.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = "http://localhost:5173")
public class QuizController {
    @Autowired
    private QuizAttemptRepository quizAttemptRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/createQuiz")
    public ResponseEntity<?> createQuiz(@RequestBody QuizDao quiz){
        try {
            Map<String, Object> map = new HashMap<>();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            // Fetch userId from email
            Optional<User> userOpt = userRepository.findByEmail(email);
            String userId = userOpt.map(User::getId).orElse(null);

            // Validate quizDate and startTime
            LocalDateTime now = LocalDateTime.now();
            if (quiz.getQuizDate() == null || quiz.getStartTime() == null) {
                map.put("Message", "Quiz date and start time are required");
                return ResponseEntity.badRequest().body(map);
            }

            LocalDateTime quizStart = LocalDateTime.of(quiz.getQuizDate().toLocalDate(), quiz.getStartTime());
            if (quizStart.isBefore(now)) {
                map.put("Message", "Quiz start time must be in the future");
                return ResponseEntity.badRequest().body(map);
            }

            // Optional: validate endTime > startTime
            if (quiz.getEndTime() != null) {
                LocalDateTime quizEnd = LocalDateTime.of(quiz.getQuizDate().toLocalDate(), quiz.getEndTime());
                if (!quizEnd.isAfter(quizStart)) {
                    map.put("Message", "Quiz end time must be after start time");
                    return ResponseEntity.badRequest().body(map);
                }
            }

            Quiz quiz2 = new Quiz();
            quiz2.setQuizName(quiz.getQuizName());
            quiz2.setDuration(quiz.getDuration());
            quiz2.setDescription(quiz.getDescription());
            quiz2.setTotalQuestions(quiz.getTotalQuestions());
            quiz2.setPassingScore((int) Math.ceil((quiz.getPassingPercentage()/100.0) * quiz.getTotalQuestions()));
            quiz2.setPassingPercentage(quiz.getPassingPercentage());
            quiz2.setQuizDate(quiz.getQuizDate());
            quiz2.setStartTime(quiz.getStartTime());
            quiz2.setEndTime(quiz.getEndTime());
            quiz2.setActive(true);
            quiz2.setCreatedAt(LocalDateTime.now());
            quiz2.setCreatedBy(userId);

            quizRepository.save(quiz2);

            map.put("message", "Quiz created successfully");
            map.put("status", true);
            map.put("quizId", quiz2.getId());

            return ResponseEntity.ok().body(map);

        } catch (Exception e) {
            log.error("Error occured : " + e.getMessage());
            return ResponseEntity.badRequest().body("Error occured : " + e.getMessage());
        }
    }

    @GetMapping("/myquizzes")
    public ResponseEntity<?> getMyQuizzes(@RequestHeader("Authorization") String authHeader) {
        try {
            Map<String, Object> map = new HashMap<>();

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                map.put("Message", "Authorization header missing or invalid");
                return ResponseEntity.status(401).body(map);
            }

            String token = authHeader.substring(7);  // Remove "Bearer "
            String userId = jwtUtil.extractUserId(token); // non-static call

            List<Quiz> myQuizzes = quizRepository.findAllByCreatedBy(userId);
            map.put("quizzes", myQuizzes);
            map.put("count", myQuizzes.size());

            return ResponseEntity.ok(map);

        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("Message", "Something went wrong: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }

    @GetMapping("/myquiz/{quizId}")
    public ResponseEntity<?> getSingleQuiz(
            @PathVariable String quizId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Map<String, Object> map = new HashMap<>();

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                map.put("Message", "Authorization header missing or invalid");
                return ResponseEntity.status(401).body(map);
            }

            // Token se userId extract karo
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            // Quiz fetch karo
            Optional<Quiz> quizOpt = quizRepository.findById(quizId);
            if (!quizOpt.isPresent()) {
                map.put("Message", "Quiz not found");
                return ResponseEntity.badRequest().body(map);
            }

            Quiz quiz = quizOpt.get();

            // Validate karo ki quiz createdBy same user hai
            if (!quiz.getCreatedBy().equals(userId)) {
                map.put("Message", "You are not authorized to view this quiz");
                return ResponseEntity.status(403).body(map);
            }

            map.put("quiz", quiz);
            map.put("status", true);
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            Map<String, Object> map = new HashMap<>();
            map.put("Message", "Something went wrong: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
        }
    }


    @PostMapping("/addQues")
    public ResponseEntity<?> addQues(
            @RequestBody QuestionDao questionDao,
            @RequestParam String quizId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Map<String, Object> map = new HashMap<>();

            if (quizId == null) {
                return ResponseEntity.badRequest().body("Please enter quiz");
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                map.put("Message", "Authorization header missing or invalid");
                return ResponseEntity.status(401).body(map);
            }

            // Token se userId nikal lo
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Optional<Quiz> quizOpt = quizRepository.findById(quizId);
            if (!quizOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Either quiz was expired or not found");
            }

            Quiz quiz = quizOpt.get();

            // Check if user is creator
            if (!quiz.getCreatedBy().equals(userId)) {
                map.put("Message", "You are not authorized to modify this quiz");
                return ResponseEntity.status(403).body(map);
            }

            int total = quiz.getTotalQuestions();
            List<Questions> existingQuestions = quiz.getQuestions();
            if (existingQuestions == null) {
                existingQuestions = new ArrayList<>();
            }

            // Check max limit
            if (existingQuestions.size() >= total) {
                return ResponseEntity.badRequest().body("You have already reached the max limit of " + total + " questions.");
            }

            // Check if same question already exists
            boolean questionExists = existingQuestions.stream()
                    .anyMatch(q -> q.getQuestionText().equalsIgnoreCase(questionDao.getQuestionText()));
            if (questionExists) {
                return ResponseEntity.badRequest().body("This question already exists in the quiz.");
            }

            // Add new question
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

    @DeleteMapping("/deletequestion")
    public ResponseEntity<?> deleteQuestion(
            @RequestParam String quizId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody QuestionDao questionTextDao) {

        try {
            Map<String, Object> map = new HashMap<>();

            if (quizId == null || questionTextDao.getQuestionText() == null) {
                return ResponseEntity.badRequest().body("Quiz ID and Question text are required");
            }

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                map.put("Message", "Authorization header missing or invalid");
                return ResponseEntity.status(401).body(map);
            }

            // Extract userId from token
            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Optional<Quiz> quizOpt = quizRepository.findById(quizId);
            if (!quizOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Quiz not found");
            }

            Quiz quiz = quizOpt.get();

            // Check if user is the creator
            if (!quiz.getCreatedBy().equals(userId)) {
                map.put("Message", "You are not authorized to delete questions from this quiz");
                return ResponseEntity.status(403).body(map);
            }

            List<Questions> existingQuestions = quiz.getQuestions();
            if (existingQuestions == null || existingQuestions.isEmpty()) {
                return ResponseEntity.badRequest().body("No questions found in this quiz");
            }

            boolean removed = existingQuestions.removeIf(
                    q -> q.getQuestionText().equalsIgnoreCase(questionTextDao.getQuestionText())
            );

            if (!removed) {
                return ResponseEntity.badRequest().body("Question not found in this quiz");
            }

            quiz.setQuestions(existingQuestions);
            quizRepository.save(quiz);

            map.put("Message", "Question deleted successfully");
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            log.error("Error occurred : " + e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong: " + e.getMessage());
        }
    }

    @PutMapping("/updatequiz")
    public ResponseEntity<?> updateQuiz(
            @RequestParam String quizId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody QuizDao updatedQuiz) {

        Map<String, Object> map = new HashMap<>();
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                map.put("Message", "Authorization header missing or invalid");
                return ResponseEntity.status(401).body(map);
            }

            String token = authHeader.substring(7);
            String userId = jwtUtil.extractUserId(token);

            Optional<Quiz> quizOpt = quizRepository.findById(quizId);
            if (!quizOpt.isPresent()) {
                map.put("Message", "Quiz not found with given ID");
                return ResponseEntity.badRequest().body(map);
            }

            Quiz quiz = quizOpt.get();

            // Owner validation
            if (!quiz.getCreatedBy().equals(userId)) {
                map.put("Message", "You are not authorized to update this quiz");
                return ResponseEntity.status(403).body(map);
            }

            // Validation: startTime & endTime must exist first
            if (updatedQuiz.getStartTime() != null && updatedQuiz.getEndTime() != null) {
                LocalTime start = updatedQuiz.getStartTime();
                LocalTime end = updatedQuiz.getEndTime();
                if (end.isBefore(start) || end.equals(start)) {
                    map.put("Message", "End time must be after start time");
                    return ResponseEntity.badRequest().body(map);
                }

                long diffMinutes = java.time.Duration.between(start, end).toMinutes();
                if (updatedQuiz.getDuration() * 60 > diffMinutes) {
                    map.put("Message", "Duration cannot exceed the difference between start and end time");
                    return ResponseEntity.badRequest().body(map);
                }

                quiz.setStartTime(start);
                quiz.setEndTime(end);
            } else if (updatedQuiz.getStartTime() != null && quiz.getEndTime() != null) {
                LocalTime start = updatedQuiz.getStartTime();
                LocalTime end = quiz.getEndTime();
                if (end.isBefore(start) || end.equals(start)) {
                    map.put("Message", "Start time cannot be after existing end time");
                    return ResponseEntity.badRequest().body(map);
                }

                long diffMinutes = java.time.Duration.between(start, end).toMinutes();
                if (updatedQuiz.getDuration() * 60 > diffMinutes) {
                    map.put("Message", "Duration cannot exceed the difference between start and end time");
                    return ResponseEntity.badRequest().body(map);
                }

                quiz.setStartTime(start);
            } else if (updatedQuiz.getEndTime() != null && quiz.getStartTime() != null) {
                LocalTime start = quiz.getStartTime();
                LocalTime end = updatedQuiz.getEndTime();
                if (end.isBefore(start) || end.equals(start)) {
                    map.put("Message", "End time cannot be before existing start time");
                    return ResponseEntity.badRequest().body(map);
                }

                long diffMinutes = java.time.Duration.between(start, end).toMinutes();
                if (updatedQuiz.getDuration() * 60 > diffMinutes) {
                    map.put("Message", "Duration cannot exceed the difference between start and end time");
                    return ResponseEntity.badRequest().body(map);
                }

                quiz.setEndTime(end);
            }

            // Update other fields if provided
            if (updatedQuiz.getQuizName() != null) quiz.setQuizName(updatedQuiz.getQuizName());
            if (updatedQuiz.getDescription() != null) quiz.setDescription(updatedQuiz.getDescription());
            if (updatedQuiz.getDuration() != 0) quiz.setDuration(updatedQuiz.getDuration());
            if (updatedQuiz.getTotalQuestions() != 0) {
                quiz.setTotalQuestions(updatedQuiz.getTotalQuestions());
                quiz.setTotalMarks(updatedQuiz.getTotalQuestions());
            }
            if (updatedQuiz.getPassingPercentage() != 0) {
                quiz.setPassingPercentage(updatedQuiz.getPassingPercentage());
                quiz.setPassingScore((int) Math.ceil(
                        (updatedQuiz.getPassingPercentage() / 100.0) *
                                (updatedQuiz.getTotalQuestions() != 0 ? updatedQuiz.getTotalQuestions() : quiz.getTotalMarks())
                ));
            }
            if (updatedQuiz.getQuizDate() != null) quiz.setQuizDate(updatedQuiz.getQuizDate());

            quiz.setUpdatedAt(LocalDateTime.now());
            quizRepository.save(quiz);

            map.put("Message", "Quiz updated successfully");
            map.put("quiz", quiz);
            map.put("status", true);
            return ResponseEntity.ok(map);

        } catch (Exception e) {
            log.error("Error occurred: " + e.getMessage());
            map.put("Message", "Something went wrong: " + e.getMessage());
            return ResponseEntity.badRequest().body(map);
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
        map.put("status",true);
        map.put("Message", attemptedQuizzes);
        return ResponseEntity.ok(map);

    } catch (Exception e) {
        log.error("Error occurred : " + e.getMessage());
        return ResponseEntity.badRequest().body("Something went wrong : " + e.getMessage());
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
