package lor.and.company.driver;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

public class FullProgressBar extends View {
    int width, height, progress;
    Canvas canvas;
    Paint paint;
    Context context;

    public FullProgressBar(Context context) {
        super(context);
        this.context = context;
    }

    public FullProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        canvas = new Canvas();
        paint = new Paint();
        paint.setColor(Color.BLUE);
    }

    public FullProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public FullProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
    }

    void setProgress(int progress) {
        this.progress = progress;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0f,0f, getWidth()/100f * progress, getHeight(), paint);
    }
}
