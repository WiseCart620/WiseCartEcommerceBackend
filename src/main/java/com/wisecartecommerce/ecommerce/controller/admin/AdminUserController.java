package com.wisecartecommerce.ecommerce.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.UpdateUserRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Users", description = "User management APIs for administrators")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponse> users = userService.getAllUsers(pageable, role, enabled, search);

        return ResponseEntity.ok(ApiResponse.success("Users retrieved", users));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated", response));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {

        UserResponse response = userService.updateUserRole(id, role);
        return ResponseEntity.ok(ApiResponse.success("User role updated", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean enabled) {

        UserResponse response = userService.updateUserStatus(id, enabled);
        return ResponseEntity.ok(ApiResponse.success(
                enabled ? "User enabled" : "User disabled", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<ApiResponse<Object>> getUserStats() {
        Object stats = userService.getUserStats();
        return ResponseEntity.ok(ApiResponse.success("User statistics retrieved", stats));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getRecentUsers(
            @RequestParam(defaultValue = "10") int limit) {

        List<UserResponse> users = userService.getRecentUsers(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent users retrieved", users));
    }

    @PostMapping("/{id}/impersonate")
    @Operation(summary = "Generate impersonation token")
    public ResponseEntity<ApiResponse<String>> impersonateUser(@PathVariable Long id) {
        String token = userService.generateImpersonationToken(id);
        return ResponseEntity.ok(ApiResponse.success("Impersonation token generated", token));
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Reset user password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body) {

        String newPassword = (body != null) ? body.get("password") : null;
        userService.resetUserPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset initiated", null));
    }
}