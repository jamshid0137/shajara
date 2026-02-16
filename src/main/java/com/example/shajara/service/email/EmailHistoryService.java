package com.example.shajara.service.email;


import com.example.shajara.entity.history.EmailHistoryEntity;
import com.example.shajara.enums.SmsType;
import com.example.shajara.repository.EmailHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmailHistoryService {
    @Autowired
    private EmailHistoryRepository emailHistoryRepository;

    public void create(String email, String message,String code, SmsType smsType){
        EmailHistoryEntity entity=new EmailHistoryEntity();
        entity.setEmail(email);
        entity.setCode(code);
        entity.setAttempt_count(0);
        entity.setSmsType(smsType);
        entity.setCreatedDate(LocalDateTime.now());

        emailHistoryRepository.save(entity);
    }

    public Long countEmail(String email){
        LocalDateTime now=LocalDateTime.now();

        return emailHistoryRepository.countByEmailAndCreatedDateBetween(email,now.minusMinutes(10),now);
        //Berilgan emailga Ushbu metod so‘nggi 10 daqiqa ichida shu emailga yuborilgan kodlar sonini hisoblaydi.
    }


    public boolean check(String email,String code){
        //find last sended sms to this phone
        Optional<EmailHistoryEntity> optional=emailHistoryRepository.findTop1ByEmailOrderByCreatedDateDesc(email);
        if(optional.isEmpty()){
            return false;
        }
        //check with code to equals

        EmailHistoryEntity entity= optional.get();
        if(!entity.getCode().equals(code)){
            //URINISHLARNI REPOSITORYDA OSHIRAMIZA
            emailHistoryRepository.updateAttemptCount(entity.getId());
            return false;
        }
        //check time 2 minutdan keyin yaroqsiz sms
        LocalDateTime expDate=entity.getCreatedDate().plusMinutes(2);
        if(expDate.isBefore(LocalDateTime.now())){   // >
            return false;
        }
        //URINISHLAR SONI OSHIB KETSA YAROQSIZ KOD BO'LADI
        if(entity.getAttempt_count()>3){
            return false;
        }

        return true;
    }
}