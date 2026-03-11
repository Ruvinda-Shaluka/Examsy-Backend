package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.TeacherDTO;

public interface TeacherProfileService {
    TeacherDTO getMyProfile(String username);

    TeacherDTO updateMyProfile(String username, TeacherDTO updateData);
}
