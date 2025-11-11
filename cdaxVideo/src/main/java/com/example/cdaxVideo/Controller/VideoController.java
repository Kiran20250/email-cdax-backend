package com.example.cdaxVideo.Controller;

import com.example.cdaxVideo.Entity.Course;
import com.example.cdaxVideo.Entity.Module;
import com.example.cdaxVideo.Entity.Video;
import com.example.cdaxVideo.Service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class VideoController {

    @Autowired
    private VideoService videoService;

    // ---------------------- COURSE APIs ----------------------

    @PostMapping("/courses")
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course saved = videoService.saveCourse(course);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/courses")
    public ResponseEntity<Map<String, Object>> getCourses() {
        List<Course> list = videoService.getAllCoursesWithModulesAndVideos();
        Map<String, Object> response = new HashMap<>();
        response.put("data", list);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<Map<String, Object>> getCourse(@PathVariable Long id) {
        return videoService.getCourseByIdWithModulesAndVideos(id)
                .map(course -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("data", course);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------- MODULE APIs ----------------------

    @PostMapping("/modules")
    public ResponseEntity<?> addModule(@RequestParam("courseId") Long courseId,
                                       @RequestBody Module module) {
        try {
            Module saved = videoService.saveModule(courseId, module);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/modules/course/{courseId}")
    public ResponseEntity<List<Module>> getModulesByCourse(@PathVariable Long courseId) {
        List<Module> list = videoService.getModulesByCourseId(courseId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/modules/{id}")
    public ResponseEntity<?> getModule(@PathVariable Long id) {
        return videoService.getModuleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------------------- VIDEO APIs ----------------------

    @PostMapping("/videos")
    public ResponseEntity<?> addVideo(@RequestParam("moduleId") Long moduleId,
                                      @RequestBody Video video) {
        try {
            Video saved = videoService.saveVideo(moduleId, video);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/modules/{moduleId}/videos")
    public ResponseEntity<List<Video>> getVideosByModule(@PathVariable Long moduleId) {
        List<Video> list = videoService.getVideosByModuleId(moduleId);
        return ResponseEntity.ok(list);
    }
}
