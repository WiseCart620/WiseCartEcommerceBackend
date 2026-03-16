package com.wisecartecommerce.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ChangePasswordRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.UpdateProfileRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.UpdateUserRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse getCurrentUserProfile();
    UserResponse updateProfile(UpdateProfileRequest request);
    void changePassword(ChangePasswordRequest request);
    UserResponse uploadAvatar(MultipartFile file);
    UserResponse removeAvatar();
    Object getUserAddresses();
    Object addAddress(Object addressRequest);
    Object updateAddress(Long addressId, Object addressRequest);
    void deleteAddress(Long addressId);
    Object setDefaultAddress(Long addressId);
    Object getOrdersSummary();
    Object getWishlist();
    void addToWishlist(Long productId);
    void removeFromWishlist(Long productId);
    
    // Admin methods
    Page<UserResponse> getAllUsers(Pageable pageable, String role, Boolean enabled, String search);
    UserResponse getUserById(Long id);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    UserResponse updateUserRole(Long id, String role);
    UserResponse updateUserStatus(Long id, boolean enabled);
    void deleteUser(Long id);
    Object getUserStats();
    List<UserResponse> getRecentUsers(int limit);
    String generateImpersonationToken(Long id);
    void resetUserPassword(Long id, String newPassword);
}