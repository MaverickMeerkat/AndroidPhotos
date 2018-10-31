package com.example.drefaeli.myphotos;

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
import android.view.ViewTreeObserver;
import android.widget.ImageView;

public class DisplayPhotoFragment extends Fragment {
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;
    private ImageView imageView;
    private ImageScaler imageScaler;
    private ImageScalerRestore scalerRestore;

    static final String PHOTO_PATH = "PHOTO_PATH"; // shared, so not private
    private static final String PREVIOUS_FRAGMENT = "previousFragmentRestoreObject";
    private static final int REQUIRED_WIDTH = 600;
    private static final int REQUIRED_HEIGHT = 1000;

    public DisplayPhotoFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView = inflater.inflate(R.layout.fragment_display_photo,
                container, false);

        setImage(rootView);

        imageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                imageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                imageScaler = new ImageScaler(imageView, scalerRestore);
            }
        });

        gestureDetector = new GestureDetector(MyApplication.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float newX = imageView.getX() - distanceX;
                float newY = imageView.getY() - distanceY;
                imageScaler.doScroll(newX, newY);
                return super.onScroll(e1, e2, distanceX, distanceY);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                imageScaler.doDoubleTapScale(e.getX(), e.getY());
                return super.onDoubleTap(e);
            }
        });

        scaleGestureDetector = new ScaleGestureDetector(MyApplication.getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        imageScaler.onScale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
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

    private void setImage(View rootView) {
        String path = getArguments().getString(PHOTO_PATH);
        imageView = rootView.findViewById(R.id.full_image_view);
        // scaling down image (if it's too big) and rotating it
        Bitmap bitmap = ImageManipulations.getScaledDownImage(path, REQUIRED_WIDTH, REQUIRED_HEIGHT);
        Bitmap rotatedBitmap = ImageManipulations.rotateImageByExif(bitmap, path);
        imageView.setImageBitmap(rotatedBitmap);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(PREVIOUS_FRAGMENT, imageScaler.getScalerRestore());
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(PREVIOUS_FRAGMENT)) {
                scalerRestore = savedInstanceState.getParcelable(PREVIOUS_FRAGMENT);
            }
        }
    }
}
