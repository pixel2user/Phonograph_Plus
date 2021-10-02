package com.kabouzeid.gramophone.adapter.song

import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.draggable.annotation.DraggableItemStateFlags
import com.kabouzeid.gramophone.R
import com.kabouzeid.gramophone.dialogs.RemoveFromPlaylistDialog
import com.kabouzeid.gramophone.dialogs.RemoveFromPlaylistDialog.Companion.create
import com.kabouzeid.gramophone.interfaces.CabHolder
import com.kabouzeid.gramophone.model.PlaylistSong
import com.kabouzeid.gramophone.model.Song
import com.kabouzeid.gramophone.util.ViewUtil
import java.util.*


//Todo: full of errors/crashes
/**
 * @author Karim Abou Zeid (kabouzeid)
 */
class OrderablePlaylistSongAdapter(
    activity: AppCompatActivity,
    dataSet: List<PlaylistSong>,
    @LayoutRes itemLayoutRes: Int,
    usePalette: Boolean,
    cabHolder: CabHolder?,
    val onMoveItemListener: OnMoveItemListener?
) : PlaylistSongAdapter(activity, dataSet as List<Song>, itemLayoutRes, usePalette, cabHolder),
    DraggableItemAdapter<OrderablePlaylistSongAdapter.ViewHolder> {

    init {
        setMultiSelectMenuRes(R.menu.menu_playlists_songs_selection)
    }

    override fun createViewHolder(view: View): SongAdapter.ViewHolder {
        return ViewHolder(view)
    }

    override fun getItemId(position: Int): Long {
        var position = position
        position--
        return if (position < 0) -2 else (dataSet as List<PlaylistSong>)[position].idInPlayList
        // important!
    }

    override fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song?>) {
        when (menuItem.itemId) {
            R.id.action_remove_from_playlist -> {
                RemoveFromPlaylistDialog.create(selection as List<PlaylistSong>?).show(
                    activity.supportFragmentManager,
                    "ADD_PLAYLIST"
                )
                return
            }
        }
        super.onMultipleItemAction(menuItem, selection)
    }

    override fun onCheckCanStartDrag(holder: ViewHolder, position: Int, x: Int, y: Int): Boolean {
        return onMoveItemListener != null && position > 0 &&
            (ViewUtil.hitTest(holder.dragView, x, y) || ViewUtil.hitTest(holder.image, x, y))
    }

    override fun onGetItemDraggableRange(holder: ViewHolder, position: Int): ItemDraggableRange {
        return ItemDraggableRange(1, dataSet.size)
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        if (onMoveItemListener != null && fromPosition != toPosition) {
            onMoveItemListener.onMoveItem(fromPosition - 1, toPosition - 1)
        }
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return dropPosition > 0
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    interface OnMoveItemListener {
        fun onMoveItem(fromPosition: Int, toPosition: Int)
    }

    inner class ViewHolder(itemView: View) :
        PlaylistSongAdapter.ViewHolder(itemView),
        DraggableItemViewHolder {

        override val songMenuRes: Int
            get() = R.menu.menu_item_playlist_song

        init {
            dragView?.let {
                if (onMoveItemListener != null) it.visibility = View.VISIBLE
                else it.visibility = View.GONE
            }
        }

        @DraggableItemStateFlags
        private var mDragStateFlags = 0


        override fun onSongMenuItemClick(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.action_remove_from_playlist -> {
                    val l: MutableList<PlaylistSong> = ArrayList(1)
                    l.add(song as PlaylistSong)
                    create(l).show(activity.supportFragmentManager, "REMOVE_FROM_PLAYLIST")
                    return true
                }
            }
            return super.onSongMenuItemClick(item)
        }

        override fun setDragStateFlags(@DraggableItemStateFlags flags: Int) {
            mDragStateFlags = flags
        }

        @DraggableItemStateFlags
        override fun getDragStateFlags(): Int {
            return mDragStateFlags
        }
    }
}
