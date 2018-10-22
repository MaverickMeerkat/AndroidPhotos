package com.example.drefaeli.myphotos;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

public class DisplayPhotoFragment extends Fragment {
    private static final float DOUBLE_TAP_SCALE = 2f;
    private static final float SLOW_DOWN_FACTOR = 6;
    private int viewWidth;
    private int viewHeight;
    private int relativeHeight;
    private int relativeWidth;
    private int centerViewX;
    private int centerViewY;
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    private float scaleFactor = 1.f;
    private final float MIN_ZOOM_RATIO = 1f;
    private final float MAX_ZOOM_RATIO = 10.0f;
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private ImageView imageView;
    public static final String PHOTO_PATH = "PHOTO_PATH";
    private boolean previousFragmentViewAvailable;
    private int previousWidth;
    private int previousHeight;
    private float previousX;
    private float previousY;
    private float previousScale;

    public DisplayPhotoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_display_photo,
                container, false);

        String path = getArguments().getString(PHOTO_PATH);
        imageView = rootView.findViewById(R.id.full_image_view);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        Bitmap rotatedBitmap = ImageManipulations.rotateImageByExif(bitmap, path);
        imageView.setImageBitmap(rotatedBitmap);

        imageView.post(() -> {
            viewWidth = imageView.getWidth();
            viewHeight = imageView.getHeight();
            float screenRatio = (float) viewWidth / viewHeight;

            int imageWidth = imageView.getDrawable().getIntrinsicWidth();
            int imageHeight = imageView.getDrawable().getIntrinsicHeight();
            float imageRatio = (float) imageWidth / imageHeight;

            if (imageRatio < screenRatio) {
                relativeHeight = viewHeight;
                relativeWidth = imageWidth * viewHeight / imageHeight;
            } else {
                relativeWidth = viewWidth;
                relativeHeight = imageHeight * viewWidth / imageWidth;
            }

            centerViewX = viewWidth / 2;
            centerViewY = viewHeight / 2;

            if (previousFragmentViewAvailable) {
                restoreToPreviousView();
            }
        });

        gestureDetector = new GestureDetector(MyApplication.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float newX = imageView.getX() - distanceX;
                float newY = imageView.getY() - distanceY;

                if (newX >= minX && newX <= maxX) {
                    imageView.setX(newX);
                }
                if (newY >= minY && newY <= maxY) {
                    imageView.setY(newY);
                }
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (scaleFactor == 1) {
                    doScale(DOUBLE_TAP_SCALE * SLOW_DOWN_FACTOR, e.getX(), e.getY());
                } else {
                    doScale(1 / scaleFactor, e.getX(), e.getY());
                }
                return super.onDoubleTap(e);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(MyApplication.getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float newScale = detector.getScaleFactor();
                newScale = slowDownScaling(newScale);
                if (scaleIsFeasible(newScale)) {
                    doScale(newScale, detector.getFocusX(), detector.getFocusY());
                }
                return super.onScale(detector);
            }
        });

        rootView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });

        return rootView;
    }

    public void restoreToPreviousView() {
        doScale(previousScale, centerViewX, centerViewY);

        int widthCorrection = (viewWidth - previousWidth) / 2;
        int heightCorrection = (viewHeight - previousHeight) / 2;

        float setX = Math.max(minX, Math.min(previousX - widthCorrection, maxX));
        float setY = Math.max(minY, Math.min(previousY - heightCorrection, maxY));

        imageView.setX(setX);
        imageView.setY(setY);
    }

    private float slowDownScaling(float newScale) {
        if (newScale > 1) { // zoom in
            newScale = 1 + (newScale - 1) / SLOW_DOWN_FACTOR;
        } else { // zoom out
            newScale = 1 - (1 - newScale) / SLOW_DOWN_FACTOR;
        }
        return newScale;
    }

    private boolean scaleIsFeasible(float newScale) {
        float scaleAfter = scaleFactor * newScale;
        return scaleAfter >= MIN_ZOOM_RATIO && scaleAfter <= MAX_ZOOM_RATIO;
    }

    private void doScale(float newScale, float focusX, float focusY) {
        scaleFactor *= newScale;

        // scale
        imageView.setScaleX(scaleFactor);
        imageView.setScaleY(scaleFactor);

        // adjust width, min/max x/y, movingCenter
        int scaledWidth = (int) (scaleFactor * relativeWidth);
        int scaledHeight = (int) (scaleFactor * relativeHeight);
        minX = (viewWidth - scaledWidth) / 2;
        minX = (minX < 0) ? minX : 0;
        maxX = -minX;
        minY = (viewHeight - scaledHeight) / 2;
        minY = (minY < 0) ? minY : 0;
        maxY = -minY;

        // translate photo
        float previousTranslationX = imageView.getX();
        float previousTranslationY = imageView.getY();
        float scaledPreviousTranslationX = previousTranslationX * newScale;
        float scaledPreviousTranslationY = previousTranslationY * newScale;

        float scaledNewTranslationX = (focusX - centerViewX) * (1 - newScale);
        float scaledNewTranslationY = (focusY - centerViewY) * (1 - newScale);

        float totalTranslationX = scaledPreviousTranslationX + scaledNewTranslationX;
        totalTranslationX = (totalTranslationX <= minX) ? minX : (totalTranslationX >= maxX) ? maxX : totalTranslationX;
        float totalTranslationY = scaledPreviousTranslationY + scaledNewTranslationY;
        totalTranslationY = (totalTranslationY <= minY) ? minY : (totalTranslationY >= maxY) ? maxY : totalTranslationY;

        imageView.setTranslationX(totalTranslationX);
        imageView.setTranslationY(totalTranslationY);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("fragmentPreviousAvailable", true);
        outState.putInt("fragmentPreviousWidth", viewWidth);
        outState.putInt("fragmentPreviousHeight", viewHeight);
        outState.putFloat("fragmentTranslateX", imageView.getX());
        outState.putFloat("fragmentTranslateY", imageView.getY());
        outState.putFloat("fragmentScaleFactor", scaleFactor);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey("fragmentPreviousAvailable")) {
                previousFragmentViewAvailable = savedInstanceState.getBoolean("fragmentPreviousAvailable");
                previousWidth = savedInstanceState.getInt("fragmentPreviousWidth");
                previousHeight = savedInstanceState.getInt("fragmentPreviousHeight");
                previousX = savedInstanceState.getFloat("fragmentTranslateX");
                previousY = savedInstanceState.getFloat("fragmentTranslateY");
                previousScale = savedInstanceState.getFloat("fragmentScaleFactor");
            }
        }
    }
}
