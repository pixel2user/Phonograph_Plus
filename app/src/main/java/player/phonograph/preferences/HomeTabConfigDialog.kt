/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.preferences

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.getActionButton
import com.afollestad.materialdialogs.customview.customView
import mt.pref.ThemeColor
import player.phonograph.R
import player.phonograph.adapter.HomeTabConfigAdapter
import player.phonograph.model.pages.PageConfig
import player.phonograph.settings.Setting

class HomeTabConfigDialog : DialogFragment() {
    private lateinit var adapter: HomeTabConfigAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.preference_dialog_home_tab_config, null)

//        Setting.instance.homeTabConfig = PageConfig.DEFAULT_CONFIG

        val config: PageConfig = Setting.instance.homeTabConfig

        adapter = HomeTabConfigAdapter(config)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter
        adapter.attachToRecyclerView(recyclerView)

        Log.v(TAG, adapter.getState())

        val dialog = MaterialDialog(requireContext())
            .title(R.string.library_categories)
            .customView(view = view, dialogWrapContent = false)
            .positiveButton(android.R.string.ok) {
                Setting.instance.homeTabConfig = adapter.currentConfig
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .negativeButton(android.R.string.cancel) { dismiss(); Log.i(TAG, adapter.getState()) }
            .neutralButton(R.string.reset_action) {
                Setting.instance.homeTabConfig = PageConfig.DEFAULT_CONFIG
                Log.v(TAG, adapter.getState())
                dismiss()
            }
            .apply {
                // set button color
                val color = ThemeColor.accentColor(requireActivity())
                getActionButton(WhichButton.POSITIVE).updateTextColor(color)
                getActionButton(WhichButton.NEGATIVE).updateTextColor(color)
                getActionButton(WhichButton.NEUTRAL).updateTextColor(color)
            }

        return dialog
    }

    companion object {
        private const val TAG = "HomeTabConfigDialog"
        fun newInstance(): HomeTabConfigDialog = HomeTabConfigDialog()
    }
}
