package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ClassAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassAnnouncementRepo extends JpaRepository<ClassAnnouncement,Integer> {
}
