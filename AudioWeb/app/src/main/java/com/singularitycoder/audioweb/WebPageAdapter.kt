package com.singularitycoder.audioweb

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.singularitycoder.audioweb.databinding.ListItemWebPageBinding

class WebPageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    val webPageList = ArrayList<WebPage>()
    private var webPageClickListener: (webPage: WebPage) -> Unit = {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemBinding = ListItemWebPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WebPageViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as WebPageViewHolder).setData(webPageList[position])
    }

    override fun getItemCount(): Int = webPageList.size

    override fun getItemViewType(position: Int): Int = position

    fun setWebPageClickListener(listener: (webPage: WebPage) -> Unit) {
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
                    placeholder(R.drawable.placeholder)
                }
                root.setOnClickListener {
                    webPageClickListener.invoke(webPage)
                }
            }
        }
    }
}
