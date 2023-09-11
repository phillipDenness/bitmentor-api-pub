package bitmentor.api.model.file

import bitmentor.api.config.Properties

enum class Buckets(val label: String) {
    IMAGE_BUCKET_NAME("bitmentor-${Properties.environment}-display-images"),
    TUTOR_VERIFICATION_BUCKET_NAME("bitmentor-${Properties.environment}-tutor-verification-files")
}
