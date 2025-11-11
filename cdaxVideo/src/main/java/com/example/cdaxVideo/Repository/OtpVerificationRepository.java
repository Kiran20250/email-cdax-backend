package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    Optional<OtpVerification> findTopByTargetAndTypeOrderByIdDesc(String target, String type);
    void deleteAllByTargetAndType(String target, String type);

}
