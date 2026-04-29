package com.wisecartecommerce.ecommerce.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.AddressRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ChangePasswordRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.UpdateProfileRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.UpdateUserRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.AddressResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.config.JwtService;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AddressRepository;
import com.wisecartecommerce.ecommerce.repository.PendingCheckoutRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.FirebaseAdminService;
import com.wisecartecommerce.ecommerce.service.UserService;
import com.wisecartecommerce.ecommerce.util.Role;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;
    private final JwtService jwtService;
    private final PendingCheckoutRepository pendingCheckoutRepository;
    private final FirebaseAdminService firebaseAdminService;
    private final CouponUsageRepository couponUsageRepository;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile() {
        User user = getCurrentUser();
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomException("Email already registered");
            }
            user.setEmail(request.getEmail());
            user.setEmailVerified(false);
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new CustomException("Phone number already registered");
            }
            user.setPhone(request.getPhone());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new CustomException("Current password is incorrect");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new CustomException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(MultipartFile file) {
        User user = getCurrentUser();
        try {
            if (user.getAvatarUrl() != null) {
                fileStorageService.deleteFile(user.getAvatarUrl());
            }
            String avatarUrl = fileStorageService.uploadUserImage(file, user.getId());
            user.setAvatarUrl(avatarUrl);
            User updatedUser = userRepository.save(user);
            log.info("Avatar uploaded for user: {}", user.getEmail());
            return mapToUserResponse(updatedUser);
        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            throw new CustomException("Failed to upload avatar: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse removeAvatar() {
        User user = getCurrentUser();
        if (user.getAvatarUrl() != null) {
            try {
                fileStorageService.deleteFile(user.getAvatarUrl());
            } catch (Exception e) {
                log.error("Failed to delete avatar file", e);
            }
            user.setAvatarUrl(null);
            User updatedUser = userRepository.save(user);
            log.info("Avatar removed for user: {}", user.getEmail());
            return mapToUserResponse(updatedUser);
        }
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserAddresses() {
        User user = getCurrentUser();
        return addressRepository.findByUserId(user.getId()).stream()
                .filter(a -> !a.isDeleted())
                .map(this::mapAddressToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Object addAddress(Object addressRequest) {
        User user = getCurrentUser();
        AddressRequest request = (AddressRequest) addressRequest;

        Address address = Address.builder()
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .addressType(request.getAddressType())
                .companyName(request.getCompanyName())
                .build();

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());
        boolean hasNoAddresses = addressRepository.findByUserId(user.getId())
                .stream().noneMatch(a -> !a.isDeleted());
        if (isDefault || hasNoAddresses) {
            address.setDefault(true);
            addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(addr -> addr.setDefault(false));
        }

        Address savedAddress = addressRepository.save(address);
        log.info("Address added for user: {}", user.getEmail());
        return mapAddressToResponse(savedAddress);
    }

    @Override
    @Transactional
    public Object updateAddress(Long addressId, Object addressRequest) {
        User user = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only update your own addresses");
        }

        AddressRequest request = (AddressRequest) addressRequest;
        if (request.getFirstName() != null) {
            address.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            address.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            address.setPhone(request.getPhone());
        }
        if (request.getAddressLine1() != null) {
            address.setAddressLine1(request.getAddressLine1());
        }
        if (request.getAddressLine2() != null) {
            address.setAddressLine2(request.getAddressLine2());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getState() != null) {
            address.setState(request.getState());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }
        if (request.getCompanyName() != null) {
            address.setCompanyName(request.getCompanyName());
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            address.setDefault(true);
            addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(addr -> {
                        if (!addr.getId().equals(addressId)) {
                            addr.setDefault(false);
                            addressRepository.save(addr);
                        }
                    });
        }

        Address updatedAddress = addressRepository.save(address);
        log.info("Address updated for user: {}", user.getEmail());
        return mapAddressToResponse(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long addressId) {
        User user = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only delete your own addresses");
        }

        if (address.isDefault()) {
            long activeCount = addressRepository.findByUserId(user.getId())
                    .stream().filter(a -> !a.isDeleted()).count();
            if (activeCount > 1) {
                throw new CustomException(
                        "Cannot delete default address. Set another address as default first.");
            }
        }

        address.setDeleted(true);
        addressRepository.save(address);
        log.info("Address soft-deleted for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public Object setDefaultAddress(Long addressId) {
        User user = getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only set your own addresses as default");
        }

        addressRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(addr -> {
                    addr.setDefault(false);
                    addressRepository.save(addr);
                });

        address.setDefault(true);
        Address updatedAddress = addressRepository.save(address);
        log.info("Default address set for user: {}", user.getEmail());
        return mapAddressToResponse(updatedAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public Object getOrdersSummary() {
        User user = getCurrentUser();
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalOrders", user.getOrders().size());
        summary.put("totalSpent", user.getOrders().stream()
                .map(order -> order.getFinalAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        summary.put("pendingOrders", user.getOrders().stream()
                .filter(order -> order.getStatus().name().equals("PENDING")).count());
        return summary;
    }

    @Override
    @Transactional(readOnly = true)
    public Object getWishlist() {
        User user = getCurrentUser();
        return user.getWishlist().stream()
                .map(product -> {
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("id", product.getId());
                    productMap.put("name", product.getName());
                    productMap.put("price", product.getPrice());
                    productMap.put("imageUrl", product.getImageUrl());
                    productMap.put("inStock", product.isInStock());
                    return productMap;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addToWishlist(Long productId) {
        log.info("Adding product {} to wishlist", productId);
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long productId) {
        log.info("Removing product {} from wishlist", productId);
    }

    // ─── Admin Methods ───────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, String role,
            Boolean enabled, String search) {
        Page<User> users = userRepository.findUsersWithFilters(role, enabled, search, pageable);
        return users.map(this::mapToUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new CustomException("Email already registered");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new CustomException("Phone number already registered");
            }
            user.setPhone(request.getPhone());
        }
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated by admin: {}", user.getEmail());
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        try {
            Role newRole = Role.valueOf(role.toUpperCase());
            user.setRole(newRole);
            User updatedUser = userRepository.save(user);
            log.info("User role updated: {} to {}", user.getEmail(), newRole);
            return mapToUserResponse(updatedUser);
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid role: " + role);
        }
    }

    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setEnabled(enabled);
        User updatedUser = userRepository.save(user);

        // Sync disable/enable with Firebase
        if (enabled) {
            firebaseAdminService.enableUser(user.getFirebaseUid());
        } else {
            firebaseAdminService.disableUser(user.getFirebaseUid());
        }

        log.info("User status updated: {} to {}", user.getEmail(), enabled ? "enabled" : "disabled");
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User currentUser = getCurrentUser();
        if (currentUser.getId().equals(id)) {
            throw new CustomException("You cannot delete your own account");
        }

        if (user.getAvatarUrl() != null) {
            try {
                if (!user.getAvatarUrl().startsWith("http://")
                        && !user.getAvatarUrl().startsWith("https://")) {
                    fileStorageService.deleteFile(user.getAvatarUrl());
                } else {
                    log.debug("Skipping avatar delete — external URL: {}", user.getAvatarUrl());
                }
            } catch (Exception e) {
                log.error("Failed to delete avatar file for user {}", id, e);
            }
        }

        // Sync deletion with Firebase (silent fail if no firebaseUid)
        firebaseAdminService.deleteUser(user.getFirebaseUid());

        List<Long> orderIds = user.getOrders().stream()
                .map(order -> order.getId())
                .collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            couponUsageRepository.deleteByOrderIdIn(orderIds);
        }
        couponUsageRepository.deleteByUserId(id);

        pendingCheckoutRepository.deleteByUserId(id);
        userRepository.delete(user);

        log.info("User deleted by admin: {}", user.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalCustomers", userRepository.countCustomers());
        stats.put("totalAdmins", userRepository.countAdmins());
        stats.put("todayRegistrations", userRepository.countTodayRegistrations());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getRecentUsers(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        return users.stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String generateImpersonationToken(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        User currentUser = getCurrentUser();
        log.info("Admin {} impersonating user {}", currentUser.getEmail(), user.getEmail());
        return jwtService.generateToken(user);
    }

    @Override
    public void resetUserPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String passwordToSet = (newPassword != null && !newPassword.isBlank())
                ? newPassword
                : UUID.randomUUID().toString().substring(0, 8);
        user.setPassword(passwordEncoder.encode(passwordToSet));
        userRepository.save(user);
        log.info("Password reset for user: {}", user.getEmail());
    }

    // ─── Mappers ─────────────────────────────────────────────────────────────
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .emailVerified(user.isEmailVerified())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private AddressResponse mapAddressToResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .addressType(address.getAddressType())
                .companyName(address.getCompanyName())
                .isDefault(address.isDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}
