/*
 *  Copyright (c) 2022~2023 chr_56
 */

package player.phonograph.ui.components.popup

import mt.util.color.resolveColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.PopupWindow

class ComposePopup private constructor(
    rootView: View,
) : PopupWindow(rootView, WRAP_CONTENT, WRAP_CONTENT, true) {

    init {
        animationStyle = android.R.style.Animation_Dialog
        super.setBackgroundDrawable(ColorDrawable(backgroundColor(rootView.context)))
    }

    fun backgroundColor(context: Context): Int =
        resolveColor(
            context,
            androidx.appcompat.R.attr.colorBackgroundFloating,
            context.getColor(player.phonograph.R.color.cardBackgroundColor)
        )


    companion object {

        fun empty(context: Context): ComposePopup {
            val view = ComposeView(context)
            return ComposePopup(view)
        }

        fun content(context: Context, content: @Composable () -> Unit): ComposePopup {
            val view = ComposeView(context)
            return ComposePopup(view).also { view.setContent(content) }
        }
    }
}