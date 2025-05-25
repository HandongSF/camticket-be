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
        try {
            val key = extractKeyFromUrl(imageUrl)
            val deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()

            s3Client.deleteObject(deleteRequest)
            println("🗑️ S3에서 삭제 완료: $key")
        } catch (e: Exception) {
            println("⚠️ S3 삭제 실패 (무시됨): $imageUrl, 이유: ${e.message}")
        }
    }


    fun deleteAll(imageUrls: List<String>) {
        imageUrls.forEach { url ->
            try {
                delete(url) // 내부에서도 try-catch 있지만, 중첩해도 문제없음
            } catch (e: Exception) {
                println("⚠️ 다중 삭제 중 실패 (무시됨): $url, 이유: ${e.message}")
            }
        }
    }


    // URL → S3 Key 추출
    private fun extractKeyFromUrl(url: String): String {
        val baseUrl = "https://$bucket.s3.$region.amazonaws.com/"
        if (!url.startsWith(baseUrl)) {
            throw IllegalArgumentException("URL does not match S3 base format: $url")
        }
        return url.removePrefix(baseUrl)
    }


}

