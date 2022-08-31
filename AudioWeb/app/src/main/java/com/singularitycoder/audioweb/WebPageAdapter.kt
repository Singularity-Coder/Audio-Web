package com.singularitycoder.audioweb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.singularitycoder.audioweb.databinding.ListItemWebPageBinding

class WebPageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var webPageList = listOf<WebPage>()
    private var webPageClickListener: (webPage: WebPage, isPlaying: Boolean) -> Unit = { webPage, isPlaying -> }
    private var playingPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ListItemWebPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebPageViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as WebPageViewHolder).setData(webPageList[position])
    }

    override fun getItemCount(): Int = webPageList.size

    override fun getItemViewType(position: Int): Int = position

    fun setWebPageClickListener(listener: (webPage: WebPage, isPlaying: Boolean) -> Unit) {
        webPageClickListener = listener
    }

    inner class WebPageViewHolder(
        private val itemBinding: ListItemWebPageBinding,
    ) : RecyclerView.ViewHolder(itemBinding.root) {
        fun setData(webPage: WebPage) {
            itemBinding.apply {
                if (webPage.pageUrl.contains("https://")) {
                    tvSource.text = webPage.pageUrl.substringAfterLast("https://").substringAfter(".")
                } else {
                    tvSource.text = webPage.pageUrl.substringAfterLast("http://").substringAfter(".")
                }
                tvTitle.text = webPage.title
                ivImage.load(webPage.imageUrl) {
                    placeholder(R.drawable.ic_placeholder)
                }
                ivPlay.setImageDrawable(root.context.drawable(R.drawable.ic_round_play_arrow_24))
                root.setOnClickListener {
                    if (playingPosition != -1 && playingPosition != bindingAdapterPosition) {
                        notifyItemChanged(playingPosition)
                    }
                    playingPosition = bindingAdapterPosition
                    ivPlay.setImageDrawable(root.context.drawable(R.drawable.ic_round_stop_24))
                    webPageClickListener.invoke(webPage, true)
                }
            }
        }
    }
}
