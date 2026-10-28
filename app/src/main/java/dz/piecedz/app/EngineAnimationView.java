package dz.piecedz.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class EngineAnimationView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private float phase;
    private ValueAnimator animator;

    public EngineAnimationView(Context c) {
        super(c);
        init();
    }

    public EngineAnimationView(Context c, AttributeSet a) {
        super(c, a);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1800);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
    }

    private void paint(Canvas c, int color, Paint.Style style, float width) {
        p.setColor(color);
        p.setStyle(style);
        p.setStrokeWidth(width);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;

        canvas.save();
        canvas.scale(w / 320f, h / 190f);

        paint(canvas, 0xFF10233F, Paint.Style.FILL, 1);
        r.set(4, 4, 316, 186);
        canvas.drawRoundRect(r, 22, 22, p);

        paint(canvas, 0xFF213B5D, Paint.Style.STROKE, 1.5f);
        r.set(12, 12, 308, 178);
        canvas.drawRoundRect(r, 17, 17, p);

        // Engine block
        paint(canvas, 0xFFCED7E2, Paint.Style.FILL, 1);
        r.set(86, 48, 211, 139);
        canvas.drawRoundRect(r, 12, 12, p);

        paint(canvas, 0xFF647A91, Paint.Style.STROKE, 3);
        canvas.drawRoundRect(r, 12, 12, p);

        // Cylinders
        for (int i = 0; i < 4; i++) {
            float x = 101 + i * 25;
            paint(canvas, 0xFF10233F, Paint.Style.FILL, 1);
            r.set(x, 58, x + 17, 88);
            canvas.drawRoundRect(r, 4, 4, p);

            paint(canvas, 0xFFFF8A24, Paint.Style.FILL, 1);
            float pistonY = 64 + (float)Math.sin((phase * Math.PI * 2) + i * 1.3) * 7;
            r.set(x + 2, pistonY, x + 15, pistonY + 9);
            canvas.drawRoundRect(r, 3, 3, p);

            paint(canvas, 0xFFFFB15C, Paint.Style.STROKE, 2);
            canvas.drawLine(x + 8.5f, pistonY + 9, x + 8.5f, 101, p);
        }

        // Moving crankshaft
        paint(canvas, 0xFF10233F, Paint.Style.STROKE, 5);
        Path shaft = new Path();
        shaft.moveTo(94, 111);
        shaft.lineTo(201, 111);
        canvas.drawPath(shaft, p);

        for (int i = 0; i < 4; i++) {
            float x = 109 + i * 25;
            float y = 111 + (float)Math.sin(phase * Math.PI * 2 + i * 1.3) * 5;
            paint(canvas, 0xFFFF8A24, Paint.Style.FILL, 1);
            canvas.drawCircle(x, y, 5, p);
        }

        // Rotating flywheel
        canvas.save();
        canvas.rotate(phase * 360, 238, 104);
        paint(canvas, 0xFFFF8A24, Paint.Style.STROKE, 5);
        canvas.drawCircle(238, 104, 25, p);
        paint(canvas, 0xFFCED7E2, Paint.Style.STROKE, 3);
        canvas.drawCircle(238, 104, 14, p);
        for (int i = 0; i < 8; i++) {
            canvas.save();
            canvas.rotate(i * 45, 238, 104);
            canvas.drawLine(238, 83, 238, 91, p);
            canvas.restore();
        }
        canvas.restore();

        // Animated status indicator
        int alpha = 100 + (int)(155 * (0.5f + 0.5f *
                (float)Math.sin(phase * Math.PI * 2)));
        paint(canvas, (alpha << 24) | 0x00FF8A24, Paint.Style.FILL, 1);
        canvas.drawCircle(35, 35, 7, p);

        paint(canvas, 0xFFFFFFFF, Paint.Style.FILL, 1);
        p.setTextSize(10);
        p.setFakeBoldText(true);
        canvas.drawText("PIECEDZ  /  ENGINE LAB", 50, 39, p);
        p.setFakeBoldText(false);

        paint(canvas, 0xFFFFB15C, Paint.Style.FILL, 1);
        p.setTextSize(9);
        canvas.drawText("MOTEUR EN MOUVEMENT", 20, 163, p);

        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (animator != null && !animator.isStarted()) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
