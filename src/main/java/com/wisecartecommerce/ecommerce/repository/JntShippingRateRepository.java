package com.wisecartecommerce.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.wisecartecommerce.ecommerce.entity.JntShippingRate;

public interface JntShippingRateRepository extends JpaRepository<JntShippingRate, Long> {

    Page<JntShippingRate> findByDestinationProvinceContainingIgnoreCaseOrDestinationCityContainingIgnoreCase(
            String province, String city, Pageable pageable);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE r.active = true
        AND UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND r.minWeightKg <= :weight
        AND (r.maxWeightKg IS NULL OR r.maxWeightKg >= :weight)
        ORDER BY r.minWeightKg DESC
        """)
    List<JntShippingRate> findMatchingRates(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("weight") BigDecimal weight);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE r.active = true
        AND UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationBarangay) = UPPER(:barangay)
        AND r.minWeightKg <= :weight
        AND (r.maxWeightKg IS NULL OR r.maxWeightKg >= :weight)
        ORDER BY r.minWeightKg DESC
        """)
    List<JntShippingRate> findMatchingRatesWithBarangay(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("barangay") String barangay,
            @Param("weight") BigDecimal weight);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE r.active = true
        AND UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        ORDER BY r.minWeightKg DESC
        """)
    List<JntShippingRate> findAllForRoute(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity);

    @Query("SELECT DISTINCT r.originProvince FROM JntShippingRate r ORDER BY r.originProvince")
    List<String> findDistinctOriginProvinces();

    @Query("SELECT DISTINCT r.originCity FROM JntShippingRate r WHERE UPPER(r.originProvince) = UPPER(:province) ORDER BY r.originCity")
    List<String> findDistinctOriginCities(@Param("province") String province);

    @Query("SELECT DISTINCT r.destinationProvince FROM JntShippingRate r ORDER BY r.destinationProvince")
    List<String> findDistinctDestinationProvinces();

    @Query("SELECT DISTINCT r.destinationCity FROM JntShippingRate r WHERE UPPER(r.destinationProvince) = UPPER(:province) ORDER BY r.destinationCity")
    List<String> findDistinctDestinationCities(@Param("province") String province);
}