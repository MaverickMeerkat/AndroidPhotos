package com.example.drefaeli.myphotos;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class PhotosViewModel extends ViewModel {
    private final MutableLiveData<ArrayList<String>> photosPaths = new MutableLiveData<>();
    private final MutableLiveData<Boolean> showPhotosButtonPressed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFragmentDisplayed = new MutableLiveData<>();
    private final ConcurrentHashMap<String, Bitmap> storedGridImages = new ConcurrentHashMap<>();

    public PhotosProvider photosProvider;

    public PhotosViewModel() {
        photosProvider = new PhotosProvider(MyApplication.getContext());
        isFragmentDisplayed.setValue(false);
        showPhotosButtonPressed.setValue(false);
    }

    public LiveData<ArrayList<String>> getPhotosObject() {
        return photosPaths;
    }

    public void loadPhotos() {
        ArrayList<String> photos = photosProvider.loadPhotos();
        photosPaths.setValue(photos);
    }

    public MutableLiveData<Boolean> getShowPhotosButtonState() {
        return showPhotosButtonPressed;
    }

    public ConcurrentHashMap<String, Bitmap> getStoredGridImages() {
        return storedGridImages;
    }

    public MutableLiveData<Boolean> getIsFragmentDisplayed() {
        return isFragmentDisplayed;
    }
}
