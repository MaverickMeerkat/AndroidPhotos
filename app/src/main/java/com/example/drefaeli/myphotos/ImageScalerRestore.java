// Copyright (c) 2018 Lightricks. All rights reserved.
// Created by David Refaeli.
package com.example.drefaeli.myphotos;

import android.os.Parcel;
import android.os.Parcelable;

public class ImageScalerRestore implements Parcelable {
    private int previousWidth;
    private int previousHeight;
    float previousX;
    float previousY;
    float previousScale;
    float previousImageToRelativeRatio;

    ImageScalerRestore(int previousWidth, int previousHeight, float previousX, float previousY, float previousScale,
                       float previousImageToRelativeRatio){
        this.previousWidth = previousWidth;
        this.previousHeight = previousHeight;
        this.previousX = previousX;
        this.previousY = previousY;
        this.previousScale = previousScale;
        this.previousImageToRelativeRatio = previousImageToRelativeRatio;
    }

    private ImageScalerRestore(Parcel in) {
        previousWidth = in.readInt();
        previousHeight = in.readInt();
        previousX = in.readFloat();
        previousY = in.readFloat();
        previousScale = in.readFloat();
        previousImageToRelativeRatio = in.readFloat();
    }

    public static final Creator<ImageScalerRestore> CREATOR = new Creator<ImageScalerRestore>() {
        @Override
        public ImageScalerRestore createFromParcel(Parcel in) {
            return new ImageScalerRestore(in);
        }

        @Override
        public ImageScalerRestore[] newArray(int size) {
            return new ImageScalerRestore[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(new int[] {previousWidth, previousHeight});
        dest.writeFloatArray(new float[] {previousX, previousY, previousScale, previousImageToRelativeRatio});
    }
}
