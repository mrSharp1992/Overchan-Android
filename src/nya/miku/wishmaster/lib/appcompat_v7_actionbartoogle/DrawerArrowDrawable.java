/*
 * Класс из кода android v7 appcompat library
 * 
 */

package nya.miku.wishmaster.lib.appcompat_v7_actionbartoogle;
//package android.support.v7.app;


import android.content.Context;
//import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
//import android.support.v7.appcompat.R;
import nya.miku.wishmaster.ui.CompatibilityUtils;
import nya.miku.wishmaster.ui.theme.ThemeUtils;

/**
 * A drawable that can draw a "Drawer hamburger" menu or an Arrow and animate between them.
 */
abstract class DrawerArrowDrawable extends Drawable {

    private final Paint mPaint = new Paint();

    // The angle in degress that the arrow head is inclined at.
    private static final float ARROW_HEAD_ANGLE = (float) Math.toRadians(45);
    private final float mBarThickness;
    // The length of top and bottom bars when they merge into an arrow
    private final float mTopBottomArrowSize;
    // The length of middle bar
    private final float mBarSize;
    // The length of the middle bar when arrow is shaped
    private final float mMiddleArrowSize;
    // The space between bars when they are parallel
    private final float mBarGap;
    // Whether bars should spin or not during progress
    private final boolean mSpin;
    // Use Path instead of canvas operations so that if color has transparency, overlapping sections
    // wont look different
    private final Path mPath = new Path();
    // The reported intrinsic size of the drawable.
    private final int mSize;
    // Whether we should mirror animation when animation is reversed.
    private boolean mVerticalMirror = false;
    // The interpolated version of the original progress
    private float mProgress;

    /**
     * @param context used to get the configuration for the drawable from
     */
    DrawerArrowDrawable(Context context) {
        /*final TypedArray typedArray = context.getTheme()
                .obtainStyledAttributes(null, R.styleable.DrawerArrowToggle,
                        R.attr.drawerArrowStyle,
                        R.style.Base_Widget_AppCompat_DrawerArrowToggle);
        mPaint.setAntiAlias(true);
        mPaint.setColor(typedArray.getColor(R.styleable.DrawerArrowToggle_color, 0));
        mSize = typedArray.getDimensionPixelSize(R.styleable.DrawerArrowToggle_drawableSize, 0);
        mBarSize = typedArray.getDimension(R.styleable.DrawerArrowToggle_barSize, 0);
        mTopBottomArrowSize = typedArray
                .getDimension(R.styleable.DrawerArrowToggle_topBottomBarArrowSize, 0);
        mBarThickness = typedArray.getDimension(R.styleable.DrawerArrowToggle_thickness, 0);
        mBarGap = typedArray.getDimension(R.styleable.DrawerArrowToggle_gapBetweenBars, 0);
        mSpin = typedArray.getBoolean(R.styleable.DrawerArrowToggle_spinBars, true);
        mMiddleArrowSize = typedArray
                .getDimension(R.styleable.DrawerArrowToggle_middleBarArrowSize, 0);
        typedArray.recycle();*/
        
        TypedValue typedValue = ThemeUtils.resolveAttribute(context.getTheme(), android.R.attr.textColorPrimary, true);
        int color;
        if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            color = typedValue.data;
        } else {
            try {
                color = CompatibilityUtils.getColor(context.getResources(), typedValue.resourceId);
            } catch (Exception e) {
                color = 0;
            }
        }
        
        mPaint.setAntiAlias(true);
        mPaint.setColor(color);
        mSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24.0f, context.getResources().getDisplayMetrics());
        mBarSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18.0f, context.getResources().getDisplayMetrics());
        mTopBottomArrowSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11.31f, context.getResources().getDisplayMetrics());
        mBarThickness = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f, context.getResources().getDisplayMetrics());
        mBarGap = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3.0f, context.getResources().getDisplayMetrics());
        mSpin = true;
        mMiddleArrowSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16.0f, context.getResources().getDisplayMetrics());

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mPaint.setStrokeWidth(mBarThickness);
    }

    abstract boolean isLayoutRtl();

    /**
     * If set, canvas is flipped when progress reached to end and going back to start.
     */
    protected void setVerticalMirror(boolean verticalMirror) {
        mVerticalMirror = verticalMirror;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        final boolean isRtl = isLayoutRtl();
        // Interpolated widths of arrow bars
        final float arrowSize = lerp(mBarSize, mTopBottomArrowSize, mProgress);
        final float middleBarSize = lerp(mBarSize, mMiddleArrowSize, mProgress);
        // Interpolated size of middle bar
        final float middleBarCut = lerp(0, mBarThickness / 2, mProgress);
        // The rotation of the top and bottom bars (that make the arrow head)
        final float rotation = lerp(0, ARROW_HEAD_ANGLE, mProgress);

        // The whole canvas rotates as the transition happens
        final float canvasRotate = lerp(isRtl ? 0 : -180, isRtl ? 180 : 0, mProgress);
        final float topBottomBarOffset = lerp(mBarGap + mBarThickness, 0, mProgress);
        mPath.rewind();

        final float arrowEdge = -middleBarSize / 2;
        // draw middle bar
        mPath.moveTo(arrowEdge + middleBarCut, 0);
        mPath.rLineTo(middleBarSize - middleBarCut, 0);

        final float arrowWidth = Math.round(arrowSize * Math.cos(rotation));
        final float arrowHeight = Math.round(arrowSize * Math.sin(rotation));

        // top bar
        mPath.moveTo(arrowEdge, topBottomBarOffset);
        mPath.rLineTo(arrowWidth, arrowHeight);

        // bottom bar
        mPath.moveTo(arrowEdge, -topBottomBarOffset);
        mPath.rLineTo(arrowWidth, -arrowHeight);
        mPath.moveTo(0, 0);
        mPath.close();

        canvas.save();
        // Rotate the whole canvas if spinning, if not, rotate it 180 to get
        // the arrow pointing the other way for RTL.
        if (mSpin) {
            canvas.rotate(canvasRotate * ((mVerticalMirror ^ isRtl) ? -1 : 1),
                    bounds.centerX(), bounds.centerY());
        } else if (isRtl) {
            canvas.rotate(180, bounds.centerX(), bounds.centerY());
        }
        canvas.translate(bounds.centerX(), bounds.centerY());
        canvas.drawPath(mPath, mPaint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int i) {
        mPaint.setAlpha(i);
    }

    // override
    public boolean isAutoMirrored() {
        // Draws rotated 180 degrees in RTL mode.
        return true;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = progress;
        invalidateSelf();
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
