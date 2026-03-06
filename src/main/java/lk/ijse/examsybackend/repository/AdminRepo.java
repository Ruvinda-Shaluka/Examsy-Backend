package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepo extends JpaRepository<Admin, Integer> {
    Optional<Admin> findByUserAccountUsername(String username);
}