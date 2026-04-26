package tn.esprit.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.esprit.userservice.dto.AdminCreateUserRequest;
import tn.esprit.userservice.dto.PublicUserDTO;
import tn.esprit.userservice.dto.UpdateProfileRequest;
import tn.esprit.userservice.dto.UserDTO;
import tn.esprit.userservice.entity.AccountStatus;
import tn.esprit.userservice.entity.Role;
import tn.esprit.userservice.entity.User;
import tn.esprit.userservice.exception.UserNotFoundException;
import tn.esprit.userservice.mapper.UserMapper;
import tn.esprit.userservice.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    private User findUserByCinOrThrow(Integer cin) {
        return userRepository.findByCin(cin)
                .orElseThrow(() -> new UserNotFoundException("User not found with cin: " + cin));
    }

    @Override
    public UserDTO getUserById(Integer cin) {
        User user = findUserByCinOrThrow(cin);
        return userMapper.toDTO(user);
    }

    @Override
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return userMapper.toDTO(user);
    }

    @Override
    public UserDTO updateProfile(Integer cin, UpdateProfileRequest request) {
        User user = findUserByCinOrThrow(cin);

        if (request.getFirstName() != null && !request.getFirstName().trim().isEmpty()) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().trim().isEmpty()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }

        if (request.getHeadline() != null) {
            user.setHeadline(request.getHeadline().trim());
        }

        if (request.getLocation() != null) {
            user.setLocation(request.getLocation().trim());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }

        if (request.getPhoto() != null && !request.getPhoto().trim().isEmpty()) {
            user.setPhoto(request.getPhoto().trim());
        }

        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        log.info("Profile updated for user: {}", updatedUser.getEmail());
        return userMapper.toDTO(updatedUser);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> searchUsers(String keyword) {
        return userRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(keyword, keyword)
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByRole(String role) {
        Role parsedRole = Role.valueOf(role.toUpperCase());

        return userRepository.findByRole(parsedRole)
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserDTO> getUsersByStatus(String status) {
        AccountStatus parsedStatus = AccountStatus.valueOf(status.toUpperCase());

        return userRepository.findByAccountStatus(parsedStatus)
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Integer cin) {
        User user = findUserByCinOrThrow(cin);

        user.setAccountStatus(AccountStatus.DELETED);
        user.setEnabled(false);
        user.setAccountNonLocked(false);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("User soft-deleted: {}", user.getEmail());
    }

    @Override
    public UserDTO createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        if (userRepository.existsByCin(request.getCin())) {
            throw new IllegalArgumentException("CIN already exists: " + request.getCin());
        }

        User user = new User();
        user.setCin(request.getCin());
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setEmail(request.getEmail().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhoneNumber());
        user.setPhoto(request.getPhoto());
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));

        userMapper.initNewUser(user);

        User savedUser = userRepository.save(user);

        log.info("Admin created new user: {}", savedUser.getEmail());
        return userMapper.toDTO(savedUser);
    }

    @Override
    public PublicUserDTO getPublicUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));

        return PublicUserDTO.builder()
                .id(user.getId())
                .cin(user.getCin())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .kycStatus(user.getKycStatus() != null ? user.getKycStatus().name() : null)
                .trustLevel(user.getTrustLevel())
                .accountStatus(user.getAccountStatus() != null ? user.getAccountStatus().name() : null)
                .build();
    }

    @Override
    public int getTrustLevel(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userId));
        return user.getTrustLevel();
    }
}