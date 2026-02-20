//package com.example.shajara.config;
//
//
//
//import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.presigner.S3Presigner;
//import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
//import software.amazon.awssdk.services.s3.model.GetObjectRequest;
//
//import org.springframework.stereotype.Service;
//
//import java.time.Duration;
//
//@Service
//public class S3SignedUrlService {
//
//    private final S3Presigner presigner;
//
//    public S3SignedUrlService() {
//        this.presigner = S3Presigner.builder()
//                .region(Region.US_EAST_1) // Sizning S3 regioningiz
//                .credentialsProvider(DefaultCredentialsProvider.create())
//                .build();
//    }
//
//    public String generateSignedUrl(String bucketName, String key, long minutesValid) {
//        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
//                .bucket(bucketName)
//                .key(key)
//                .build();
//
//        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
//                .getObjectRequest(getObjectRequest)
//                .signatureDuration(Duration.ofMinutes(minutesValid))
//                .build();
//
//        return presigner.presignGetObject(presignRequest).url().toString();
//    }
//}
