package de.healthforge.media

import de.healthforge.auth.AuthPrincipal
import de.healthforge.common.ApiException
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/media")
class ImageUploadController(
    private val uploadService: ImageUploadService,
) {

    /**
     * `bucket` constrained to {recipes, supplements, avatars}.
     * Max payload size enforced by `spring.servlet.multipart.max-file-size` (configure if needed).
     * REQ-RECIPE-006: clients should pre-compress to ≤1080×1080 / WebP / ≤200 KB; the server
     * accepts up to 5 MB to absorb client variance and re-encodes everything into the 3 variants.
     */
    @PostMapping("/upload")
    fun upload(
        @AuthenticationPrincipal principal: AuthPrincipal?,
        @RequestParam("bucket") bucket: String,
        @RequestParam("file") file: MultipartFile,
    ): Map<String, String> {
        principal ?: throw ApiException(HttpStatus.UNAUTHORIZED, "NO_PRINCIPAL", "authentication required")
        if (file.isEmpty) throw ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILE", "file is empty")
        if (file.size > 5L * 1024 * 1024) throw ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "max 5 MB")
        val key = try {
            uploadService.upload(bucket, file.bytes, file.contentType ?: "application/octet-stream")
        } catch (e: IllegalArgumentException) {
            throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", e.message ?: "invalid image")
        }
        return mapOf("key" to key, "bucket" to bucket)
    }
}
