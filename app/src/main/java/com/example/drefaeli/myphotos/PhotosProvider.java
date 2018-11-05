package com.example.drefaeli.myphotos;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

class PhotosProvider {
    private Context context;
    private Executor executorService;
    MutableLiveData<List<String>> photosUri = new MutableLiveData<>();

    PhotosProvider(Context context, Executor executorService) {
        this.context = context;
        this.executorService = executorService;
    }

    LiveData<List<String>> getPhonePhotos() {
        return photosUri;
    }

    /**
     * Loads all photos from phone and returns it in a list
     *
     * @return
     */
    void loadPhotos() {
        executorService.execute(() -> {
            List<String> urls = new ArrayList<>();
            Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            String photosColumnName = MediaStore.MediaColumns.DATA;
            String photosColumnDate = MediaStore.MediaColumns.DATE_MODIFIED;
            String[] projection = {photosColumnName, photosColumnDate};
            Cursor images = context.getContentResolver().query(uri, projection, null,
                    null, photosColumnDate + " DESC");
            int column_index_data = images.getColumnIndexOrThrow(photosColumnName);

            while (images.moveToNext()) {
                urls.add(images.getString(column_index_data));
            }
            photosUri.postValue(urls);
        });
    }

    List<String> findRemoved(List<String> originalList){
        List<String> returnList = new ArrayList<>(originalList);
        List<String> newList = getPhonePhotos().getValue();
        returnList.removeAll(newList);
        return returnList;
    }

}
