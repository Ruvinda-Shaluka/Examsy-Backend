package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.reqres.TeacherDTO;

public interface TeacherProfileService {
    TeacherDTO getMyProfile(String username);

    TeacherDTO updateMyProfile(String username, TeacherDTO updateData);
}
