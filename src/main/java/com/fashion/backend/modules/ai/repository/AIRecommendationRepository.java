package com.fashion.backend.modules.ai.repository;

import com.fashion.backend.modules.ai.entity.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIRecommendationRepository extends JpaRepository<AIRecommendation, Long> {
    List<AIRecommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
