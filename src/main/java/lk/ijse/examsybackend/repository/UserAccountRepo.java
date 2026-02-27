package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepo extends JpaRepository<UserAccount,Integer> {
}
