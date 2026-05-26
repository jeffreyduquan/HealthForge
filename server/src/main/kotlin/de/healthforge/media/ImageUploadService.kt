package de.healthforge.media

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.SetBucketPolicyArgs
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import net.coobird.thumbnailator.Thumbnails

/**
 * Server-side image pipeline for recipe (+ later supplement/avatar) uploads.
 *
 * Pipeline (REQ-RECIPE-006):
 *  1. Client uploads JPEG / PNG / WebP (≤ ~5 MB, ≤ 1080×1080 after client compression).
 *  2. Server resizes to three variants:
 *      - thumb  256 px (square fit, JPEG q 85)
 *      - medium 800 px
 *      - full   1600 px
 *  3. All variants stored in MinIO under the same logical key with size suffix:
 *      `<bucket>/<key>__thumb.jpg`, `__medium.jpg`, `__full.jpg`.
 *  4. The response returns the logical key (without suffix); client constructs concrete URLs
 *     via `<MINIO_PUBLIC_BASE_URL>/<bucket>/<key>__<variant>.jpg`.
 */

@Configuration
class MinioConfig(
    @Value("\${healthforge.minio.endpoint}") private val endpoint: String,
    @Value("\${healthforge.minio.access-key}") private val accessKey: String,
    @Value("\${healthforge.minio.secret-key}") private val secretKey: String,
) {
    @Bean
    fun minioClient(): MinioClient =
        MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
}

@Component
class MinioBucketInitializer(
    private val client: MinioClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val publicBuckets = listOf("recipes", "ingredients", "supplements", "avatars")
    private val privateBuckets = listOf("exports", "backups")

    @PostConstruct
    fun ensureBuckets() {
        runCatching {
            (publicBuckets + privateBuckets).forEach { bucket ->
                val exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())
                if (!exists) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                    log.info("Created MinIO bucket: {}", bucket)
                }
            }
            publicBuckets.forEach { bucket ->
                val policy = """
                    {
                      "Version":"2012-10-17",
                      "Statement":[
                        {"Effect":"Allow","Principal":{"AWS":["*"]},"Action":["s3:GetObject"],"Resource":["arn:aws:s3:::$bucket/*"]}
                      ]
                    }
                """.trimIndent()
                client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build())
            }
        }.onFailure { log.warn("MinIO bucket init skipped: {}", it.message) }
    }
}

@Service
class ImageUploadService(
    private val client: MinioClient,
) {

    data class Variant(val suffix: String, val maxEdge: Int)
    private val variants = listOf(
        Variant("thumb", 256),
        Variant("medium", 800),
        Variant("full", 1600),
    )

    /**
     * Resizes the source bytes into all three variants and uploads them to MinIO.
     * Returns the *logical* key (without size suffix).
     */
    fun upload(bucket: String, sourceBytes: ByteArray, originalContentType: String): String {
        require(sourceBytes.isNotEmpty()) { "empty payload" }
        require(bucket in setOf("recipes", "supplements", "avatars")) { "bucket $bucket not allowed for image upload" }

        val src = ImageIO.read(ByteArrayInputStream(sourceBytes))
            ?: throw IllegalArgumentException("not a decodable image (got $originalContentType)")

        val baseKey = UUID.randomUUID().toString()

        variants.forEach { v ->
            val out = ByteArrayOutputStream()
            Thumbnails.of(src)
                .size(v.maxEdge, v.maxEdge)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toOutputStream(out)
            val bytes = out.toByteArray()
            ByteArrayInputStream(bytes).use { input ->
                client.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .`object`("${baseKey}__${v.suffix}.jpg")
                        .stream(input, bytes.size.toLong(), -1)
                        .contentType("image/jpeg")
                        .build()
                )
            }
        }
        return baseKey
    }
}
