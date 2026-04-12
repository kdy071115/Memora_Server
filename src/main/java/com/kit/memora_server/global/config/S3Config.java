package com.kit.memora_server.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3Client 빈은 AWS access-key/secret-key 가 모두 채워져 있을 때만 등록됩니다.
 * 비어 있으면 S3Service 가 자동으로 로컬 파일 저장 모드로 폴백합니다.
 */
@Slf4j
@Configuration
public class S3Config {

    @Value("${cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Bean
    @ConditionalOnExpression(
            "'${cloud.aws.credentials.access-key:}' != '' and '${cloud.aws.credentials.secret-key:}' != ''"
    )
    public S3Client s3Client() {
        log.info("S3 자격증명 감지 → S3Client 빈 등록 (region={})", region);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
