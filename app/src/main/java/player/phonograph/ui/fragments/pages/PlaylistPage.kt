/*
 *  Copyright (c) 2023 chr_56
 */

package player.phonograph.ui.fragments.pages

import mt.pref.accentColor
import mt.pref.primaryColor
import mt.util.color.lightenColor
import player.phonograph.App
import player.phonograph.BROADCAST_PLAYLISTS_CHANGED
import player.phonograph.BuildConfig.DEBUG
import player.phonograph.R
import player.phonograph.adapter.display.DisplayAdapter
import player.phonograph.adapter.display.PlaylistDisplayAdapter
import player.phonograph.mediastore.PlaylistLoader
import player.phonograph.misc.PlaylistsModifiedReceiver
import player.phonograph.model.playlist.FavoriteSongsPlaylist
import player.phonograph.model.playlist.HistoryPlaylist
import player.phonograph.model.playlist.LastAddedPlaylist
import player.phonograph.model.playlist.MyTopTracksPlaylist
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.sort.SortMode
import player.phonograph.model.sort.SortRef
import player.phonograph.provider.FavoritesStore
import player.phonograph.settings.Setting
import player.phonograph.ui.components.popup.ListOptionsPopup
import player.phonograph.ui.dialogs.CreatePlaylistDialog
import player.phonograph.ui.fragments.pages.util.DisplayConfig
import player.phonograph.ui.fragments.pages.util.DisplayConfigTarget
import androidx.fragment.app.viewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.coroutines.CoroutineScope

class PlaylistPage : AbsDisplayPage<Playlist, DisplayAdapter<Playlist>, GridLayoutManager>() {

    override val viewModel: AbsDisplayPageViewModel<Playlist> get() = _viewModel

    private val _viewModel: PlaylistPageViewModel by viewModels()

    class PlaylistPageViewModel : AbsDisplayPageViewModel<Playlist>() {
        override suspend fun loadDataSetImpl(context: Context, scope: CoroutineScope): Collection<Playlist> {
            return mutableListOf<Playlist>(
                LastAddedPlaylist(context),
                HistoryPlaylist(context),
                MyTopTracksPlaylist(context),
            ).also {
                if (!Setting.instance.useLegacyFavoritePlaylistImpl) it.add(FavoriteSongsPlaylist(context))
            }.also {
                val allPlaylist = PlaylistLoader.allPlaylists(context)
                val (pined, normal) = allPlaylist.partition {
                    FavoritesStore.instance.containsPlaylist(it.id, it.associatedFilePath)
                }
                it.addAll(pined)
                it.addAll(normal)
            }
        }

        override val headerTextRes: Int get() = R.plurals.item_playlists
    }

    // private _viewModel:

    //region MediaStore & FloatingActionButton

    private lateinit var playlistsModifiedReceiver: PlaylistsModifiedReceiver
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // PlaylistsModifiedReceiver
        playlistsModifiedReceiver = PlaylistsModifiedReceiver(this::refreshDataSet)
        LocalBroadcastManager.getInstance(App.instance).registerReceiver(
            playlistsModifiedReceiver,
            IntentFilter().also { it.addAction(BROADCAST_PLAYLISTS_CHANGED) }
        )
        // AddNewItemButton
        setUpFloatingActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LocalBroadcastManager.getInstance(App.instance).unregisterReceiver(playlistsModifiedReceiver)
    }
    //endregion

    override val displayConfigTarget: DisplayConfigTarget get() = DisplayConfigTarget.PlaylistPage

    override fun initLayoutManager(): GridLayoutManager {
        return GridLayoutManager(hostFragment.requireContext(), 1)
            .also { it.spanCount = DisplayConfig(displayConfigTarget).gridSize }
    }

    override fun initAdapter(): DisplayAdapter<Playlist> {
        return PlaylistDisplayAdapter(
            hostFragment.mainActivity,
            hostFragment.cabController,
        ) {
            showSectionName = true
        }
    }


    override fun updateDataset(dataSet: List<Playlist>) {
        adapter.dataset = dataSet
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun refreshDataSet() {
        adapter.notifyDataSetChanged()
    }

    override fun setupSortOrderImpl(displayConfig: DisplayConfig, popup: ListOptionsPopup) {
        val currentSortMode = displayConfig.sortMode
        if (DEBUG) Log.d(TAG, "Read cfg: sortMode $currentSortMode")

        popup.maxGridSize = 0
        popup.allowRevert = true
        popup.revert = currentSortMode.revert

        popup.sortRef = currentSortMode.sortRef
        popup.sortRefAvailable = arrayOf(SortRef.DISPLAY_NAME, SortRef.PATH, SortRef.ADDED_DATE, SortRef.MODIFIED_DATE)
    }

    override fun saveSortOrderImpl(displayConfig: DisplayConfig, popup: ListOptionsPopup) {
        val selected = SortMode(popup.sortRef, popup.revert)
        if (displayConfig.sortMode != selected) {
            displayConfig.sortMode = selected
            viewModel.loadDataset(requireContext())
            Log.d(TAG, "Write cfg: sortMode $selected")
        }
    }

    private fun setUpFloatingActionButton() {
        val primaryColor = addNewItemButton.context.primaryColor()
        val accentColor = addNewItemButton.context.accentColor()
        addNewItemButton.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_activated),
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf(),
            ),
            intArrayOf(
                lightenColor(primaryColor), accentColor, primaryColor
            )
        )
        addNewItemButton.visibility = View.VISIBLE
        addNewItemButton.setOnClickListener {
            CreatePlaylistDialog.create(null).show(childFragmentManager, "CREATE_NEW_PLAYLIST")
        }
    }

    companion object {
        const val TAG = "PlaylistPage"
    }
}