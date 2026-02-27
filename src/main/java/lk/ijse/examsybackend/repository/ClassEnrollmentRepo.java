package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.ClassEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassEnrollmentRepo extends JpaRepository<ClassEnrollment,Integer> {
}
