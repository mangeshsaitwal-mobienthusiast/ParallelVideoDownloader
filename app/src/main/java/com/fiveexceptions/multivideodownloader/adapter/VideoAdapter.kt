package com.fiveexceptions.multivideodownloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fiveexceptions.multivideodownloader.databinding.VideoItemBinding
import com.fiveexceptions.multivideodownloader.model.VideoItem
import com.fiveexceptions.multivideodownloader.utils.Constants
import com.google.android.material.progressindicator.CircularProgressIndicator

class VideoAdapter(/*private val dataSet: List<VideoItem>*/) :
    //RecyclerView.Adapter<VideoAdapter.ViewHolder>(Diff()) {
    ListAdapter<VideoItem, VideoAdapter.ViewHolder>(Diff()) {


    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    //class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener?) {
        this.listener = listener
    }
    interface OnItemClickListener {
        fun onItemClick(item: VideoItem) // Or pass the data object: void onItemClick(MyItem item);
    }



    class ViewHolder(val binding: VideoItemBinding) : RecyclerView.ViewHolder(binding.root) {


        /*val textView: TextView

        init {
            // Define click listener for the ViewHolder's View
            textView = view.findViewById(R.id.textView)
        }*/
        val title: TextView = binding.title
        val status: TextView = binding.status
        val progress: CircularProgressIndicator = binding.progress
        val thumbProgress: CircularProgressIndicator = binding.thumbProgress
        val ivPlay : ImageView = binding.ivPlay
        val videoView : ImageView = binding.video

        //imageView.setImageBitmap(thumbnail)


        fun bind(item: VideoItem,listener: OnItemClickListener) {

            itemView.setOnClickListener {
                listener.onItemClick(item)
            }
            title.text = item.name
            /*
            progressBar.progress = item.progress*/
            when (item.status) {
                Constants.STATUS_DOWNLOADING -> {
                    status.text = "Downloading - ${item.progress?:0}%"
                    progress.progress = item.progress?:0
                    ivPlay.visibility = View.GONE
                    progress.visibility = View.VISIBLE
                }
                Constants.STATUS_PENDING -> {
                    status.text = "Pending"
                    //progress.progress = item.progress?:0
                    progress.visibility = View.VISIBLE
                    ivPlay.visibility = View.VISIBLE
                    ivPlay.setImageResource(android.R.drawable.stat_sys_download)
                }
                Constants.STATUS_DOWNLOADED -> {
                    status.text = "Downloaded"
                    progress.visibility = View.GONE
                    ivPlay.visibility = View.VISIBLE
                    ivPlay.setImageResource(android.R.drawable.ic_media_play);
                }
                Constants.STATUS_FAILED -> {
                status.text = "Failed"
                progress.visibility = View.GONE
                ivPlay.visibility = View.VISIBLE
                ivPlay.setImageResource(android.R.drawable.stat_sys_download)
                }
                else ->{
                    ivPlay.visibility = View.GONE
                    status.text = "Undefined"
                }
            }


            ivPlay.setOnClickListener {
                when (item.status) {
                    Constants.STATUS_DOWNLOADING -> {

                    }
                    Constants.STATUS_PENDING -> {

                    }
                    Constants.STATUS_DOWNLOADED -> {

                    }
                    Constants.STATUS_FAILED -> {

                    }
                    else ->{
                        return@setOnClickListener
                    }
                }
            }

            Glide.with(binding.root.context)
                .asBitmap()
                .load(item.url)        // video URL
                .frame(1_000_000)      // capture frame at 1 second
                .into(videoView)

            /*item.thumbNail?.let {
                videoView.setImageBitmap(it)
                thumbProgress.visibility = View.GONE
            }?:run{
                thumbProgress.visibility = View.GONE
            }*/
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = VideoItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }
    // Create new views (invoked by the layout manager)
    /*override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.video_item, viewGroup, false)

        return ViewHolder(view)
    }*/

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position),listener!!)
    }

    /*override*/ fun onBindViewHolder1(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        //holder.bind(getItem(position))
        if (payloads.isNotEmpty()) {
            // Handle partial updates based on the payload information
            // For example, update only a specific TextView or ImageView
            val payload = payloads[0] // Assuming a single payload for simplicity
            holder.progress.progress = payload.let { if(it is Int)it else if((it is String) && it.length<=3) it.toInt() else 0 }
            // ... apply partial update using 'payload' ...
        } else {
            // Perform a full bind if no payload is provided (e.g., initial bind or full refresh)
            holder.bind(getItem(position),listener!!) // Or implement full bind logic
        }
    }


    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }
    // Replace the contents of a view (invoked by the layout manager)
    /*override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.textView.text = dataSet[position]
    }*/

    // Return the size of your dataset (invoked by the layout manager)
    //override fun getItemCount() = dataSet.size

    class Diff : DiffUtil.ItemCallback<VideoItem>() {
        override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
            return oldItem == newItem
        }
        override fun getChangePayload(oldItem: VideoItem, newItem: VideoItem): Any? {
            if (oldItem.progress != newItem.progress) {
                return "progress"
            }
            return super.getChangePayload(oldItem, newItem)
        }
    }


}
