package lk.ijse.examsybackend.service;

import lk.ijse.examsybackend.dto.AdminProfileDTO;
import lk.ijse.examsybackend.dto.AdminProfileUpdateDTO;

public interface AdminProfileService {
    AdminProfileDTO getMyProfile(String username);
    AdminProfileDTO updateProfile(String username, AdminProfileUpdateDTO dto);


}
