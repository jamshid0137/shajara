package com.example.shajara.service.email;


import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailSendingService {

//    @Value("${spring.mail.username}")
//    private String fromAccount;//email usernameni olvolish.
//    @Value("${server.domain}")
//    private String serverDomain;
//    @Autowired
//    private JavaMailSender javaMailSender;
//
//    @Autowired
//    private EmailHistoryService emailHistoryService;
//    private Integer smsLimit=3;

    public void sendRegistrationEmail(String email, Integer profileId){
//        String title="Complete Registration email";
//        String body="<!DOCTYPE html>\n" +
//                "<html lang=\"en\">\n" +
//                "<head>\n" +
//                "    <style>\n" +
//                "        a {\n" +
//                "            padding: 10px 30px;\n" +
//                "            display: inline-block;\n" +
//                "        }\n" +
//                "\n" +
//                "        .button-link {\n" +
//                "            text-decoration: none;\n" +
//                "            color: white;\n" +
//                "            background-color: indianred;\n" +
//                "        }\n" +
//                "        .button-link:hover {\n" +
//                "            background-color: #dd4444;\n" +
//                "        }\n" +
//                "\n" +
//                "    </style>\n" +
//                "    <meta charset=\"UTF-8\">\n" +
//                "    <title>Title</title>\n" +
//                "</head>\n" +
//                "<body>\n" +
//                "\n" +
//                "<h1>Complete Registration email</h1>\n" +
//                "<p>Salom yaxshimisiz</p>\n" +
//                "<p>\n" +
//                "    Please lick to link <a class=\"button-link\"\n" +
//                "        href=\"%s/auth/registration/email-verification/%s\" target=\"_blank\">click there</a>\n" +
//                "</p>\n" +
//                "\n" +
//                "</body>\n" +
//                "</html>";
//        body=String.format(body,serverDomain, JwtUtil.encode(profileId));//%s larni almashtiradi !
//
//        //String body="Please lick to link http://localhost:8080/auth/registration/verification/"+encode(profileId);
//
//
//        sendMimeEmail(email,title,body);
    }



    private void sendMimeEmail(String email, String subject, String body){//subject titlesi,body-ichi xatni.

//        MimeMessage msg = javaMailSender.createMimeMessage();
//        try {
//            msg.setFrom(fromAccount);
//
//            MimeMessageHelper helper = new MimeMessageHelper(msg, true);//true fayllar bn degani
//            helper.setTo(email);
//            helper.setSubject(subject);
//            helper.setText(body, true);//bodyni html formatdan parse qilib ishlat
//
//            CompletableFuture.runAsync(()->{
//                javaMailSender.send(msg);
//            });//har bir user uchun alohida thread yaratib asosiy threaddan ajratib responseni tez yuboradigan qilamiz
//
//
//        } catch (MessagingException e) {
//            throw new RuntimeException(e);
//        }
    }



    private void sendSimpleEmail(String email, String subject, String body){//subject titlesi,body-ichi xatni.
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setFrom(fromAccount);
//        msg.setTo(email);
//        msg.setSubject(subject);
//        msg.setText(body);
//        javaMailSender.send(msg);
    }


    public void sendResetPasswordEmail(String email){
//        String title="Reset Password Confirmation";
//        String code= RandomUtil.getRandomSmsCode();
//        String body="How are you,Mazgi.This is confirm code for reset Password.Your code:  "+code;
//
//        checkAndSendRegistrationEmail(email,title,body,code);
    }


    public void sendChangeUsernameEmail(String email){
//        String title="change username Confirmation";
//        String code= RandomUtil.getRandomSmsCode();
//        String body="How are you,Mazgi.This is confirm code for change username.Your code:  "+code;
//
//        checkAndSendRegistrationEmail(email,title,body,code);
    }

    public void sendShareUsernameEmail(String email,String name,String treeName){
//        String title="Invite you special family tree";
//        String code= String.valueOf(name);
//        String body=code+"dan sizga : "+treeName+" nomli familytree jo'natildi !";
//
//        checkAndSendRegistrationEmail(email,title,body,code);
    }


    public void checkAndSendRegistrationEmail(String email,String title,String body,String code){
//        //checking
//        Long count=emailHistoryService.countEmail(email);
//
//        if(count>=smsLimit){
//            throw new AppBadException("10 minutda maximal 2 ta sms yuboraloasiz !");
//        }
//
//        sendMimeEmail(email,title,body);//body ichida bor code !
//        emailHistoryService.create(email,title,code, SmsType.CHANGE_USERNAME_CONFIRM); //todo smstypega qaramay tekshiryapmiza
    }



}
