// Copyright (c) 2018 Lightricks. All rights reserved.
// Created by David Refaeli.
package com.example.drefaeli.myphotos;

import android.widget.ImageView;

class ImageScaler {
    private ImageView imageView;
    private ImageScalerRestore scalerRestore;
    private int viewWidth;
    private int viewHeight;
    private int centerViewX;
    private int centerViewY;
    private int relativeHeight;
    private int relativeWidth;
    private int minLayoutTranslationX;
    private int minLayoutTranslationY;
    private int maxLayoutTranslationX;
    private int maxLayoutTranslationY;
    private float imageToRelativeRatio;
    private float scaleFactor = 1.f; // initial value
    private float maxZoomRatio = 10f; // initial value; will be updated by actual image size ratio

    private final float MIN_ZOOM_RATIO = 1f;
    private static final float SLOW_DOWN_FACTOR = 6f;

    ImageScaler(ImageView view, ImageScalerRestore restore) {
        imageView = view;
        scalerRestore = restore;

        setInitialValues(view);

        if (scalerRestore != null) {
            // User scaling-down might end in 1.0000001 which is technically 1, so we ignore it -
            // and treat every zoom under 1.05 as 1.
            if (scalerRestore.previousScale > MIN_ZOOM_RATIO + 0.05) {
                restoreToPreviousView();
            }
        }
    }

    private void setInitialValues(ImageView view) {
        viewWidth = imageView.getWidth();
        viewHeight = imageView.getHeight();
        centerViewX = viewWidth / 2;
        centerViewY = viewHeight / 2;
        float screenRatio = (float) viewWidth / viewHeight;
        int imageWidth = view.getDrawable().getIntrinsicWidth();
        int imageHeight = view.getDrawable().getIntrinsicHeight();
        float imageRatio = (float) imageWidth / imageHeight;
        setRelativeRatios(screenRatio, imageWidth, imageHeight, imageRatio);
    }

    // Scaling is done on the view, not image. Image is scaled down to fit view, in a certain ratio.
    // Unless the image width/height ratio is exactly the same as the view, the image will only
    // reside in part of the view leaving margins. When the view is stretched, so are the margins.
    // Relative width/height is the size of the image (scaled down to fit the entire view) minus the
    // margins.
    // Zoom is subjected to the actual image (by multiplying on the ratio) and not the view - for
    // uniformity sake (ratios change on rotation, and you want the max zoom to be relative to the
    // image, not the view).
    // Note that if image is really(!) smaller than view, zooming won't be possible.
    private void setRelativeRatios(float screenRatio, int imageWidth, int imageHeight, float imageRatio) {
        if (imageRatio < screenRatio) { // margins on the widths
            relativeHeight = viewHeight;
            imageToRelativeRatio = (float) imageHeight / (float) viewHeight;
            relativeWidth = (int) (imageWidth / imageToRelativeRatio);
            maxZoomRatio *= imageToRelativeRatio;
        } else { // margins on the heights
            relativeWidth = viewWidth;
            imageToRelativeRatio = (float) imageWidth / (float) viewWidth;
            relativeHeight = (int) (imageHeight / imageToRelativeRatio);
            maxZoomRatio *= imageToRelativeRatio;
        }
    }

    // Making sure scrolling doesn't exceed the image boundary. Min/max x/y is set on image, not view.
    void doScroll(float x, float y) {
        if (x >= minLayoutTranslationX && x <= maxLayoutTranslationX) {
            imageView.setX(x);
        }
        if (y >= minLayoutTranslationY && y <= maxLayoutTranslationY) {
            imageView.setY(y);
        }
    }

    // Used on screen-rotations - to restore the view to the correct scaling and position it was before
    private void restoreToPreviousView() {
        // Adjusting the old image ratio for the new image ratio.
        // Previous scale is what is set on the view, not the image. We have to divide the scaling by
        // the previous ratio (which brings us back to [pure] scaling of the view), and then multiply
        // it by the new ratio to be again scaled by the image and not the view.
        float correction = imageToRelativeRatio / scalerRestore.previousImageToRelativeRatio;
        float newScaleFactor = Math.max(MIN_ZOOM_RATIO,
                Math.min(scalerRestore.previousScale * correction, maxZoomRatio));
        doScale(newScaleFactor, centerViewX, centerViewY);

        // positioning doesn't change, as it's relative to center, and image
        float setX = Math.max(minLayoutTranslationX, Math.min(scalerRestore.previousX, maxLayoutTranslationX));
        float setY = Math.max(minLayoutTranslationY, Math.min(scalerRestore.previousY, maxLayoutTranslationY));
        imageView.setX(setX);
        imageView.setY(setY);
    }

    // Double tap on a point in the view performs max scale to it
    void doDoubleTapScale(float focusX, float focusY) {
        if (scaleFactor == 1) {
            doScale(maxZoomRatio, focusX, focusY);
        } else {
            doScale(1 / scaleFactor, focusX, focusY);
        }
    }

    // Scaling is really fast, so we slow it down by a factor
    private float slowDownScaling(float newScale) {
        if (newScale > 1) { // zoom in
            newScale = 1 + (newScale - 1) / SLOW_DOWN_FACTOR;
        } else { // zoom out
            newScale = 1 - (1 - newScale) / SLOW_DOWN_FACTOR;
        }
        return newScale;
    }

    // Check if scaling is possible (more than minimum ratio, less than maximum)
    private boolean scaleIsFeasible(float newScale) {
        float scaleAfter = scaleFactor * newScale;
        return scaleAfter >= MIN_ZOOM_RATIO && scaleAfter <= maxZoomRatio;
    }

    private void doScale(float newScale, float focusX, float focusY) {
        scaleFactor *= newScale;
        imageView.setScaleX(scaleFactor);
        imageView.setScaleY(scaleFactor);
        updateScrollBoundaries();
        translateViewToNewScale(newScale, focusX, focusY);
    }

    private void translateViewToNewScale(float newScale, float focusX, float focusY) {
        // previous translation needs to be updated based on the new scale, in order to stay in place
        float scaledPreviousTranslationX = imageView.getX() * newScale;
        float scaledPreviousTranslationY = imageView.getY() * newScale;
        // if we also scale into a specific focus point, we have to translate in that direction
        float scaledNewTranslationX = (focusX - centerViewX) * (1 - newScale);
        float scaledNewTranslationY = (focusY - centerViewY) * (1 - newScale);
        // add both together
        float totalTranslationX = scaledPreviousTranslationX + scaledNewTranslationX;
        float totalTranslationY = scaledPreviousTranslationY + scaledNewTranslationY;
        // make sure total translation doesn't break the boundaries
        totalTranslationX = (totalTranslationX <= minLayoutTranslationX) ? minLayoutTranslationX :
                (totalTranslationX >= maxLayoutTranslationX) ? maxLayoutTranslationX : totalTranslationX;
        totalTranslationY = (totalTranslationY <= minLayoutTranslationY) ? minLayoutTranslationY :
                (totalTranslationY >= maxLayoutTranslationY) ? maxLayoutTranslationY : totalTranslationY;
        // do translation
        imageView.setTranslationX(totalTranslationX);
        imageView.setTranslationY(totalTranslationY);
    }

    private void updateScrollBoundaries() {
        // Scaling-up enlarges the view layout, leaving the view centered. This means that the new view
        // position can "move" up and down, left and right - and so one must check how much we can "move".
        // What actually moves is the underlying layout, so we move left by pulling the layout right,
        // and move down by pulling the layout up. The view itself never actually moves.
        // We want to move only relative to the image, not the view (i.e. not scroll to see the initial
        // margins). Also we can only move in a certain axis once the margin of that axis is completely gone.
        // The scaled relative width/height is the max size of the image in the current scaling.
        // Subtracting it from the view size will give the size the layout can move (doubled, and negative).
        // Note that the negative number (min x/y) represents the minimal size we can translate the
        // layout to, which corresponds to the maximum scrolling right/down. The positive number (max x/y)
        // represents the maximum translation, and corresponds to the maximum scrolling left/up.
        int scaledRelativeWidth = (int) (scaleFactor * relativeWidth);
        int scaledRelativeHeight = (int) (scaleFactor * relativeHeight);
        minLayoutTranslationX = (viewWidth - scaledRelativeWidth) / 2;
        // not negative means view is still larger than image, so don't allow movement
        minLayoutTranslationX = (minLayoutTranslationX < 0) ? minLayoutTranslationX : 0;
        maxLayoutTranslationX = -minLayoutTranslationX;
        minLayoutTranslationY = (viewHeight - scaledRelativeHeight) / 2;
        // not negative means view is still larger than image, so don't allow movement
        minLayoutTranslationY = (minLayoutTranslationY < 0) ? minLayoutTranslationY : 0;
        maxLayoutTranslationY = -minLayoutTranslationY;
    }

    void onScale(float newScale, float focusX, float focusY) {
        newScale = slowDownScaling(newScale);
        if (scaleIsFeasible(newScale)) {
            doScale(newScale, focusX, focusY);
        }
    }

    ImageScalerRestore getScalerRestore() {
        return new ImageScalerRestore(viewWidth, viewHeight, imageView.getX(), imageView.getY(),
                scaleFactor, imageToRelativeRatio);
    }
}
