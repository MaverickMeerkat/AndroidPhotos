package com.example.drefaeli.myphotos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;

public class PhotosProvider {
    private Context context;

    public PhotosProvider(Context context){
        this.context = context;
    }

    /**
     * Loads all photos from phone and returns it in a list
     * @return
     */
    public ArrayList<String> loadPhotos() {
        ArrayList<String> photosUri = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor images = context.getContentResolver().query(uri, projection, null, null, null);
        int column_index_data = images.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

        while (images.moveToNext()) {
            photosUri.add(images.getString(column_index_data));
        }

        images.close();
        Collections.reverse(photosUri);
        return photosUri;
    }

    public ArrayList<String> findRemoved(ArrayList<String> originalList){
        ArrayList<String> returnList = new ArrayList<>(originalList);
        ArrayList<String> newList = loadPhotos();
        returnList.removeAll(newList);
        return returnList;
    }
}
