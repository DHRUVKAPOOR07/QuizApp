package QuizApp.example.QuizApp.Utility;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtAuthenticationFilter(JwtUtil jwtUtil,UserDetailsService userDetailsService){
        this.jwtUtil=jwtUtil;
        this.userDetailsService=userDetailsService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,FilterChain chain)
        throws ServletException,IOException{
            final String authHeader = request.getHeader("Authorization");
            String username = null;
            String jwt = null; 
            if(authHeader!=null && authHeader.startsWith("Bearer ")){
                jwt = authHeader.substring(7);
                username = jwtUtil.extractUsername(jwt);
            }
            String ip = request.getRemoteAddr(); //to check for remote address
            
            if(username!=null&&SecurityContextHolder.getContext().getAuthentication()==null){
                var userDetails = userDetailsService.loadUserByUsername(username);
                if(jwtUtil.validateToken(jwt, userDetails)){
                    var authToken = new UsernamePasswordAuthenticationToken(userDetails, null,userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            chain.doFilter(request, response);
            
        }
    

}
