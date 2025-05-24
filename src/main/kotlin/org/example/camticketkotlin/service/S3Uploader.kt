package org.example.camticketkotlin.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.core.sync.RequestBody
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class S3Uploader(
    private val s3Client: S3Client,
    @Value("\${spring.cloud.aws.s3.bucket}") private val bucket: String,
    @Value("\${spring.cloud.aws.region.static}") private val region: String
) {
    // 단일 업로드
    fun upload(file: MultipartFile, folder: String): String {
        val fileName = "$folder/${UUID.randomUUID()}.${file.originalFilename?.substringAfterLast('.')}"
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(fileName)
            .contentType(file.contentType)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream, file.size))
        return "https://$bucket.s3.$region.amazonaws.com/$fileName"
    }

    // 다중 업로드
    fun upload(files: List<MultipartFile>, folder: String): List<String> {
        return files.map { upload(it, folder) } // 위 함수 재사용
    }
}

