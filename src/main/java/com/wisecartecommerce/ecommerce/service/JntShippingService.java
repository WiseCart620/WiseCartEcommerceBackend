package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.wisecartecommerce.ecommerce.Dto.Request.JntEstimateRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.JntEstimateResponse;
import com.wisecartecommerce.ecommerce.entity.AppSettings;
import com.wisecartecommerce.ecommerce.entity.JntShippingRate;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AppSettingsRepository;
import com.wisecartecommerce.ecommerce.repository.JntShippingRateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JntShippingService {

    private final JntShippingRateRepository rateRepository;
    private final AppSettingsRepository appSettingsRepository;

    private AppSettings settings() {
        return appSettingsRepository.findAll().stream().findFirst().orElse(null);
    }

    private BigDecimal orDefault(BigDecimal value, BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    public Page<com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary> getGroupedRoutes(String search, Pageable pageable) {
        List<JntShippingRate> all = rateRepository.findAllForGrouping();

        java.util.Map<String, List<JntShippingRate>> grouped = all.stream()
                .collect(Collectors.groupingBy(this::routeKey, java.util.LinkedHashMap::new, Collectors.toList()));

        List<com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary> summaries = grouped.values().stream()
                .map(this::toSummary)
                .filter(s -> search == null || search.isBlank()
                || s.getDestinationProvince().toLowerCase().contains(search.toLowerCase())
                || (s.getDestinationCity() != null && s.getDestinationCity().toLowerCase().contains(search.toLowerCase())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), summaries.size());
        List<com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary> pageContent
                = start >= summaries.size() ? List.of() : summaries.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, summaries.size());
    }

    private String routeKey(JntShippingRate r) {
        return r.getOriginProvince() + "|" + r.getOriginCity() + "|"
                + r.getDestinationProvince() + "|" + r.getDestinationCity() + "|"
                + (r.getDestinationBarangay() == null ? "" : r.getDestinationBarangay());
    }

    private com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary toSummary(List<JntShippingRate> rows) {
        JntShippingRate any = rows.get(0);
        com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary s
                = new com.wisecartecommerce.ecommerce.Dto.Response.JntRouteRateSummary();
        s.setOriginProvince(any.getOriginProvince());
        s.setOriginCity(any.getOriginCity());
        s.setDestinationProvince(any.getDestinationProvince());
        s.setDestinationCity(any.getDestinationCity());
        s.setDestinationBarangay(any.getDestinationBarangay());
        s.setServiceType(any.getServiceType());
        s.setOverweightAdditionalFee(any.getOverweightAdditionalFee());
        s.setActive(rows.stream().allMatch(JntShippingRate::getActive));
        for (JntShippingRate r : rows) {
            if (SMALL.equals(r.getBagSize())) {
                s.setSmallId(r.getId());
                s.setSmallFee(r.getShippingFee());
                s.setSmallItemFee(r.getItemAdditionalFee());
            } else if (MEDIUM.equals(r.getBagSize())) {
                s.setMediumId(r.getId());
                s.setMediumFee(r.getShippingFee());
                s.setMediumItemFee(r.getItemAdditionalFee());
            } else if (BIG.equals(r.getBagSize())) {
                s.setBigId(r.getId());
                s.setBigFee(r.getShippingFee());
                s.setBigItemFee(r.getItemAdditionalFee());
            }
        }
        return s;
    }

    @org.springframework.transaction.annotation.Transactional
    public void toggleRouteActive(Long smallId, Long mediumId, Long bigId, boolean active) {
        for (Long id : new Long[]{smallId, mediumId, bigId}) {
            if (id == null) {
                continue;
            }
            JntShippingRate r = getRateById(id);
            r.setActive(active);
            rateRepository.save(r);
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteRoute(Long smallId, Long mediumId, Long bigId) {
        for (Long id : new Long[]{smallId, mediumId, bigId}) {
            if (id == null) {
                continue;
            }
            rateRepository.deleteById(id);
        }
    }

    public JntShippingRate getRateById(Long id) {
        return rateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("J&T shipping rate not found: " + id));
    }

    @org.springframework.transaction.annotation.Transactional
    public JntShippingRate createRate(com.wisecartecommerce.ecommerce.Dto.Request.JntShippingRateRequest req) {
        JntShippingRate rate = new JntShippingRate();
        applyRequestToEntity(rate, req);
        return rateRepository.save(rate);
    }

    @org.springframework.transaction.annotation.Transactional
    public JntShippingRate updateRate(Long id, com.wisecartecommerce.ecommerce.Dto.Request.JntShippingRateRequest req) {
        JntShippingRate rate = getRateById(id);
        applyRequestToEntity(rate, req);
        return rateRepository.save(rate);
    }

    private void applyRequestToEntity(JntShippingRate rate, com.wisecartecommerce.ecommerce.Dto.Request.JntShippingRateRequest req) {
        rate.setOriginProvince(norm(req.getOriginProvince()));
        rate.setOriginCity(norm(req.getOriginCity()));
        rate.setDestinationProvince(norm(req.getDestinationProvince()));
        rate.setDestinationCity(req.getDestinationCity() != null && !req.getDestinationCity().isBlank()
                ? norm(req.getDestinationCity()) : null);
        rate.setDestinationBarangay(req.getDestinationBarangay() != null && !req.getDestinationBarangay().isBlank()
                ? norm(req.getDestinationBarangay()) : null);
        rate.setServiceType(req.getServiceType() != null ? req.getServiceType() : "EZ");
        rate.setBagSize(validateBagSize(req.getBagSize()));
        rate.setShippingFee(req.getShippingFee() != null ? req.getShippingFee() : BigDecimal.ZERO);
        rate.setItemAdditionalFee(req.getItemAdditionalFee() != null ? req.getItemAdditionalFee() : BigDecimal.ZERO);
        rate.setOverweightAdditionalFee(BigDecimal.ZERO);
        rate.setActive(req.getActive() != null ? req.getActive() : true);
    }

    private String validateBagSize(String bagSize) {
        if (SMALL.equals(bagSize) || MEDIUM.equals(bagSize) || BIG.equals(bagSize)) {
            return bagSize;
        }
        throw new com.wisecartecommerce.ecommerce.exception.CustomException(
                "Invalid bagSize. Must be one of: " + SMALL + ", " + MEDIUM + ", " + BIG);
    }

    @org.springframework.transaction.annotation.Transactional
    public void toggleActive(Long id, boolean active) {
        JntShippingRate rate = getRateById(id);
        rate.setActive(active);
        rateRepository.save(rate);
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteRate(Long id) {
        if (!rateRepository.existsById(id)) {
            throw new ResourceNotFoundException("J&T shipping rate not found: " + id);
        }
        rateRepository.deleteById(id);
    }

    private static final BigDecimal MAX_JNT_WEIGHT_KG = new BigDecimal("50");
    private static final String SMALL = "Small (<=3KG)";
    private static final String MEDIUM = "Medium (<=5KG)";
    private static final String BIG = "Big (<=8KG)";

    private String norm(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    @org.springframework.transaction.annotation.Transactional
    public void saveRouteRates(com.wisecartecommerce.ecommerce.Dto.Request.JntRouteRateRequest req) {
        BigDecimal overweightFee = req.getOverweightAdditionalFee() != null ? req.getOverweightAdditionalFee() : BigDecimal.ZERO;
        upsertBagRate(req, SMALL, req.getSmallFee(), req.getSmallItemFee(), overweightFee, req.getSmallId());
        upsertBagRate(req, MEDIUM, req.getMediumFee(), req.getMediumItemFee(), overweightFee, req.getMediumId());
        upsertBagRate(req, BIG, req.getBigFee(), req.getBigItemFee(), overweightFee, req.getBigId());
    }

    private void upsertBagRate(com.wisecartecommerce.ecommerce.Dto.Request.JntRouteRateRequest req,
            String bagSize, BigDecimal fee, BigDecimal itemFee, BigDecimal overweightFee, Long existingId) {
        String op = norm(req.getOriginProvince()), oc = norm(req.getOriginCity());
        String dp = norm(req.getDestinationProvince());
        String dc = req.getDestinationCity() != null && !req.getDestinationCity().isBlank()
                ? norm(req.getDestinationCity()) : null;
        String db = req.getDestinationBarangay() != null && !req.getDestinationBarangay().isBlank()
                ? norm(req.getDestinationBarangay()) : null;

        JntShippingRate rate = existingId != null
                ? rateRepository.findById(existingId).orElseGet(JntShippingRate::new)
                : rateRepository.findExactRoute(op, oc, dp, dc, db, bagSize).orElseGet(JntShippingRate::new);

        rate.setOriginProvince(op);
        rate.setOriginCity(oc);
        rate.setDestinationProvince(dp);
        rate.setDestinationCity(dc);
        rate.setDestinationBarangay(db);
        rate.setServiceType(req.getServiceType() != null ? req.getServiceType() : "EZ");
        rate.setBagSize(bagSize);
        rate.setShippingFee(fee != null ? fee : BigDecimal.ZERO);
        rate.setItemAdditionalFee(itemFee != null ? itemFee : BigDecimal.ZERO);
        rate.setOverweightAdditionalFee(overweightFee);
        rate.setActive(req.getActive() != null ? req.getActive() : true);
        rateRepository.save(rate);
    }

    public List<JntShippingRate> getRouteRates(String originProvince, String originCity,
            String destinationProvince, String destinationCity, String destinationBarangay) {
        String db = destinationBarangay != null && !destinationBarangay.isBlank() ? norm(destinationBarangay) : null;
        return rateRepository.findAllBagRatesForRoute(
                norm(originProvince), norm(originCity), norm(destinationProvince), norm(destinationCity), db);
    }

    private String resolveBagSize(BigDecimal weightKg) {
        if (weightKg.compareTo(new BigDecimal("3")) <= 0) {
            return SMALL;
        }
        if (weightKg.compareTo(new BigDecimal("5")) <= 0) {
            return MEDIUM;
        }
        return BIG;
    }

    private BigDecimal calculateOverweightFee(BigDecimal weightKg, BigDecimal routeSurcharge, AppSettings settings) {
        if (weightKg.compareTo(MAX_JNT_WEIGHT_KG) > 0) {
            throw new com.wisecartecommerce.ecommerce.exception.CustomException(
                    "Package weight (" + weightKg + "kg) exceeds J&T Express's 50kg maximum.");
        }
        int ceilKg = weightKg.setScale(0, RoundingMode.CEILING).intValueExact();
        BigDecimal ratePerKg = orDefault(settings != null ? settings.getJntOverweightRatePerKg() : null, new BigDecimal("70"));
        BigDecimal baseFee = orDefault(settings != null ? settings.getJntOverweightBaseFee() : null, new BigDecimal("15"));
        BigDecimal base = ratePerKg.multiply(BigDecimal.valueOf(ceilKg)).add(baseFee);
        BigDecimal surcharge = routeSurcharge != null ? routeSurcharge : BigDecimal.ZERO;
        return base.add(surcharge).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal findRouteSurcharge(String op, String oc, String dp, String dc, String barangay) {
        java.util.Optional<JntShippingRate> anyRow = (barangay != null && dc != null
                ? rateRepository.findByRouteAndBagSizeWithBarangay(op, oc, dp, dc, barangay, BIG)
                : java.util.Optional.<JntShippingRate>empty())
                .or(() -> dc != null
                ? rateRepository.findByRouteAndBagSize(op, oc, dp, dc, BIG)
                : java.util.Optional.empty())
                .or(() -> rateRepository.findByRouteAndBagSizeProvinceWide(op, oc, dp, BIG));
        return anyRow.map(JntShippingRate::getOverweightAdditionalFee).orElse(BigDecimal.ZERO);
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
        String originProvince = norm(req.getOriginProvince());
        String originCity = norm(req.getOriginCity());
        String destProvince = norm(req.getDestinationProvince());
        String destCity = req.getDestinationCity() != null && !req.getDestinationCity().isBlank()
                ? norm(req.getDestinationCity()) : null;
        String barangay = req.getDestinationBarangay() != null && !req.getDestinationBarangay().isBlank()
                ? norm(req.getDestinationBarangay()) : null;

        AppSettings settings = settings();
        boolean isCod = Boolean.TRUE.equals(req.getCod());
        BigDecimal declaredValue = req.getDeclaredValue() != null ? req.getDeclaredValue() : BigDecimal.ZERO;

        BigDecimal valuationRate = orDefault(settings != null ? settings.getJntValuationFeeRate() : null, new BigDecimal("0.01"));
        BigDecimal valuationMin = orDefault(settings != null ? settings.getJntValuationFeeMinimum() : null, new BigDecimal("5"));
        BigDecimal codFeeRate = orDefault(settings != null ? settings.getJntCodFeeRate() : null, new BigDecimal("0.0275"));
        BigDecimal vatRate = orDefault(settings != null ? settings.getVatRate() : null, new BigDecimal("0.12"));

        BigDecimal valuationFee = declaredValue.multiply(valuationRate).max(valuationMin)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal codFee = BigDecimal.ZERO;
        BigDecimal codFeeWithVat = BigDecimal.ZERO;
        if (isCod) {
            codFee = declaredValue.multiply(codFeeRate).setScale(2, RoundingMode.HALF_UP);
            codFeeWithVat = codFee.multiply(BigDecimal.ONE.add(vatRate)).setScale(2, RoundingMode.HALF_UP);
        }

        if (weight.compareTo(new BigDecimal("8")) > 0) {
            BigDecimal surcharge = findRouteSurcharge(originProvince, originCity, destProvince, destCity, barangay);
            BigDecimal overweightShipping = calculateOverweightFee(weight, surcharge, settings);
            BigDecimal total = overweightShipping.add(valuationFee).add(codFeeWithVat).setScale(2, RoundingMode.HALF_UP);
            return new JntEstimateResponse(originProvince, originCity, destProvince, destCity, barangay,
                    "EZ", "Auto-calculated (>8KG)", weight, overweightShipping, BigDecimal.ZERO,
                    valuationFee, codFee, codFeeWithVat, total);
        }

        String bagSize = resolveBagSize(weight);
        JntShippingRate rate = (barangay != null && destCity != null
                ? rateRepository.findByRouteAndBagSizeWithBarangay(originProvince, originCity, destProvince, destCity, barangay, bagSize)
                : java.util.Optional.<JntShippingRate>empty())
                .or(() -> destCity != null
                ? rateRepository.findByRouteAndBagSize(originProvince, originCity, destProvince, destCity, bagSize)
                : java.util.Optional.empty())
                .or(() -> rateRepository.findByRouteAndBagSizeProvinceWide(originProvince, originCity, destProvince, bagSize))
                .orElseThrow(() -> new ResourceNotFoundException(
                "No J&T rate configured for " + bagSize + " on this route yet."));

        BigDecimal shippingFee = rate.getShippingFee().setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemFee = rate.getItemAdditionalFee() != null ? rate.getItemAdditionalFee() : BigDecimal.ZERO;
        BigDecimal total = shippingFee.add(itemFee).add(valuationFee).add(codFeeWithVat).setScale(2, RoundingMode.HALF_UP);

        return new JntEstimateResponse(
                rate.getOriginProvince(), rate.getOriginCity(),
                rate.getDestinationProvince(), rate.getDestinationCity(),
                rate.getDestinationBarangay(),
                rate.getServiceType(), rate.getBagSize(),
                weight, shippingFee, itemFee, valuationFee, codFee, codFeeWithVat, total
        );
    }
}
