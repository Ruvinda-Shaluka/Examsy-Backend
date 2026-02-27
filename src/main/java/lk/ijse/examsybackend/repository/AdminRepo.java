package lk.ijse.examsybackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import lk.ijse.examsybackend.entity.Admin;

public interface AdminRepo extends JpaRepository<Admin,Integer> {
}
