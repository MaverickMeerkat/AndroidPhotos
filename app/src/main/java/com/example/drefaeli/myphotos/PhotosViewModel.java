package com.example.drefaeli.myphotos;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class PhotosViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<String>> photosPaths = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showPhotosButtonPressed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFragmentDisplayed = new MutableLiveData<>();
//    private final ConcurrentHashMap<String, Bitmap> storedGridImages = new ConcurrentHashMap<>();
    LruCache<String, Bitmap> memoryCache;

    PhotosProvider photosProvider;

    public PhotosViewModel() {
        photosProvider = new PhotosProvider(MyApplication.getContext());
        isFragmentDisplayed.setValue(false);
        showPhotosButtonPressed.setValue(false);
        setUpMemoryCache();
    }

    private void setUpMemoryCache() {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    LiveData<ArrayList<String>> getPhotosObject() {
        return photosPaths;
    }

    void loadPhotos() {
        ArrayList<String> photos = photosProvider.getPhonePhotos();
        photosPaths.setValue(photos);
    }

    MutableLiveData<Boolean> getShowPhotosButtonState() {
        return showPhotosButtonPressed;
    }

//    ConcurrentHashMap<String, Bitmap> getStoredGridImages() {
//        return storedGridImages;
//    }

    MutableLiveData<Boolean> getIsFragmentDisplayed() {
        return isFragmentDisplayed;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }
}
