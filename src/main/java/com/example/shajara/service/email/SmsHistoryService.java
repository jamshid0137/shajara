package com.example.shajara.service.email;


import com.example.shajara.entity.history.SmsHistoryEntity;
import com.example.shajara.enums.SmsType;
import com.example.shajara.repository.SmsHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SmsHistoryService {
    @Autowired
    private SmsHistoryRepository smsHistoryRepository;

    public void create(String phone, String message,String code, SmsType smsType){
        SmsHistoryEntity entity=new SmsHistoryEntity();
        entity.setUsername(phone);
        entity.setCode(code);
        entity.setAttempt_count(0);
        entity.setMessage(message);
        entity.setSmsType(smsType);
        entity.setCreatedDate(LocalDateTime.now());

        smsHistoryRepository.save(entity);
    }

    public Long countSms(String phone){
        LocalDateTime now=LocalDateTime.now();

        return smsHistoryRepository.countByUsernameAndCreatedDateBetween(phone,now.minusMinutes(10),now);//10 minutda o'sha nomerga nechta sms bor
    }


    public boolean check(String phone,String code){
        //find last sended sms to this phone

        Optional<SmsHistoryEntity> optional=smsHistoryRepository.findTop1ByUsernameOrderByCreatedDateDesc(phone);
        if(optional.isEmpty()){
            return false;
        }

        //check with code to equals
        SmsHistoryEntity entity= optional.get();
        if(!entity.getCode().equals(code)){
            //REPOSITORYNI O'ZIDA UPDATE QILAMIZA
            smsHistoryRepository.updateAttemptCount(entity.getId());

            return false;
        }

        //check time 2 minutdan keyin yaroqsiz sms
        LocalDateTime expDate=entity.getCreatedDate().plusMinutes(2);
        if(expDate.isBefore(LocalDateTime.now())){   // >
            return false;
        }

        //o'sha sms uchun urunishlar soni
        if(entity.getAttempt_count()>3){
            return false;
        }

        return true;

    }
}
