package com.wisecartecommerce.ecommerce.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Address;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUserId(Long userId);
    
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND " +
           "(:addressType IS NULL OR a.addressType = :addressType)")
    List<Address> findByUserIdAndAddressType(
            @Param("userId") Long userId,
            @Param("addressType") String addressType);
    
    boolean existsByUserIdAndIsDefaultTrue(Long userId);
}