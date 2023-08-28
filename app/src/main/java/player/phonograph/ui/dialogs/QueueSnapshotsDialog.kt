/*
 * Copyright (c) 2022 chr_56
 */

package player.phonograph.ui.dialogs

import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.customView
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import com.vanpra.composematerialdialogs.title
import org.koin.android.ext.android.inject
import player.phonograph.R
import player.phonograph.service.queue.QueueManager
import player.phonograph.ui.compose.base.BridgeDialogFragment
import player.phonograph.ui.compose.dialogs.QueueSnapshotsDialogContent
import player.phonograph.ui.compose.theme.PhonographTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

class QueueSnapshotsDialog : BridgeDialogFragment() {

    private val queueManager: QueueManager by inject()

    @Composable
    override fun Content() {
        val dialogState = rememberMaterialDialogState(true)
        PhonographTheme {
            MaterialDialog(
                dialogState = dialogState,
                elevation = 0.dp,
                onCloseRequest = { dismiss() },
                buttons = {
                    button(res = android.R.string.ok) { dismiss() }
                }
            ) {
                title(res = R.string.playing_queue_history)
                customView {
                    QueueSnapshotsDialogContent(
                        requireContext(),
                        queueManager,
                        ::dismiss
                    )
                }
            }
        }
    }
}