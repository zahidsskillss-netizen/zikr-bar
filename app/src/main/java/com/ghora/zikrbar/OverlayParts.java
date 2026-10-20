package com.ghora.zikrbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Small extra views used around the floating tasbih. */
final class OverlayParts {

    private OverlayParts() {}

    /** The small tab left on the screen edge when the tasbih is hidden. */
    static class EdgeTab extends View {
        private final float d;
        private final int side; // 1 = left, 2 = right
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        EdgeTab(Context c, int side) {
            super(c);
            this.side = side;
            d = c.getResources().getDisplayMetrics().density;
            p.setTypeface(Ui.amiriBold(c));
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension((int) (30 * d), (int) (76 * d));
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), r = 16 * d;
            RectF rect = new RectF(0, 2 * d, w, h - 2 * d);
            float[] radii = side == 1
                    ? new float[]{0, 0, r, r, r, r, 0, 0}
                    : new float[]{r, r, 0, 0, 0, 0, r, r};
            path.reset();
            path.addRoundRect(rect, radii, Path.Direction.CW);
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xE61B5E20);
            p.setShadowLayer(4 * d, 0, 2 * d, 0x44000000);
            c.drawPath(path, p);
            p.clearShadowLayer();

            p.setColor(0xFFF3E3A5);
            p.setTextSize(17 * d);
            p.setTextAlign(Paint.Align.CENTER);
            Paint.FontMetrics fm = p.getFontMetrics();
            c.drawText("ﷲ", w / 2, h / 2 - 8 * d - (fm.ascent + fm.descent) / 2, p);

            p.setColor(Color.WHITE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.2f * d);
            p.setStrokeCap(Paint.Cap.ROUND);
            float cx = w / 2, cy = h / 2 + 16 * d, a = 4 * d;
            if (side == 1) {
                c.drawLine(cx - a / 2, cy - a, cx + a / 2, cy, p);
                c.drawLine(cx + a / 2, cy, cx - a / 2, cy + a, p);
            } else {
                c.drawLine(cx + a / 2, cy - a, cx - a / 2, cy, p);
                c.drawLine(cx - a / 2, cy, cx + a / 2, cy + a, p);
            }
        }
    }

    /** The X target shown at the bottom while the tasbih is dragged. */
    static class CloseTarget extends View {
        private final float d;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean active;

        CloseTarget(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        void setActive(boolean a) {
            if (a == active) return;
            active = a;
            invalidate();
        }

        @Override
        protected void onMeasure(int w, int h) {
            setMeasuredDimension((int) (96 * d), (int) (96 * d));
        }

        @Override
        protected void onDraw(Canvas c) {
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            float r = (active ? 36 : 28) * d;
            p.setStyle(Paint.Style.FILL);
            p.setColor(active ? 0xF0C62828 : 0xCC1A1A1A);
            p.setShadowLayer(6 * d, 0, 3 * d, 0x55000000);
            c.drawCircle(cx, cy, r, p);
            p.clearShadowLayer();
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f * d);
            p.setColor(0x66FFFFFF);
            c.drawCircle(cx, cy, r, p);
            p.setColor(Color.WHITE);
            p.setStrokeWidth(3 * d);
            p.setStrokeCap(Paint.Cap.ROUND);
            float a = (active ? 11 : 9) * d;
            c.drawLine(cx - a, cy - a, cx + a, cy + a, p);
            c.drawLine(cx + a, cy - a, cx - a, cy + a, p);
        }
    }
}
