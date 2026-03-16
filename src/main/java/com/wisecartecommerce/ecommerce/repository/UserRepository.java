package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.util.Role;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByResetToken(String token);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    boolean existsByPhone(String phone);

    @Query("SELECT u FROM User u WHERE " +
            "(:role IS NULL OR u.role = :role) AND " +
            "(:enabled IS NULL OR u.enabled = :enabled) AND " +
            "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findUsersWithFilters(
            @Param("role") String role,
            @Param("enabled") Boolean enabled,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= CURRENT_DATE")
    Long countTodayRegistrations();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'CUSTOMER'")
    Long countCustomers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
    Long countAdmins();
}