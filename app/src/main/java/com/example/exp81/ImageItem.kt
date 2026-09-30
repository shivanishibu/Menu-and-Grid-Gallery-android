package com.example.exp81

/**
 * Simple model representing one grid cell.
 * type tells the adapter where to load the image bitmap from:
 *  DRAWABLE      -> app's own res/drawable resources
 *  PHONE_STORAGE -> images picked up from the device gallery (MediaStore)
 *  URL           -> images downloaded from the internet (loaded with Glide)
 */
data class ImageItem(
    val type: Type,
    val label: String,
    var drawableResId: Int = 0,   // used when type == DRAWABLE
    var storageUri: String? = null, // used when type == PHONE_STORAGE (content:// uri as string)
    var url: String? = null,        // used when type == URL
    var selected: Boolean = false
) {
    enum class Type { DRAWABLE, PHONE_STORAGE, URL }
}