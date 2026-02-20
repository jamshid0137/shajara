package com.example.shajara.repository;


import com.example.shajara.entity.ProfileEntity;
import com.example.shajara.enums.GeneralStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends CrudRepository<ProfileEntity,Integer> {
    Optional<ProfileEntity> findById(Integer id);
    //select *
    Optional<ProfileEntity>findByUsernameAndVisibleTrue(String username);

    Optional<ProfileEntity>findByUsername(String username);

    Optional<ProfileEntity>findByIdAndVisibleTrue(Integer id);

    @Modifying
    @Transactional
    @Query("update ProfileEntity set status=?2 where id=?1")
    void changeStatus(Integer id, GeneralStatus status);

    @Modifying
    @Transactional
    @Query("update ProfileEntity set password=?2 where id=?1")
    void changePassword(Integer id, String password);

    @Modifying
    @Transactional
    @Query("update ProfileEntity set name=?2 where id=?1")
    void changeName(Integer userId, String name);

    @Modifying
    @Transactional
    @Query("update ProfileEntity set tempUsername=?2 where id=?1")
    void changeTempUsername(Integer userId, String tempUsername);

    @Modifying
    @Transactional
    @Query("update ProfileEntity set username=?2 where id=?1")
    void changeUsername(Integer userId, String username);

}
