package QuizApp.example.QuizApp.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import QuizApp.example.QuizApp.Dao.UserDao;
import QuizApp.example.QuizApp.Dao.UserLoginDao;
import QuizApp.example.QuizApp.Model.User;
import QuizApp.example.QuizApp.Repository.UserRepository;
import QuizApp.example.QuizApp.Utility.JwtUtil;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping("/api/user")
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
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
            Map<String, Object> map = new HashMap<>();
            if(user.getEmail()==null || user.getUsername()==null || user.getPassword()==null){
                map.put("Message", "Fields can't be empty");
                return ResponseEntity.badRequest().body(map);
            }
            
            Optional<User> existEmail = userRepository.findByEmail(user.getEmail());
            if(existEmail.isPresent()){
                map.put("Message", "User already exists");
                return ResponseEntity.ok().body(map);
            }

            User user1 = new User();
            user1.setUsername(user.getUsername());
            user1.setEmail(user.getEmail());
            String hashedPass = passwordEncoder.encode(user.getPassword());
            user1.setPassword(hashedPass);
            List<String> li = new ArrayList<>();
            li.add("ROLE_USER");
            user1.setRoles(li);

            userRepository.save(user1);
            map.put("Message", "User created successfully");
            return ResponseEntity.ok().body(map);
        } catch (Exception e) {
            log.error("Error occured : " + e.getMessage());
            System.out.println("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Error occured = "+e.getMessage());
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDao userLoginDao){
        try {
            Map<String, Object> map = new HashMap<>();
            Optional<User> user = userRepository.findByEmail(userLoginDao.getEmail());
            if(!user.isPresent()){
                map.put("Message", "User not found. Please enter valid credentials");
                return ResponseEntity.badRequest().body(map);
            }
            if(!(passwordEncoder.matches(userLoginDao.getPassword(), user.get().getPassword()))){
                map.put("Message", "Invalid username or password");
                return ResponseEntity.badRequest().body(map);
            }
            String token = jwtUtil.generateToken(userLoginDao.getEmail(), "USER", userLoginDao.getEmail(),user.get().getId());
            map.put("token", token);
            map.put("message", "Login successfull");
            map.put("username",user.get().getUsername());
            return ResponseEntity.ok().body(map);
        } catch (Exception e) {
            log.error("Error occured : "+e.getMessage());
            return ResponseEntity.badRequest().body("Something went wrong : "+e.getMessage());
        }
    }
}
