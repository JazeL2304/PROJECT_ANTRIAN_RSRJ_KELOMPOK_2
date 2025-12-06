package com.example.projectantrianrsrjkelompok2.model

import com.google.gson.annotations.SerializedName

/**
 * ✅ ImageKit Upload Response Model
 * Response dari ImageKit API setelah upload image
 */
data class ImageKitUploadResponse(
    @SerializedName("fileId")
    val fileId: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("url")
    val url: String,

    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @SerializedName("height")
    val height: Int,

    @SerializedName("width")
    val width: Int,

    @SerializedName("size")
    val size: Long,

    @SerializedName("filePath")
    val filePath: String,

    @SerializedName("fileType")
    val fileType: String
)