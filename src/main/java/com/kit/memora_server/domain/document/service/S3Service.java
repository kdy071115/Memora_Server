package com.kit.memora_server.domain.document.service;

import com.kit.memora_server.global.exception.BusinessException;
import com.kit.memora_server.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 파일 저장 서비스.
 *
 * - AWS 자격증명이 있으면 S3 업로드
 * - 없으면 로컬 디스크 (`storage.local-path`, 기본 `./uploads`) 에 저장
 *
 * 어느 모드든 반환값 `storedPath` 를 그대로 AI 서버에 전달하면 됩니다.
 * 로컬 모드의 storedPath 는 절대경로이므로, AI 서버는 첫 글자가 `/` 인 경우 디스크에서 직접 읽습니다.
 */
@Slf4j
@Service
public class S3Service {

    private final S3Client s3Client;
    private final String bucket;
    private final Path localRoot;
    private final boolean useS3;

    public S3Service(
            ObjectProvider<S3Client> s3ClientProvider,
            @Value("${cloud.aws.s3.bucket:memora-uploads}") String bucket,
            @Value("${storage.local-path:./uploads}") String localPath
    ) {
        this.s3Client = s3ClientProvider.getIfAvailable();
        this.bucket = bucket;
        this.localRoot = Paths.get(localPath).toAbsolutePath().normalize();
        this.useS3 = this.s3Client != null;
    }

    @PostConstruct
    void init() {
        if (useS3) {
            log.info("[Storage] mode=S3, bucket={}", bucket);
        } else {
            try {
                Files.createDirectories(localRoot);
            } catch (IOException e) {
                throw new IllegalStateException("로컬 업로드 디렉토리 생성 실패: " + localRoot, e);
            }
            log.info("[Storage] mode=LOCAL, path={}", localRoot);
        }
    }

    public String upload(MultipartFile file, String directory) {
        String filename = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        String relativeKey = directory + "/" + filename;

        if (useS3) {
            return uploadToS3(file, relativeKey);
        }
        return uploadToLocal(file, relativeKey);
    }

    private String uploadToS3(MultipartFile file, String key) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException e) {
            log.error("S3 업로드 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private String uploadToLocal(MultipartFile file, String key) {
        try {
            Path target = localRoot.resolve(key).normalize();
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
            log.info("로컬 파일 저장: {}", target);
            return target.toString();
        } catch (IOException e) {
            log.error("로컬 파일 저장 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
