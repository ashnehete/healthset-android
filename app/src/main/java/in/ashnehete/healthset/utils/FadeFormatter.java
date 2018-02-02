package in.ashnehete.healthset.utils;

import android.graphics.Paint;

import com.androidplot.xy.AdvancedLineAndPointRenderer;

/**
 * Created by Aashish Nehete on 23-Jan-18.
 */

public class FadeFormatter extends AdvancedLineAndPointRenderer.Formatter {
    private int trailSize;

    public FadeFormatter(int trailSize) {
        this.trailSize = trailSize;
    }

    @Override
    public Paint getLinePaint(int thisIndex, int latestIndex, int seriesSize) {
        // offset from the latest index:
        int offset;
        if (thisIndex > latestIndex) {
            offset = latestIndex + (seriesSize - thisIndex);
        } else {
            offset = latestIndex - thisIndex;
        }
        float scale = 255f / trailSize;
        int alpha = (int) (255 - (offset * scale));
        getLinePaint().setAlpha(alpha > 0 ? alpha : 0);
        return getLinePaint();
    }
}
