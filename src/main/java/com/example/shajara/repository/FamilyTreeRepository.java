package com.example.shajara.repository;

import com.example.shajara.entity.FamilyTree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByProfileId(Integer profileId);

    Optional<FamilyTree> findByIdAndProfileId(Long id, Integer profileId);
}
