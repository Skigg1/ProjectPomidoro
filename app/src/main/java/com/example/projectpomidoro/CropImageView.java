package com.example.projectpomidoro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CropImageView extends androidx.appcompat.widget.AppCompatImageView {
    private Rect cropBounds = new Rect();

    public CropImageView(Context context) {
        super(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CropImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setCropBounds(int left, int top, int right, int bottom) {
        cropBounds.set(left, top, right, bottom);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!cropBounds.isEmpty()) {
            canvas.save();
            canvas.clipRect(cropBounds);
            super.onDraw(canvas);
            canvas.restore();
        } else {
            super.onDraw(canvas);
        }
    }
}
