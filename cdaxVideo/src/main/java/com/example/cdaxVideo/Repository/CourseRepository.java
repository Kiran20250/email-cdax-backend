package com.example.cdaxVideo.Repository;

import com.example.cdaxVideo.Entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // Fetch only courses + modules (not videos here)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules")
    List<Course> findAllWithModules();

    // Fetch single course + modules (not videos here)
    @Query("SELECT DISTINCT c FROM Course c LEFT JOIN FETCH c.modules WHERE c.id = :id")
    Optional<Course> findByIdWithModules(Long id);
}
