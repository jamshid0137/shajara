package com.example.shajara.repository;


import com.example.shajara.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    List<Person> findAllByFamilyTreeId(Long treeId);

    List<Person> findByFamilyTreeId(Long treeId);

    List<Person> findAllByFatherIdOrMotherId(Long fatherId, Long motherId);

}