package lk.ijse.examsybackend.service.impl;

import lk.ijse.examsybackend.dto.AdminProfileDTO;
import lk.ijse.examsybackend.dto.AdminProfileUpdateDTO;
import lk.ijse.examsybackend.entity.Admin;
import lk.ijse.examsybackend.repository.AdminRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProfileServiceImpl {

    private final AdminRepo adminRepository;

    @Transactional(readOnly = true)
    public AdminProfileDTO getMyProfile(String username) {
        Admin admin = adminRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin profile not found."));

        return AdminProfileDTO.builder()
                .id(admin.getId())
                .fullName(admin.getFullName())
                .profilePictureUrl(admin.getProfilePictureUrl())
                .roleLevel(admin.getRoleLevel())
                .build();
    }

    @Transactional
    public AdminProfileDTO updateProfile(String username, AdminProfileUpdateDTO dto) {
        Admin admin = adminRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new RuntimeException("Admin profile not found."));

        admin.setFullName(dto.getFullName());

        if (dto.getProfilePictureUrl() != null) {
            admin.setProfilePictureUrl(dto.getProfilePictureUrl());
        }

        Admin savedAdmin = adminRepository.save(admin);

        return AdminProfileDTO.builder()
                .id(savedAdmin.getId())
                .fullName(savedAdmin.getFullName())
                .profilePictureUrl(savedAdmin.getProfilePictureUrl())
                .roleLevel(savedAdmin.getRoleLevel())
                .build();
    }
}