package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.reqres.TeacherDTO;
import lk.ijse.examsybackend.entity.Teacher;
import lk.ijse.examsybackend.repository.TeacherRepo;
import lk.ijse.examsybackend.service.TeacherProfileService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherProfileServiceImpl implements TeacherProfileService {

    private final TeacherRepo teacherRepository;
    private final ModelMapper modelMapper;

    @Override
    public TeacherDTO getMyProfile(String username) {
        Teacher teacher = teacherRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher profile not found"));
        return modelMapper.map(teacher, TeacherDTO.class);
    }

    @Override
    public TeacherDTO updateMyProfile(String username, TeacherDTO updateData) {
        Teacher teacher = teacherRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Teacher profile not found"));

        // Match the exact field names from your new TeacherDTO
        if (updateData.getFullName() != null) teacher.setFullName(updateData.getFullName());
        if (updateData.getSpecialization() != null) teacher.setSpecialization(updateData.getSpecialization());
        if (updateData.getOfficeLocation() != null) teacher.setOfficeLocation(updateData.getOfficeLocation());
        if (updateData.getProfessionalBio() != null) teacher.setProfessionalBio(updateData.getProfessionalBio());
        if (updateData.getProfilePictureUrl() != null) teacher.setProfilePictureUrl(updateData.getProfilePictureUrl());

        if (updateData.getNotifyEmail() != null) teacher.setNotifyEmail(updateData.getNotifyEmail());
        if (updateData.getNotifyPush() != null) teacher.setNotifyPush(updateData.getNotifyPush());
        if (updateData.getNotifySecurity() != null) teacher.setNotifySecurity(updateData.getNotifySecurity());

        Teacher savedTeacher = teacherRepository.save(teacher);
        return modelMapper.map(savedTeacher, TeacherDTO.class);
    }
}