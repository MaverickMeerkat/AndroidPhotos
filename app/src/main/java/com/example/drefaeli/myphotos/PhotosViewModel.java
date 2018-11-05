package com.example.drefaeli.myphotos;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.LruCache;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public class PhotosViewModel extends ViewModel {
    private LiveData<List<String>> photosPaths;
    private final MutableLiveData<Boolean> showPhotosButtonPressed = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isFragmentDisplayed = new MutableLiveData<>();
    private ConcurrentHashMap<String, AsyncImageLoader> tasksDictionary = new ConcurrentHashMap<>();
    private LruCache<String, Bitmap> memoryCache;
    PhotosProvider photosProvider;

    // images look ok even if they are twice as small (though x4 is already too much)
    private final static int SCALE_DOWN_IMAGE_FACTOR = 2;

    public PhotosViewModel(PhotosProvider photosProvider) {
        this.photosProvider = photosProvider;
        isFragmentDisplayed.setValue(false);
        showPhotosButtonPressed.setValue(false);
        setUpMemoryCache();
        photosPaths = photosProvider.getPhonePhotos();
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

    LiveData<List<String>> getPhotosObject() {
        return photosPaths;
    }

    void loadPhotos() {
        photosProvider.loadPhotos();
    }

    MutableLiveData<Boolean> getShowPhotosButtonState() {
        return showPhotosButtonPressed;
    }

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

    public LiveData<Bitmap> getImage(String path, int requiredWidth) {
        MutableLiveData<Bitmap> res = new MutableLiveData<>();
        AsyncImageLoader task = new AsyncImageLoader(res, path, requiredWidth);
        tasksDictionary.put(path, task);
        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (RejectedExecutionException ex) {
            // ignore -
            // this exception doesn't really seems to create any problem, and the alternative is
            // to use a task.execute() which is single threaded... or creating a new thread pool
            // executer which is quite complicated (I tried, and it doesn't work well)
        }
        return res;
    }

    @SuppressLint("StaticFieldLeak") // memory leaks are cautioned for
    private class AsyncImageLoader extends AsyncTask<Void, Void, Bitmap> {
        private final MutableLiveData<Bitmap> liveData;
        private String path;
        private int requiredWidth;
        private boolean shouldShow = true;

        AsyncImageLoader(MutableLiveData<Bitmap> liveData, String path, int requiredWidth) {
            this.liveData = liveData;
            this.path = path;
            this.requiredWidth = requiredWidth;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // first check the cache
            Bitmap image = getBitmapFromMemCache(path);
            // if empty, create new
            if (image == null) {
                image = ImageDecoding.getScaledAndRotatedImage(path,
                        requiredWidth / SCALE_DOWN_IMAGE_FACTOR,
                        requiredWidth / SCALE_DOWN_IMAGE_FACTOR);
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            liveData.postValue(bitmap);
            if (!this.isCancelled() && shouldShow && bitmap != null) {
                addBitmapToMemoryCache(path, bitmap);
                tasksDictionary.remove(path);
            }
        }
    }

    public boolean cancelTask(String path) {
        AsyncImageLoader task = tasksDictionary.get(path);
        if (task != null) {
            task.cancel(true);
            tasksDictionary.remove(path);
            return true;
        }
        return false;
    }
}
