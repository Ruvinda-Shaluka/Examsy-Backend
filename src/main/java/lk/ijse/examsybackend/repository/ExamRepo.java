package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRepo extends JpaRepository<Exam,Integer> {
    List<Exam> findAllByStatus(String published);

    // Change it from OrderByCreatedAtDesc to OrderByIdDesc
    List<Exam> findByCourseIdOrderByIdDesc(Integer classId);
}
