package ua.com.radiokot.photoprism.features.gallery.logic

import ua.com.radiokot.photoprism.api.photos.model.PhotoPrismMergedPhoto
import ua.com.radiokot.photoprism.extension.kLogger

class PhotoPrismPreviewUrlFactory(
    apiUrl: String,
    private val previewToken: String,
    private val videoFormatSupport: VideoFormatSupport,
) : MediaPreviewUrlFactory {
    private val log = kLogger("PPPreviewUrlFactory")

    private val previewUrlBase = "${apiUrl}v1"

    override fun getSmallThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_224"

    override fun getMediumThumbnailUrl(hash: String): String =
        "$previewUrlBase/t/$hash/$previewToken/tile_500"

    override fun getImagePreview720Url(hash: String): String =
        getFitPreviewUrl(hash, 720)

    override fun getImagePreview1280Url(hash: String): String =
        getFitPreviewUrl(hash, 1280)

    override fun getImagePreview1920Url(hash: String): String =
        getFitPreviewUrl(hash, 1920)

    override fun getImagePreview2048Url(hash: String): String =
        getFitPreviewUrl(hash, 2048)

    override fun getImagePreview2560Url(hash: String): String =
        getFitPreviewUrl(hash, 2560)

    override fun getImagePreview3840Url(hash: String): String =
        getFitPreviewUrl(hash, 3840)

    override fun getImagePreview4096Url(hash: String): String =
        getFitPreviewUrl(hash, 4096)

    override fun getImagePreview7680Url(hash: String): String =
        getFitPreviewUrl(hash, 7680)

    private fun getFitPreviewUrl(hash: String, size: Int) =
        "$previewUrlBase/t/$hash/$previewToken/fit_$size"

    override fun getVideoPreviewUrl(mergedPhoto: PhotoPrismMergedPhoto): String {
        // https://github.com/photoprism/photoprism/blob/2f9792e5411f6bb47a84b638dfc42d51b7790853/frontend/src/model/photo.js#L489

        val videoFile = mergedPhoto.videoFile
        // Valid case for live photos.
            ?: return "$previewUrlBase/videos/${mergedPhoto.hash}/$previewToken/$DEFAULT_VIDEO_PREVIEW_FORMAT"
                .also {
                    log.debug {
                        "getVideoPreviewUrl(): no_video_file_found:" +
                                "\nphotoUid=${mergedPhoto.uid}," +
                                "\npreviewFormat=$DEFAULT_VIDEO_PREVIEW_FORMAT"
                    }
                }

        val videoCodec = videoFile.codec ?: ""

        val previewFormat = when {
            (videoCodec == "hvc1" || videoCodec == "hev1") && videoFormatSupport.canPlayHevc() ->
                "hevc"

            videoCodec == "vp8" && videoFormatSupport.canPlayVp8() ->
                "vp8"

            videoCodec == "vp9" && videoFormatSupport.canPlayVp9() ->
                "vp9"

            (videoCodec == "av01" || videoCodec == "av1c") && videoFormatSupport.canPlayAv1() ->
                "av01"

            // WebM and OGV seems not supported.

            else ->
                DEFAULT_VIDEO_PREVIEW_FORMAT
        }

        return "$previewUrlBase/videos/${videoFile.hash}/$previewToken/$previewFormat"
            .also { result ->
                log.debug {
                    "getVideoPreviewUrl(): preview_url_created:" +
                            "\nphotoUid=${mergedPhoto.uid}," +
                            "\nvideoCodec=$videoCodec," +
                            "\npreviewFormat=$previewFormat"
                }
            }
    }

    private companion object {
        private const val DEFAULT_VIDEO_PREVIEW_FORMAT = "avc"
    }
}
