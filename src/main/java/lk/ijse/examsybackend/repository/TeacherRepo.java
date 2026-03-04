package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TeacherRepo extends JpaRepository<Teacher, Integer> {
    @Query("SELECT t FROM Teacher t WHERE t.userAccount.username = :username")
    Optional<Teacher> findByUserAccountUsername(@Param("username") String username);
}