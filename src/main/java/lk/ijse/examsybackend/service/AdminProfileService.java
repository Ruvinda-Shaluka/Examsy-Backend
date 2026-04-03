package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.reqres.AdminProfileDTO;
import lk.ijse.examsybackend.dto.AdminProfileUpdateDTO;
import org.springframework.transaction.annotation.Transactional;

public interface AdminProfileService {


    @Transactional(readOnly = true)
    AdminProfileDTO getMyProfile(String username);

    @Transactional
    AdminProfileDTO updateProfile(String username, AdminProfileUpdateDTO dto);
}
