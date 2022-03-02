package player.phonograph.views;

import android.content.Context;
import android.graphics.PorterDuff;
import androidx.appcompat.widget.AppCompatImageView;
import android.util.AttributeSet;

import player.phonograph.R;

import util.mddesign.util.Util;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class IconImageView extends AppCompatImageView {
    public IconImageView(Context context) {
        super(context);
        init(context);
    }

    public IconImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public IconImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        if (context == null) return;
        setColorFilter(Util.resolveColor(context, R.attr.iconColor), PorterDuff.Mode.SRC_IN);
    }
}
