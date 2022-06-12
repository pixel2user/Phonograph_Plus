/*
 * Copyright (c) 2022 chr_56 & Abou Zeid (kabouzeid) (original author)
 */

package player.phonograph.adapter.display

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView
import player.phonograph.R
import player.phonograph.adapter.base.MediaEntryViewClickListener
import player.phonograph.adapter.base.MultiSelectAdapter
import player.phonograph.adapter.base.UniversalMediaEntryViewHolder
import player.phonograph.interfaces.Displayable
import player.phonograph.interfaces.MultiSelectionCabProvider
import player.phonograph.model.Album
import player.phonograph.model.Artist
import player.phonograph.model.Song
import player.phonograph.util.MusicUtil
import util.mdcolor.ColorUtil
import util.mddesign.util.MaterialColorHelper

open class DisplayAdapter<I : Displayable>(
    protected val activity: AppCompatActivity,
    host: MultiSelectionCabProvider?,
    dataSet: List<I>,
    @LayoutRes var layoutRes: Int,
    cfg: (DisplayAdapter<I>.() -> Unit)?
) :
    MultiSelectAdapter<DisplayAdapter<I>.DisplayViewHolder, I>(activity, host), FastScrollRecyclerView.SectionedAdapter {

    var dataset: List<I> = dataSet
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    init {
        setHasStableIds(true)
        cfg?.invoke(this)
    }

    var usePalette: Boolean = false

    var useImageText: Boolean = false

    var showDuration: Boolean = false

    var showSectionName: Boolean = true

    override var multiSelectMenuRes: Int = R.menu.menu_media_selection

    override fun getItemId(position: Int): Long =
        dataset[position].getItemID()

    override fun getItem(datasetPosition: Int): I {
        return dataset[datasetPosition]
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
        return DisplayViewHolder(
            LayoutInflater.from(activity).inflate(layoutRes, parent, false)
        )
    }

    private val defaultIcon = when (dataset.getOrNull(0)) {
        is Artist -> AppCompatResources.getDrawable(activity, R.drawable.default_artist_image)
        else -> AppCompatResources.getDrawable(activity, R.drawable.default_album_art)
    }

    override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
        val item: I = dataset[position]
        holder.shortSeparator?.visibility = View.VISIBLE
        holder.itemView.isActivated = isChecked(item)
        holder.title?.text = item.getDisplayTitle()
        holder.text?.text = if (showDuration) {
            // todo
            val song = (item as? Song) ?: Song.EMPTY_SONG
            "${MusicUtil.getReadableDurationString(song.duration)} · ${song.artistName}"
        } else {
            item.getDescription()
        }
        if (useImageText) {
            setImageText(holder, getRelativeOrdinalText(item))
        } else {
            setImage(holder, position)
        }
    }

    protected open fun setImage(holder: DisplayViewHolder, position: Int) {
        holder.image?.also {
            it.visibility = View.VISIBLE
            it.setImageDrawable(defaultIcon)
        }
    }

    protected open fun setImageText(holder: DisplayViewHolder, text: String) {
        holder.imageText?.also {
            it.visibility = View.VISIBLE
            it.text = text
        }
    }

    private fun getRelativeOrdinalText(item: I): String =
        when (item) {
            is Song -> {
                val num = MusicUtil.getFixedTrackNumber(item.trackNumber)
                if (num > 0) num.toString() else "-"
            }
            is Album -> {
                item.songCount.toString()
            }
            is Artist -> {
                item.songCount.toString()
            }
            else -> "-"
        }

    override fun getItemCount(): Int = dataset.size

    override fun updateItemCheckStatusForAll() = notifyDataSetChanged()
    override fun updateItemCheckStatus(datasetPosition: Int) = notifyItemChanged(datasetPosition)

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<I>) {
        if (dataset.isNotEmpty()) dataset[0].multiMenuHandler()?.invoke(activity, selection, menuItem.itemId)
    }

    override fun getSectionName(position: Int): String = if (showSectionName) getSectionNameImp(position) else ""

    // for inheriting
    protected fun setPaletteColors(color: Int, holder: DisplayViewHolder) {
        holder.paletteColorContainer?.let { paletteColorContainer ->
            paletteColorContainer.setBackgroundColor(color)
            holder.title?.setTextColor(MaterialColorHelper.getPrimaryTextColor(activity, ColorUtil.isColorLight(color)))
            holder.text?.setTextColor(MaterialColorHelper.getSecondaryTextColor(activity, ColorUtil.isColorLight(color)))
        }
    }

    // for inheriting
    open fun getSectionNameImp(position: Int): String = dataset[position].getSortOrderReference()?.substring(0..1) ?: ""

    inner class DisplayViewHolder(itemView: View) : UniversalMediaEntryViewHolder(itemView), MediaEntryViewClickListener {

        init {
            // Item Click
            setClickListener(object : MediaEntryViewClickListener {
                override fun onLongClick(v: View): Boolean {
                    return toggleChecked(bindingAdapterPosition)
                }
                override fun onClick(v: View) {
                    when (isInQuickSelectMode) {
                        true -> toggleChecked(bindingAdapterPosition)
                        false -> dataset[0].clickHandler().invoke(activity, dataset[bindingAdapterPosition], dataset, image)
                    }
                }
            })
            // Menu Click
            if (dataset[0].menuRes() == 0 || dataset[0].menuHandler() == null) {
                menu?.visibility = View.GONE
            } else {
                menu?.setOnClickListener { view ->
                    if (dataset.isNotEmpty()) {
                        val menuRes = dataset[0].menuRes()
                        val popupMenu = PopupMenu(activity, view)
                        popupMenu.inflate(menuRes)
                        popupMenu.setOnMenuItemClickListener { menuItem ->
                            if (menuItem != null)
                                return@setOnMenuItemClickListener dataset[0].menuHandler()
                                    ?.invoke(activity, dataset[bindingAdapterPosition], menuItem.itemId) ?: false
                            else return@setOnMenuItemClickListener false
                        }
                        popupMenu.show()
                    }
                }
            }
        }
    }
}
