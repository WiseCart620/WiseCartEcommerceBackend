package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.PromoCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoCardRepository extends JpaRepository<PromoCard, Long> {

    List<PromoCard> findAllByOrderByDisplayOrderAsc();

    List<PromoCard> findByActiveTrueOrderByDisplayOrderAsc();
}