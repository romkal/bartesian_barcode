package us.romkal.barcode

import kotlinx.serialization.Serializable

@Serializable
object Camera
@Serializable
data class Details(val barcode: Int, val file: String?)