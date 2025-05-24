package org.example.camticketkotlin.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.core.sync.RequestBody
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
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

    // 단일 삭제
    fun delete(imageUrl: String) {
        val key = extractKeyFromUrl(imageUrl)
        val deleteRequest = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        s3Client.deleteObject(deleteRequest)
    }

    // 다중 삭제
    fun deleteAll(imageUrls: List<String>) {
        imageUrls.forEach { delete(it) }
    }

    // URL → S3 Key 추출
    private fun extractKeyFromUrl(url: String): String {
        val baseUrl = "https://$bucket.s3.$region.amazonaws.com/"
        return url.removePrefix(baseUrl)
    }

}

