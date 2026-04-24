package tn.esprit.userservice.service;

import tn.esprit.userservice.dto.AdminCreateUserRequest;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.UpdateProfileRequest;
import tn.esprit.userservice.dto.UserDTO;

import java.util.List;

public interface IUserService {

    UserDTO getUserById(Integer cin);

    UserDTO getUserByEmail(String email);

    UserDTO updateProfile(Integer cin, UpdateProfileRequest request);

    List<UserDTO> getAllUsers();

    List<UserDTO> searchUsers(String keyword);

    List<UserDTO> getUsersByRole(String role);

    List<UserDTO> getUsersByStatus(String status);

    void deleteUser(Integer cin);

    UserDTO createUserByAdmin(AdminCreateUserRequest request);

    // ==================== INTER-MODULES ====================

    // Retourne le DTO public minimal pour les autres microservices
    PublicUserDTO getPublicUser(Long userId);

    // Retourne uniquement le Trust Level — consommé par Module 05
    int getTrustLevel(Long userId);
}