package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.StudentDTO;
import lk.ijse.examsybackend.entity.Student;
import lk.ijse.examsybackend.repository.StudentRepo;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentProfileServiceImpl {

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

        // Progressive Profiling: Update fields if they are provided in the request
        if (updateData.getFullName() != null) student.setFullName(updateData.getFullName());
        if (updateData.getMajor() != null) student.setMajor(updateData.getMajor());
        if (updateData.getAcademicBio() != null) student.setAcademicBio(updateData.getAcademicBio());
        if (updateData.getProfilePictureUrl() != null) student.setProfilePictureUrl(updateData.getProfilePictureUrl());

        // Notification Preferences
        if (updateData.getNotifyEmail() != null) student.setNotifyEmail(updateData.getNotifyEmail());
        if (updateData.getNotifyPush() != null) student.setNotifyPush(updateData.getNotifyPush());
        if (updateData.getNotifyIdentity() != null) student.setNotifyIdentity(updateData.getNotifyIdentity());

        Student savedStudent = studentRepository.save(student);
        return modelMapper.map(savedStudent, StudentDTO.class);
    }
}