package com.example.shajara.util;


import com.example.shajara.dto.JwtDTO;
import com.example.shajara.enums.ProfileRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.stream.Collectors;

public class JwtUtil {
    private static final long tokenLiveTime1 = 1000 * 3600 * 24 * 2;  //loginni 2 kunlik qildim tokenni
    private static final int tokenLiveTime = 1000 * 3600 ; // 1-hour emailga jo'natilgan linkni muddati
    private static final String secretKey = "veryLongSecretmazgillattayevlasharaaxmojonjinnijonsurbetbekkiydirhonuxlatdibekloxovdangasabekochkozjonduxovmashaynikmaydagapchishularnioqiganbolsangizgapyoqaniqsizmazgi";


    public static String encode(String username,Integer id, List<ProfileRole> roleList) {
        String strRoles=roleList.stream().map(item->item.name()) //rolni nameni stringga o'tkazyapmiz
                .collect(Collectors.joining(","));//hosil bo'lganni bir biriga , orqali qo'shyapmiz

        Map<String,String>claims=new HashMap<>();
        claims.put("role",strRoles);
        claims.put("id",String.valueOf(id));


        return Jwts
                .builder()

                .subject(username) //setsubject qildik
                .claims(claims)//rolelarini map qilib berdik
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenLiveTime1))   //account logini uchun
                .signWith(getSignInKey())
                .compact();
    }

    public static JwtDTO decode(String token) {
        Claims claims = Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username=claims.getSubject();//setsubject ichidagini olish

        Integer id= Integer.valueOf((String) claims.get("id"));

        String strRoleList=(String) claims.get("role");

        String [] roleList=strRoleList.split(",");//berilgan rollar yigindisini ajratish har verguldan
        List<ProfileRole> profileRoles=new LinkedList<>();
        for(String role:roleList){
            profileRoles.add(ProfileRole.valueOf(role));
        }
        //yoki
        List<ProfileRole> profileRoles2=Arrays.stream(strRoleList.split(","))
                        .map(item->ProfileRole.valueOf(item))
                        .collect(Collectors.toList());


        return new JwtDTO(username,id,profileRoles2);
    }









    public static String encode(Integer id) {//emailda idni shifrlash uchun

        return Jwts
                .builder()

                .subject(String.valueOf(id)) //setsubject qildik
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenLiveTime))
                .signWith(getSignInKey())
                .compact();
    }

    public static Integer decodeRegVerToken(String token) {//emaildagi idni olish uchun
        Claims claims = Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Integer id=Integer.valueOf(claims.getSubject());
        return id;
    }

    private static SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}