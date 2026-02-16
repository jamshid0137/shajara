package com.example.shajara.repository;


import com.example.shajara.entity.history.EmailHistoryEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailHistoryRepository extends CrudRepository<EmailHistoryEntity,String> {

    Long countByEmailAndCreatedDateBetween(String email, LocalDateTime from, LocalDateTime to);

    Optional<EmailHistoryEntity> findTop1ByEmailOrderByCreatedDateDesc(String email);//oxirgi jo'natilgan emailni oladi.

    @Modifying
    @Transactional                  /// UNI 1 GA OSHIRADI
    @Query("update EmailHistoryEntity set attempt_count=  coalesce( attempt_count,0)+1 where id=?1")//attempt_count null bo'lmasa o'zini aks xolda 0ni jo'nat degani.
    void updateAttemptCount(String id);
}

