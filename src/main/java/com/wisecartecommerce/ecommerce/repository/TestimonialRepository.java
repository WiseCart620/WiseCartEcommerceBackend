package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
    List<Testimonial> findAllByOrderByDisplayOrderAsc();
    List<Testimonial> findByActiveTrueOrderByDisplayOrderAsc();
}