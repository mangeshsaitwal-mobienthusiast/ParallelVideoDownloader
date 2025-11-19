package com.fiveexceptions.multivideodownloader

/*import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController*/
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fiveexceptions.multivideodownloader.adapter.VideoAdapter
import com.fiveexceptions.multivideodownloader.databinding.ActivityMainBinding
import com.fiveexceptions.multivideodownloader.model.VideoItem
import com.fiveexceptions.multivideodownloader.utils.Constants
import com.fiveexceptions.multivideodownloader.viewmodel.VideoViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class MainActivity : AppCompatActivity(),VideoAdapter.OnItemClickListener{

    //private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    private val viewModel: VideoViewModel by viewModels()
    private val played = mutableSetOf<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        /*val str = readJsonFromAssets(this,"video_list.json")
        Log.e("VideoList",str+"__")
        val list = parseJsonToModel(str)
        Log.e("ListSize","${list.size}")*/
        //setSupportActionBar(binding.toolbar.toolbar)

        val adapter = VideoAdapter()
        val layoutManager = LinearLayoutManager(this) // 'this' refers to the Context (e.g., your Activity)
        binding.rvVideos.setLayoutManager(layoutManager)
        val dividerItemDecoration = DividerItemDecoration(
            this,
            layoutManager.orientation // Use the orientation of your LinearLayoutManager
        )
// Add the decoration to the RecyclerView
        binding.rvVideos.addItemDecoration(dividerItemDecoration)
        adapter.setHasStableIds(true)
        binding.rvVideos.adapter = adapter
        adapter.setOnItemClickListener(this)
        viewModel.prepare(this)

        lifecycleScope.launch {
            viewModel.items.collect { list ->
                adapter.submitList(list)
                //downloadThumbnail(list,adapter)

                val completed = list.firstOrNull { it.status == Constants.STATUS_DOWNLOADED && it.file != null && !played.contains(it.id) }
                Log.e("Completed Id","${completed?.id}")
                completed?.let { item ->

                    playFile(item)
                    /*if (exoPlayer == null || exoPlayer?.currentMediaItem == null ||
                        exoPlayer?.currentMediaItem?.localConfiguration?.uri != uri) {
                        playFile(uri)
                    }*/
                }
            }
        }

        //initPlayer()

        viewModel.startDownloadingOrPlaying()


        /*val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)*/

        binding.fab.setOnClickListener { view ->
            preparePlaylist()
            binding.fab.visibility = View.GONE
            Toast.makeText(this,"Autoplay enabled",Toast.LENGTH_SHORT).show()
        }
    }

    fun preparePlaylist(){

        played.clear()
        lifecycleScope.launch {


            viewModel.items.collect { list ->

                list.forEach {
                    if(it.status == Constants.STATUS_DOWNLOADED && it.file != null && !played.contains(it.id)){
                        playFile(it)
                    }
                }
                /*val completed = list.firstOrNull { it.status == Constants.STATUS_DOWNLOADED && it.file != null && !played.contains(it.id) }
                Log.e("Completed Id","${completed?.id}")
                completed?.let { item ->

                    playFile(item)
                }*/
            }
        }
    }
    fun downloadThumbnail(list:List<VideoItem>,adapter:VideoAdapter){
        lifecycleScope.launch(Dispatchers.IO) {

            list.forEachIndexed { index, videoItem ->
                if(videoItem.thumbNail==null){
                    val thumbnail = fetchVideoThumbnail(videoItem.url)

                    withContext(Dispatchers.Main) {
                        //imageView.setImageBitmap(thumbnail)
                        videoItem.thumbNail = thumbnail
                        adapter.notifyItemChanged(index)
                    }
                }

            }
            /*list.forEach {

            }*/

        }
    }
    fun readJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    fun parseJsonToModel(jsonString: String): List<VideoItem> {
        val gson = Gson()
        return gson.fromJson(jsonString, object : TypeToken<List<VideoItem>>() {}.type)
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this).build()
        // create a PlayerView in code and add to container (or use layout xml)
        val playerView = PlayerView(this)
        playerView.player = exoPlayer
        playerView.useController = true
        binding.container.removeAllViews()

        binding.container.addView(playerView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        binding.container.visibility = View.VISIBLE
    }

    fun checkFileExists(id:Int): Boolean{
        val targetFile = File(this.filesDir, "video_${id}.mp4")
        if(targetFile.exists()){
            return true
        }
        return false
    }
    private fun playFile(item: VideoItem,forcePlay:Boolean =  false) {

        if(item.file==null){
            Log.e("Clicked Video","is Null")
            return
        }
        val uri = Uri.fromFile(item.file)
        exoPlayer ?: initPlayer()
        exoPlayer?.let { player ->

            /*if(player.isPlaying){
                player.stop()
                player.prepare()
            }*/
            val mediaItem = MediaItem.fromUri(uri)

            //player.setMediaItem(mediaItem)
            player.currentMediaItem?.let {
                Log.e("CurrentMediaItem","${it.mediaId}  ${it.requestMetadata.mediaUri}")
            }
            /*player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        if (isPlaying) {
                            // Active playback.
                            //adapter.submitList(list)
                        } else {
                            // Not playing because playback is paused, ended, suppressed, or the player
                            // is buffering, stopped or failed. Check player.playWhenReady,
                            // player.playbackState, player.playbackSuppressionReason and
                            // player.playerError for details.
                        }
                    }
                }
            )*/

//            player.remo
            if(!player.isPlaying){
                player.prepare()
                player.playWhenReady = true
                //player.addMediaItem(mediaItem)
            }
            if(forcePlay){
                /*player.stop()
                player.prepare()
                player.seekToNextMediaItem()*/

                //player.addMediaItem(mediaItem)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                binding.fab.visibility = View.VISIBLE
            }
            else{
                player.addMediaItem(mediaItem)
            }
            played.add(item.id)
            //viewModel.updateItemStatus(item.id) { it.copy(status = Constants.STATUS_FAILED) }

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

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onSupportNavigateUp(): Boolean {
        /*val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()*/
        return false
    }

    override fun onItemClick(item: VideoItem) {
        Log.e("onItemClick","${item.name} ${item.status}")
        item.status?.let{
            if(it==Constants.STATUS_DOWNLOADED){
                playFile(item, forcePlay = true)
                //Toast.makeText(this,"Autoplay disabled",Toast.LENGTH_SHORT).show()
            }
            else if(it==Constants.STATUS_FAILED){
                viewModel.startDownloadingOrPlaying()
            }
        }

    }
}