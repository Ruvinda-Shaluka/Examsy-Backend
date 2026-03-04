package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepo extends JpaRepository<Student,Integer> {

    @Query("SELECT s FROM Student s WHERE s.userAccount.username = :username")
    Optional<Student> findByUserAccountUsername(@Param("username") String username);
}
