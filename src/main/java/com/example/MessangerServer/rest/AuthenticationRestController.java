package com.example.MessangerServer.rest;

import com.example.MessangerServer.dto.MessageDto;
import com.example.MessangerServer.dto.RegisterDto;
import com.example.MessangerServer.dto.UserAuthDto;
import com.example.MessangerServer.model.Status;
import com.example.MessangerServer.security.jwt.JwtTokenProvider;
import com.example.MessangerServer.dto.AuthenticationRequestDto;
import com.example.MessangerServer.model.Employee;
import com.example.MessangerServer.security.jwt.JwtUser;
import com.example.MessangerServer.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping(value = "/api/auth/")
public class AuthenticationRestController {
    private final AuthenticationManager authenticationManager;

    private final JwtTokenProvider jwtTokenProvider;

    private final UserService userService;

    @Autowired
    public AuthenticationRestController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @PostMapping("login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthenticationRequestDto requestDto) {
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestDto.getUsername(), requestDto.getPassword()));
            JwtUser user = (JwtUser) auth.getPrincipal();
            final List<String> roles = user.getAuthorities().stream().map(
                    GrantedAuthority::getAuthority
            ).collect(Collectors.toList());
            String token = jwtTokenProvider.createToken(user.getUsername(), roles);
            return ResponseEntity.ok(new UserAuthDto(
                    token, user.getId(), user.getUsername(), user.getEmail(), user.getFirstname(),
                    user.getLastname(), roles
            ));
        } catch (AuthenticationServiceException exp) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
    @PostMapping("register")
    public ResponseEntity<?> register(@RequestBody RegisterDto registerDto) {
        try {
            ResponseEntity<?> responseEntity;
            if (!userService.existsByUsername(registerDto.getUsername())){
                responseEntity = ResponseEntity.badRequest().body(new MessageDto("Пользователь с таким именен уже зарегистрирован"));
            } else if (!userService.existsByEmail(registerDto.getEmail())){
                responseEntity = ResponseEntity.badRequest().body(new MessageDto("Пользователь с таким email уже зарегистрирован"));
            } else {
                userService.register(new Employee(
                        registerDto.getUsername(),
                        registerDto.getEmail(),
                        registerDto.getPassword(),
                        registerDto.getFirstName(),
                        registerDto.getLastName()
                ));
                responseEntity = ResponseEntity.ok(new MessageDto("Пользователь зарегистрирован"));
            }
            return responseEntity;
        } catch (AuthenticationServiceException exp) {
            throw new BadCredentialsException("Invalid username or password");
        }
    }
}
