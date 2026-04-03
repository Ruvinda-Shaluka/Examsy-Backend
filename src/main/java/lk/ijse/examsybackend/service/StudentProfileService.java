package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.reqres.StudentDTO;

public interface StudentProfileService {
    StudentDTO getMyProfile(String username);

    StudentDTO updateMyProfile(String username, StudentDTO updateData);
}
