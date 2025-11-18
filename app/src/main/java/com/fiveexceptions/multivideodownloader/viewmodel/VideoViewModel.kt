package com.fiveexceptions.multivideodownloader.viewmodel



import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveexceptions.multivideodownloader.model.VideoItem
import com.fiveexceptions.multivideodownloader.utils.Constants
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Semaphore

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val _items = MutableStateFlow<List<VideoItem>>(emptyList())
    val items: StateFlow<List<VideoItem>> = _items

    private val client = OkHttpClient()
    private val semaphore = Semaphore(10) // limit parallel downloading to 10

    fun readJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun parseJsonToModel(jsonString: String): List<VideoItem> {
        val gson = Gson()
        return gson.fromJson(jsonString, object : TypeToken<List<VideoItem>>() {}.type)
    }
    fun prepare(context: Context) {

        val str = readJsonFromAssets(context,"video_list.json")
        Log.e("VideoList",str+"__")
        val list = parseJsonToModel(str)
        _items.value = list
    }

    fun startDownloading() {
        val current = _items.value
        current.forEach { item ->
            viewModelScope.launch(Dispatchers.IO) {
                if(!checkFileExists(item.id)){
                    downloadWithProgress(item)
                }
                else{
                    updateItemStatus(item.id) { it.copy(status = Constants.STATUS_DOWNLOADED, progress = 100) }
                }

            }
        }
    }

    fun checkFileExists(id:Int): Boolean{
        val targetFile = File(getApplication<Application>().filesDir, "video_${id}.mp4")
        if(targetFile.exists()){
            return true
        }
        return false
    }
    private fun downloadWithProgress(item: VideoItem) {
        semaphore.acquire()
        try {
            updateItemStatus(item.id) { it.copy(status = Constants.STATUS_DOWNLOADING, progress = 0) }

            val request = Request.Builder().url(item.url).get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                updateItemStatus(item.id) { it.copy(status = Constants.STATUS_FAILED) }
                response.close()
                return
            }

            val body = response.body
            if (body == null) {
                updateItemStatus(item.id) { it.copy(status = Constants.STATUS_FAILED) }
                return
            }

            val totalBytes = body.contentLength() // may be -1
            val input = body.byteStream()

            val targetFile = File(getApplication<Application>().filesDir, "video_${item.id}.mp4")
            val output = FileOutputStream(targetFile)

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Long = 0
            var read: Int
            try {
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    val progress = if (totalBytes > 0) {
                        ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
                    } else {
                        // when total unknown, we can approximate by capping progress to 99 until finished
                        ((bytesRead / 1024) % 100).toInt().coerceIn(0, 99)
                    }
                    //Log.e("PublishProgress","${item.id} $progress")
                    updateItemStatus(item.id) { it.copy(progress = progress) }
                }
                output.flush()
                // finished
                response.close()
                updateItemStatus(item.id) { it.copy(progress = 100, status = Constants.STATUS_DOWNLOADED,
                    file = targetFile) }
            } catch (e: Exception) {
                response.close()
                targetFile.delete()
                updateItemStatus(item.id) { it.copy(status = Constants.STATUS_FAILED) }
            } finally {
                try { input.close() } catch (e: Exception) {
                    Log.e("DownloadException","${e.printStackTrace()}")
                }
                try { output.close() } catch (e: Exception) {
                    Log.e("DownloadException","${e.printStackTrace()}")
                }
            }
        } finally {
            semaphore.release()
        }
    }

    fun fetchVideoThumbnail(videoUrl: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoUrl, HashMap())
            val bitmap = retriever.getFrameAtTime(0) // first frame
            retriever.release()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateItemStatus(id: Int, updater: (VideoItem) -> VideoItem) {

        val old = _items.value
        val new = old.map { if (it.id == id) updater(it) else it }
        _items.value = new

    }
}
