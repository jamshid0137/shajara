package com.example.shajara.repository;

import com.example.shajara.entity.ProfileRoleEntity;
import com.example.shajara.enums.ProfileRole;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileRoleRepository extends JpaRepository<ProfileRoleEntity,Integer> {
    @Transactional //xatolik yuz bersa butun transaksiyalar bekor qilnadi
    //transaksiya-rollback qilinadi xatolik bo'lsa yani amal orqaga qaytadi.
    @Modifying //annotatsiyasi ma'lumotlar bazasida faqat o'zgartirishlar amalga oshiradigan so'rovlar uchun mo'ljallangan.
    void deleteByProfileId(Integer id);

    @Query("select p.roles FROM ProfileRoleEntity p where p.profileId=?1")
    List<ProfileRole>getAllRolesListByProfileId(Integer profileId); //idsi boyicha rollarini olish

}
/*
@Transactional
@Modifying
bular add,update,delete metodlarda ishlatiladi
 */