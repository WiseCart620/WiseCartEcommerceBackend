package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.wisecartecommerce.ecommerce.Dto.Request.JntEstimateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.JntShippingRateRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.JntEstimateResponse;
import com.wisecartecommerce.ecommerce.entity.JntShippingRate;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.JntShippingRateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JntShippingService {

    private final JntShippingRateRepository rateRepository;

    public Page<JntShippingRate> getAllRates(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return rateRepository.findAll(pageable);
        }
        return rateRepository.findByDestinationProvinceContainingIgnoreCaseOrDestinationCityContainingIgnoreCase(
                search, search, pageable);
    }

    public JntShippingRate getRateById(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("J&T shipping rate not found: " + id));
    }

    public JntShippingRate createRate(JntShippingRateRequest req) {
        JntShippingRate rate = new JntShippingRate();
        applyRequest(rate, req);
        return rateRepository.save(rate);
    }

    public JntShippingRate updateRate(Long id, JntShippingRateRequest req) {
        JntShippingRate rate = getRateById(id);
        applyRequest(rate, req);
        return rateRepository.save(rate);
    }

    public void deleteRate(Long id) {
        rateRepository.delete(getRateById(id));
    }

    public void toggleActive(Long id, boolean active) {
        JntShippingRate rate = getRateById(id);
        rate.setActive(active);
        rateRepository.save(rate);
    }

    private void applyRequest(JntShippingRate rate, JntShippingRateRequest req) {
        rate.setOriginProvince(req.getOriginProvince().trim().toUpperCase());
        rate.setOriginCity(req.getOriginCity().trim().toUpperCase());
        rate.setDestinationProvince(req.getDestinationProvince().trim().toUpperCase());
        rate.setDestinationCity(req.getDestinationCity().trim().toUpperCase());
        rate.setDestinationBarangay(
                req.getDestinationBarangay() != null && !req.getDestinationBarangay().isBlank()
                ? req.getDestinationBarangay().trim().toUpperCase()
                : null);
        rate.setServiceType(req.getServiceType());
        rate.setBagSize(req.getBagSize());
        rate.setMinWeightKg(req.getMinWeightKg());
        rate.setMaxWeightKg(req.getMaxWeightKg());
        rate.setShippingFee(req.getShippingFee());
        rate.setItemAdditionalFee(req.getItemAdditionalFee() != null ? req.getItemAdditionalFee() : BigDecimal.ZERO);
        rate.setAdditionalFeePerKgOverMax(req.getAdditionalFeePerKgOverMax() != null ? req.getAdditionalFeePerKgOverMax() : BigDecimal.ZERO);
        rate.setActive(req.getActive() != null ? req.getActive() : true);
    }

    public List<String> getOriginProvinces() {
        return rateRepository.findDistinctOriginProvinces();
    }

    public List<String> getOriginCities(String province) {
        return rateRepository.findDistinctOriginCities(province);
    }

    public List<String> getDestinationProvinces() {
        return rateRepository.findDistinctDestinationProvinces();
    }

    public List<String> getDestinationCities(String province) {
        return rateRepository.findDistinctDestinationCities(province);
    }

    public JntEstimateResponse estimate(JntEstimateRequest req) {
        BigDecimal weight = req.getWeightKg() != null ? req.getWeightKg() : BigDecimal.ONE;
        String originProvince = req.getOriginProvince() != null ? req.getOriginProvince().trim().toUpperCase() : null;
        String originCity = req.getOriginCity() != null ? req.getOriginCity().trim().toUpperCase() : null;
        String destProvince = req.getDestinationProvince() != null ? req.getDestinationProvince().trim().toUpperCase() : null;
        String destCity = req.getDestinationCity() != null ? req.getDestinationCity().trim().toUpperCase() : null;
        String barangay = req.getDestinationBarangay() != null && !req.getDestinationBarangay().isBlank()
                ? req.getDestinationBarangay().trim().toUpperCase()
                : null;

        List<JntShippingRate> matches = barangay != null
                ? rateRepository.findMatchingRatesWithBarangay(
                        originProvince, originCity,
                        destProvince, destCity, barangay, weight)
                : List.of();

        if (matches.isEmpty()) {
            matches = rateRepository.findMatchingRates(
                    originProvince, originCity,
                    destProvince, destCity, weight);
        }

        JntShippingRate rate;
        BigDecimal extraFee = BigDecimal.ZERO;

        if (!matches.isEmpty()) {
            rate = matches.get(0);
        } else {
            List<JntShippingRate> allForRoute = rateRepository.findAllForRoute(
                    originProvince, originCity,
                    destProvince, destCity);

            if (allForRoute.isEmpty()) {
                throw new ResourceNotFoundException(
                        "No J&T shipping rate configured for this route yet.");
            }

            rate = allForRoute.get(0); // highest bracket for this route
            if (rate.getMaxWeightKg() != null && weight.compareTo(rate.getMaxWeightKg()) > 0) {
                BigDecimal overageKg = weight.subtract(rate.getMaxWeightKg());
                extraFee = overageKg.multiply(
                        rate.getAdditionalFeePerKgOverMax() != null ? rate.getAdditionalFeePerKgOverMax() : BigDecimal.ZERO
                ).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal shippingFee = rate.getShippingFee().add(extraFee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemFee = rate.getItemAdditionalFee() != null ? rate.getItemAdditionalFee() : BigDecimal.ZERO;
        BigDecimal total = shippingFee.add(itemFee).setScale(2, RoundingMode.HALF_UP);

        return new JntEstimateResponse(
                rate.getOriginProvince(), rate.getOriginCity(),
                rate.getDestinationProvince(), rate.getDestinationCity(),
                rate.getDestinationBarangay(),
                rate.getServiceType(), rate.getBagSize(),
                weight, shippingFee, itemFee, total
        );
    }
}
