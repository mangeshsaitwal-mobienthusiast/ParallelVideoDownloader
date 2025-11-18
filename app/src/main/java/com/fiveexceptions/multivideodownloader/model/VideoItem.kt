package com.fiveexceptions.multivideodownloader.model

import android.graphics.Bitmap
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class VideoItem(val id: Int, val name: String, val url: String,val status:Int?=null,val progress:Int?=null,@Contextual var thumbNail:Bitmap?,@Contextual var file: File?,val playStatus:Int?=null)


