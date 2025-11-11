package com.example.cdaxVideo.Service;


import com.example.cdaxVideo.Entity.LoginToken;
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Repository.LoginTokenRepository;
import com.example.cdaxVideo.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OTPService otpService;

    @Autowired
    private LoginTokenRepository loginTokenRepository;

    // ----------------- ORIGINAL METHODS (kept exactly) -----------------
    public String registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "Email already exists";
        }
        if (userRepository.existsByMobile(user.getMobile())) {
            return "Mobile number already registered";
        }
        if (!user.getPassword().equals(user.getCpassword())) {
            return "Passwords do not match";
        }
        userRepository.save(user);
        return "User registered successfully";
    }

    public String loginUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            if (existingUser.get().getPassword().equals(user.getPassword())) {
                return "Login successful";
            } else {
                return "Incorrect password";
            }
        }
        return "Email not found";
    }

    public String getFirstNameByEmail(String email) {
        return userRepository.findByEmail(email).map(User::getFirstName).orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public boolean toggleSubscription(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setSubscribed(!user.isSubscribed());
            userRepository.save(user);
            return true;
        }
        return false;
    }

    // ----------------- NEW METHODS FOR OTP / 2FA / LOGIN-TOKEN -----------------
    private final Map<String, String> pendingLoginTokens = new HashMap<>();

    public String generateAndSendEmailOtp(String email) {
        return otpService.generateAndSendEmailOtp(email);
    }

    public boolean verifyEmailOtp(String email, String otp) {
        boolean ok = otpService.verifyOtp(email, "EMAIL", otp);
        if (ok) {
            userRepository.findByEmail(email).ifPresent(u -> {
                u.setEmailVerified(true);
                userRepository.save(u);
            });
        }
        return ok;
    }

    public void savePendingLoginToken(String email, String token) {
        pendingLoginTokens.put(email, token);
        try {
            User u = userRepository.findByEmail(email).orElse(null);
            if (u != null) {
                LoginToken lt = new LoginToken();
                lt.setUserId(u.getId());
                lt.setToken(token);
                lt.setExpiresAt(LocalDateTime.now().plusMinutes(15));
                lt.setVerified(false);
                loginTokenRepository.save(lt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getEmailByPendingToken(String token) {
        return pendingLoginTokens.entrySet().stream()
                .filter(e -> e.getValue().equals(token))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void clearPendingToken(String email) {
        pendingLoginTokens.remove(email);
    }

    public Optional<LoginToken> findLatestLoginTokenForUser(Long userId) {
        return loginTokenRepository.findTopByUserIdOrderByIdDesc(userId);
    }

    public void markLoginTokenVerified(String token, String jwt) {
        try {
            loginTokenRepository.findByToken(token).ifPresent(lt -> {
                lt.setVerified(true);
                lt.setJwt(jwt);
                loginTokenRepository.save(lt);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ NEW METHOD ADDED - Check if email already exists (used by AuthController)
    public boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
