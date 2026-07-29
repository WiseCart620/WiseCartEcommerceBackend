package com.wisecartecommerce.ecommerce.repository;

import java.util.List;
import java.util.Optional;

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
        AND r.destinationBarangay IS NULL
        AND r.bagSize = :bagSize
        """)
    Optional<JntShippingRate> findByRouteAndBagSize(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("bagSize") String bagSize);

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
        AND r.bagSize = :bagSize
        """)
    Optional<JntShippingRate> findByRouteAndBagSizeWithBarangay(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("barangay") String barangay,
            @Param("bagSize") String bagSize);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND ((:destinationCity IS NULL AND r.destinationCity IS NULL) OR (:destinationCity IS NOT NULL AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))))
        AND ((:barangay IS NULL AND r.destinationBarangay IS NULL) OR UPPER(r.destinationBarangay) = UPPER(:barangay))
        AND r.bagSize = :bagSize
        """)
    Optional<JntShippingRate> findExactRoute(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("barangay") String barangay,
            @Param("bagSize") String bagSize);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND ((:destinationCity IS NULL AND r.destinationCity IS NULL) OR (:destinationCity IS NOT NULL AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:destinationCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))))
        AND ((:barangay IS NULL AND r.destinationBarangay IS NULL) OR UPPER(r.destinationBarangay) = UPPER(:barangay))
        """)
    List<JntShippingRate> findAllBagRatesForRoute(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("destinationCity") String destinationCity,
            @Param("barangay") String barangay);

    @Query("""
        SELECT r FROM JntShippingRate r
        WHERE r.active = true
        AND UPPER(r.originProvince) = UPPER(:originProvince)
        AND UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(r.originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
            = UPPER(TRIM(REPLACE(REPLACE(REPLACE(REPLACE(UPPER(:originCity), 'CITY OF ', ''), ' CITY', ''), '-', ' '), '  ', ' ')))
        AND UPPER(r.destinationProvince) = UPPER(:destinationProvince)
        AND r.destinationCity IS NULL
        AND r.destinationBarangay IS NULL
        AND r.bagSize = :bagSize
        """)
    Optional<JntShippingRate> findByRouteAndBagSizeProvinceWide(
            @Param("originProvince") String originProvince,
            @Param("originCity") String originCity,
            @Param("destinationProvince") String destinationProvince,
            @Param("bagSize") String bagSize);

    @Query("SELECT r FROM JntShippingRate r ORDER BY r.destinationProvince, r.destinationCity, r.destinationBarangay, r.bagSize")
    List<JntShippingRate> findAllForGrouping();

    @Query("SELECT DISTINCT r.originProvince FROM JntShippingRate r ORDER BY r.originProvince")
    List<String> findDistinctOriginProvinces();

    @Query("SELECT DISTINCT r.originCity FROM JntShippingRate r WHERE UPPER(r.originProvince) = UPPER(:province) ORDER BY r.originCity")
    List<String> findDistinctOriginCities(@Param("province") String province);

    @Query("SELECT DISTINCT r.destinationProvince FROM JntShippingRate r ORDER BY r.destinationProvince")
    List<String> findDistinctDestinationProvinces();

    @Query("SELECT DISTINCT r.destinationCity FROM JntShippingRate r WHERE UPPER(r.destinationProvince) = UPPER(:province) ORDER BY r.destinationCity")
    List<String> findDistinctDestinationCities(@Param("province") String province);
}
