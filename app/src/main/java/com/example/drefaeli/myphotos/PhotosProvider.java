package com.example.drefaeli.myphotos;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.util.ArrayList;

class PhotosProvider {
    private Context context;

    PhotosProvider(Context context){
        this.context = context;
    }

    /**
     * Loads all photos from phone and returns it in a list
     * @return
     */
    ArrayList<String> getPhonePhotos() {
        ArrayList<String> photosUri = new ArrayList<>();
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String photosColumnName =  MediaStore.MediaColumns.DATA;
        String[] projection = { photosColumnName };
        Cursor images = context.getContentResolver().query(uri, projection, null,
                null, photosColumnName + " DESC");
        int column_index_data = images.getColumnIndexOrThrow(photosColumnName);

        while (images.moveToNext()) {
            photosUri.add(images.getString(column_index_data));
        }

        images.close();
        return photosUri;
    }

    ArrayList<String> findRemoved(ArrayList<String> originalList){
        ArrayList<String> returnList = new ArrayList<>(originalList);
        ArrayList<String> newList = getPhonePhotos();
        returnList.removeAll(newList);
        return returnList;
    }
}
