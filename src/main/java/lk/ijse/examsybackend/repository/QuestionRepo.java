package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepo extends JpaRepository<QuestionOption,Integer> {
}
