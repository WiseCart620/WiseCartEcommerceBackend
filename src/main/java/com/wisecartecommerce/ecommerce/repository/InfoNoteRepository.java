package com.wisecartecommerce.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.InfoNote;

@Repository
public interface InfoNoteRepository extends JpaRepository<InfoNote, Long> {

    List<InfoNote> findAllByOrderByDisplayOrderAsc();

    List<InfoNote> findByActiveTrueAndAppliesToAllFalseOrderByDisplayOrderAsc();

    List<InfoNote> findByActiveTrueAndAppliesToAllTrueOrderByDisplayOrderAsc();
}