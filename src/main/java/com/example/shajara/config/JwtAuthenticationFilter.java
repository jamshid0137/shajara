package com.example.shajara.config;


import com.example.shajara.dto.JwtDTO;
import com.example.shajara.repository.ProfileRepository;
import com.example.shajara.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private ProfileRepository profileRepository;

    @Override//dofilter metodiga kirmaydigan urllar
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {//permit all lar uchun filter qilma degani
        AntPathMatcher pathMatcher = new AntPathMatcher();
        return Arrays
                .stream(SecurityConfig.AUTH_WHITELIST)
                .anyMatch(p -> pathMatcher.match(p, request.getServletPath()));
    }

    @Override //har gal request kelayotganda manashu filterga tushadi va nimadirini o'zgartiramizmi yo o'tkazivoramizmi shuni qiladi
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader("Authorization");//kelayotgandan Authorization bn boshlanganini olamiz
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Continue the filter chain
            return;
        }

        try {//block account va hzolar chiqsa ushlashimiz uchun
            final String token = header.substring(7).trim();
            JwtDTO jwtDTO = JwtUtil.decode(token);
            // load user depending on role

            //String phone = jwtDTO.getPhone();
            //Integer id= jwtDTO.getId();
            //String username=profileRepository.findById(id).get().getUsername();
            String username=jwtDTO.getUsername();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            //beanlar ioc contenerga joylashtirilgani kabi contxHolderga set qiladi
            //auth request haqida biladi va shu requstni kim murojat qilayotganini biladi
            //manashu requestga borganda manashu userdetailsni bizga taqdim qiladi
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));//manashu authga shu requestni ula degani
            SecurityContextHolder.getContext().setAuthentication(authentication);
            //***
            WebAuthenticationDetails details = (WebAuthenticationDetails) authentication.getDetails();
            String ipAddress = details.getRemoteAddress();
            String sessionId = details.getSessionId();
            System.out.println(ipAddress);
            System.out.println(sessionId);
            //***
            filterChain.doFilter(request, response); // Continue the filter chain
        } catch (JwtException | UsernameNotFoundException e) {
            filterChain.doFilter(request, response); // Continue the filter chain
            return;
        }
    }
}
/*
murojat qilayotganda headerdan jwtni bor yo'qligini tekshirib
jwt bo'lsa undan encode qilib userni idsini oladi shunaqa userli profileni topadi
uni spring security conteks holderga joylashtirib qo'yadi bu jwt filter.
 */