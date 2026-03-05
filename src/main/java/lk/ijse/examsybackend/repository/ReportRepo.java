package lk.ijse.examsybackend.repository;

import lk.ijse.examsybackend.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepo extends JpaRepository<Report, Integer> {
}
