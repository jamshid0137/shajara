package com.example.shajara.repository;


import com.example.shajara.entity.history.SmsHistoryEntity;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SmsHistoryRepository extends CrudRepository<SmsHistoryEntity,String> {

    //Berilgan username (yoki telefon raqami) bo‘yicha ma’lum vaqt oralig‘ida yuborilgan SMSlar sonini hisoblaydi.
    Long countByUsernameAndCreatedDateBetween(String username, LocalDateTime from, LocalDateTime to);

    //Berilgan username yoki telefon raqam bo‘yicha oxirgi yuborilgan SMSni olish.
    Optional<SmsHistoryEntity> findTop1ByUsernameOrderByCreatedDateDesc(String phone);

    @Modifying
    @Transactional        //SMS kodi noto‘g‘ri kiritilganda urinishlar sonini (attempt_count) oshiradi.
    @Query("update SmsHistoryEntity set attempt_count=  coalesce( attempt_count,0)+1 where id=?1")
    void updateAttemptCount(String id);

}
