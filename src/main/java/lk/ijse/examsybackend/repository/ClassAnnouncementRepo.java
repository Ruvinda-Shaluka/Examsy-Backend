package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ClassAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassAnnouncementRepo extends JpaRepository<ClassAnnouncement,Integer> {
    // Fetches all announcements for a class, newest first!
    List<ClassAnnouncement> findByCourseIdOrderByCreatedAtDesc(Integer courseId);
}
