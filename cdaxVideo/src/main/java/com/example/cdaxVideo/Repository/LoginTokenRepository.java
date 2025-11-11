// package: com.example.flutter.Repository
package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.LoginToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LoginTokenRepository extends JpaRepository<LoginToken, Long> {
    Optional<LoginToken> findByToken(String token);
    Optional<LoginToken> findTopByUserIdOrderByIdDesc(Long userId);
    List<LoginToken> findByUserId(Long userId);
}
