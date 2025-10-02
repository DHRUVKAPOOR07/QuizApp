package QuizApp.example.QuizApp.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import QuizApp.example.QuizApp.Dao.UserDao;
import QuizApp.example.QuizApp.Dao.UserLoginDao;
import QuizApp.example.QuizApp.Model.User;
import QuizApp.example.QuizApp.Repository.UserRepository;
import QuizApp.example.QuizApp.Utility.JwtUtil;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @PostMapping("/createUser")
    public ResponseEntity<?> createUser(@RequestBody UserDao user){
        try {
            if(user.getEmail()==null || user.getName()==null || user.getPassword()==null|| user.getPhoneNumber()==null){
                return ResponseEntity.badRequest().body("Fields can't be empty");
            }
            
            Optional<User> existEmail = userRepository.findByEmail(user.getEmail());
            Optional<User> existPhone = userRepository.findByEmail(user.getPhoneNumber());
            if(existEmail.isPresent() || existPhone.isPresent()){
                return ResponseEntity.ok().body("User already exists");
            }
            if(user.getPhoneNumber().length()!=10){
                return ResponseEntity.badRequest().body("Please enter correct phone number");

            }
            User user1 = new User();
            user1.setName(user.getName());
            user1.setEmail(user.getEmail());
            String hashedPass = passwordEncoder.encode(user.getPassword());
            user1.setPassword(hashedPass);
            user1.setPhoneNumber(user.getPhoneNumber());
            List<String> li = new ArrayList<>();
            li.add("ROLE_USER");
            user1.setRoles(li);

            userRepository.save(user1);
            return ResponseEntity.ok().body("User created successfully");
        } catch (Exception e) {
            log.error("Error occured : " + e.getMessage());
            System.out.println("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Error occured = "+e.getMessage());
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDao userLoginDao){
        try {
            Optional<User> user = userRepository.findByEmail(userLoginDao.getEmail());
            if(!user.isPresent()){
                return ResponseEntity.badRequest().body("User not found. Please enter valid credentials");
            }
            if(!(passwordEncoder.matches(userLoginDao.getPassword(), user.get().getPassword()))){
                return ResponseEntity.badRequest().body("Invalid username or password");
            }
            String token = jwtUtil.generateToken(userLoginDao.getEmail(), "USER", userLoginDao.getEmail());
            return ResponseEntity.ok().body("Login successfull"+"\n"+token);
        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }
}
