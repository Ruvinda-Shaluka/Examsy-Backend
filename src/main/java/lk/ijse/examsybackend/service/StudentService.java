package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.StudentDTO;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.repository.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepo studentRepository;
    private final ModelMapper modelMapper; // Inject ModelMapper here!

    public StudentDTO getMyProfile(String username) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        return modelMapper.map(student, StudentDTO.class);
    }

    public StudentDTO updateMyProfile(String username, StudentDTO updateData) {
        Student student = studentRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Student profile not found"));

        if (updateData.getMajor() != null) student.setMajor(updateData.getMajor());
        if (updateData.getAcademicBio() != null) student.setAcademicBio(updateData.getAcademicBio());
        if (updateData.getProfilePictureUrl() != null) student.setProfilePictureUrl(updateData.getProfilePictureUrl());

        Student savedStudent = studentRepository.save(student);

        return modelMapper.map(savedStudent, StudentDTO.class);
    }
}