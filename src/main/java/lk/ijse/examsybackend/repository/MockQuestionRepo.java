package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.MockQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MockQuestionRepo extends JpaRepository<MockQuestion,Integer> {
}
