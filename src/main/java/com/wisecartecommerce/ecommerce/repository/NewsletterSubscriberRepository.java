package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Long> {
    Optional<NewsletterSubscriber> findByEmail(String email);
    List<NewsletterSubscriber> findByActiveTrueOrderBySubscribedAtDesc();
}