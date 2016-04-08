package edu.umd.hcil.impressionistpainter434;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jon on 3/20/2016.
 */
public class ImpressionistView extends View {

    //get pixel from left pic (see code at bottom of spec), use Color.red(pix), etc to get each rgb val


    private ImageView _imageView;

    private Canvas _offScreenCanvas = null;
    private Bitmap _offScreenBitmap = null;
    private Paint _paint = new Paint();

    private int savedNum = 0; //gives unique identifiers to images for saving
    private int _alpha = 180;
    private int _defaultRadius = 25;
    private Point _lastPoint = null;
    private long _lastPointTime = -1;
    private boolean _useMotionSpeedForBrushStrokeSize = true;
    private VelocityTracker velocityTracker = null;
    private Paint _paintBorder = new Paint();
    private BrushType _brushType = BrushType.Square;
    private float _minBrushRadius = 5;
    private int symmetry = 0; //0 for none, 1 for left/right, 2 for top/bottom, 3 for radial

    //private ArrayList<PaintPoint> _listPaintPoints = new ArrayList<PaintPoint>();


    public ImpressionistView(Context context) {
        super(context);
        init(null, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ImpressionistView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    /**
     * Because we have more than one constructor (i.e., overloaded constructors), we use
     * a separate initialization method
     * @param attrs
     * @param defStyle
     */
    private void init(AttributeSet attrs, int defStyle){

        // Set setDrawingCacheEnabled to true to support generating a bitmap copy of the view (for saving)
        // See: http://developer.android.com/reference/android/view/View.html#setDrawingCacheEnabled(boolean)
        //      http://developer.android.com/reference/android/view/View.html#getDrawingCache()
        this.setDrawingCacheEnabled(true);

        _paint.setColor(Color.RED);
        _paint.setAlpha(_alpha);
        _paint.setAntiAlias(true);
        _paint.setStyle(Paint.Style.FILL);
        _paint.setStrokeWidth(4);

        _paintBorder.setColor(Color.BLACK);
        _paintBorder.setStrokeWidth(3);
        _paintBorder.setStyle(Paint.Style.STROKE);
        _paintBorder.setAlpha(50);

        //_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh){

        Bitmap bitmap = getDrawingCache();
        Log.v("onSizeChanged", MessageFormat.format("bitmap={0}, w={1}, h={2}, oldw={3}, oldh={4}", bitmap, w, h, oldw, oldh));
        if(bitmap != null) {
            _offScreenBitmap = getDrawingCache().copy(Bitmap.Config.ARGB_8888, true);
            _offScreenCanvas = new Canvas(_offScreenBitmap);
        }
    }

    /**
     * Sets the ImageView, which hosts the image that we will paint in this view
     * @param imageView
     */
    public void setImageView(ImageView imageView){
        _imageView = imageView;
    }

    /**
     * Sets the brush type. Feel free to make your own and completely change my BrushType enum
     * @param brushType
     */
    public void setBrushType(BrushType brushType){
        _brushType = brushType;
    }

    public void setSymmetry(int s){
        symmetry=s;
    }

    /**
     * Clears the painting
     */
    public void clearPainting(){
        if(_offScreenCanvas != null) {
            Paint paint = new Paint();
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            _offScreenCanvas.drawRect(0, 0, this.getWidth(), this.getHeight(), paint);
            invalidate();
        }
    }

    public void savePainting(){
        //First attempt, based on stackoverflow responses
        //Bitmap toDisk = Bitmap.createBitmap(_offScreenCanvas.getWidth(),_offScreenCanvas.getHeight(),Bitmap.Config.ARGB_8888);
        //_offScreenCanvas.setBitmap(toDisk);

        //Second, more successful attempt, based on discussion board on ELMS
        MediaStore.Images.Media.insertImage(getContext().getContentResolver(),_offScreenBitmap,
                "Painting"+savedNum, "Painting number " + savedNum++);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(_offScreenBitmap != null) {
            canvas.drawBitmap(_offScreenBitmap, 0, 0, _paint);
        }

        // Draw the border. Helpful to see the size of the bitmap in the ImageView
        canvas.drawRect(getBitmapPositionInsideImageView(_imageView), _paintBorder);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent){
        //Basically, the way this works is to liste for Touch Down and Touch Move events and determine where those
        //touch locations correspond to the bitmap in the ImageView. You can then grab info about the bitmap--like the pixel color--
        //at that location

        float curTouchX = motionEvent.getX();
        float curTouchY = motionEvent.getY();
        int curTouchXRounded = (int) curTouchX;
        int curTouchYRounded = (int) curTouchY;
        float brushRadius = _defaultRadius;

        //If no image has been loaded, no drawing should take place
        if(_imageView.getDrawingCache() == null){
            return true;
        }

        switch(motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:

            case MotionEvent.ACTION_MOVE:
                // I've removed all of the historical point code, which was causing problems
                //int historySize = motionEvent.getHistorySize();
                int historySize=0;
                //_paint.setColor(Color.BLUE);
                for (int i = 0; i < historySize; i++) {
                    float touchX = motionEvent.getHistoricalX(i);
                    float touchY = motionEvent.getHistoricalY(i);
                    _offScreenCanvas.drawRect(touchX, touchY, touchX + 10.0f, touchY + 10.0f, _paint);
                }

                //draw to the offscreen bitmap for current x,y point.

                Bitmap imageBitmap = _imageView.getDrawingCache();
                int imgHeight = imageBitmap.getHeight();
                int imgWidth = imageBitmap.getWidth();
                //look at corresponding pixel in bitmap, if it is within bounds
                if(getBitmapPositionInsideImageView(_imageView).contains(curTouchXRounded,curTouchYRounded)) {
                    int colorAtTouchPixelInImage = imageBitmap.getPixel(curTouchXRounded, curTouchYRounded);
                    int red = Color.red(colorAtTouchPixelInImage);
                    int blue = Color.blue(colorAtTouchPixelInImage);
                    int green = Color.green(colorAtTouchPixelInImage);
                    _paint.setARGB(_alpha, red, green, blue);
                    float right = _offScreenCanvas.getWidth();
                    float bottom = _offScreenCanvas.getHeight();
                    //Based on brush type, draw different shape of use velocity to determine radius
                    switch(_brushType){
                        case Circle:
                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, _defaultRadius - 5, _paint);
                            switch(symmetry){
                                case 1:
                                    _offScreenCanvas.drawCircle(right-curTouchX, curTouchY, _defaultRadius-5, _paint);
                                    break;
                                case 2:
                                    _offScreenCanvas.drawCircle(curTouchX, bottom-curTouchY, _defaultRadius-5, _paint);
                                    break;
                                case 3:
                                    _offScreenCanvas.drawCircle(right-curTouchX, curTouchY, _defaultRadius-5, _paint);
                                    _offScreenCanvas.drawCircle(curTouchX, bottom-curTouchY, _defaultRadius-5, _paint);
                                    _offScreenCanvas.drawCircle(right-curTouchX, bottom-curTouchY, _defaultRadius-5, _paint);
                                    break;
                            }
                            break;
                        case Square:
                            _offScreenCanvas.drawRect(curTouchX, curTouchY, curTouchX + _defaultRadius, curTouchY + _defaultRadius, _paint);
                            switch(symmetry){
                                case 1:
                                    _offScreenCanvas.drawRect(right - curTouchX, curTouchY, right - curTouchX - _defaultRadius, curTouchY + _defaultRadius, _paint);
                                    break;
                                case 2:
                                    _offScreenCanvas.drawRect(curTouchX, bottom - curTouchY, curTouchX + _defaultRadius, bottom - curTouchY + _defaultRadius, _paint);
                                    break;
                                case 3:
                                    _offScreenCanvas.drawRect(right - curTouchX, curTouchY, right - curTouchX - _defaultRadius, curTouchY + _defaultRadius, _paint);
                                    _offScreenCanvas.drawRect(curTouchX, bottom - curTouchY, curTouchX + _defaultRadius, bottom - curTouchY - _defaultRadius, _paint);
                                    _offScreenCanvas.drawRect(right-curTouchX, bottom - curTouchY, right - curTouchX - _defaultRadius, bottom - curTouchY - _defaultRadius, _paint);
                                    break;
                            }
                            break;
                        case CircleSplatter:
                            //use VelocityTracker class to get current velocity of motion
                            int pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
                            if(velocityTracker == null){
                                velocityTracker = VelocityTracker.obtain();
                            }
                            velocityTracker.clear();
                            velocityTracker.addMovement(motionEvent);
                            velocityTracker.computeCurrentVelocity(1000);
                            float vel = VelocityTrackerCompat.getXVelocity(velocityTracker,pointerId) +
                                    VelocityTrackerCompat.getYVelocity(velocityTracker,pointerId);
                            int velRadius = (int)Math.abs(vel)/40;
                            if(velRadius>200){
                                velRadius /=2;
                            }
                            _offScreenCanvas.drawCircle(curTouchX, curTouchY, velRadius, _paint);
                            switch(symmetry){
                                case 1:
                                    _offScreenCanvas.drawCircle(right-curTouchX, curTouchY, velRadius, _paint);
                                    break;
                                case 2:
                                    _offScreenCanvas.drawCircle(curTouchX, bottom-curTouchY, velRadius, _paint);
                                    break;
                                case 3:
                                    _offScreenCanvas.drawCircle(right-curTouchX, curTouchY, velRadius, _paint);
                                    _offScreenCanvas.drawCircle(curTouchX, bottom-curTouchY, velRadius, _paint);
                                    _offScreenCanvas.drawCircle(right-curTouchX, bottom-curTouchY, velRadius, _paint);
                                    break;
                            }
                            break;
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
        }

        return true;
    }





    /**
     * This method is useful to determine the bitmap position within the Image View. It's not needed for anything else
     * Modified from:
     *  - http://stackoverflow.com/a/15538856
     *  - http://stackoverflow.com/a/26930938
     * @param imageView
     * @return
     */
    private static Rect getBitmapPositionInsideImageView(ImageView imageView){
        Rect rect = new Rect();
        if (imageView == null || imageView.getDrawable() == null) {
            return rect;
        }

        // Get image dimensions
        // Get image matrix values and place them in an array
        float[] f = new float[9];
        imageView.getImageMatrix().getValues(f);

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        final float scaleX = f[Matrix.MSCALE_X];
        final float scaleY = f[Matrix.MSCALE_Y];

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        final Drawable d = imageView.getDrawable();
        final int origW = d.getIntrinsicWidth();
        final int origH = d.getIntrinsicHeight();

        // Calculate the actual dimensions
        final int widthActual = Math.round(origW * scaleX);
        final int heightActual = Math.round(origH * scaleY);

        // Get image position
        // We assume that the image is centered into ImageView
        int imgViewW = imageView.getWidth();
        int imgViewH = imageView.getHeight();

        int top = (int) (imgViewH - heightActual)/2;
        int left = (int) (imgViewW - widthActual)/2;

        rect.set(left, top, left + widthActual, top + heightActual);

        return rect;
    }
}

