package com.example.drefaeli.myphotos;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.ViewHolder> {
    private ArrayList<String> photosPaths;
    private PhotosViewModel viewModel;
    private FragmentManager fragmentManager;
    private int requiredWidth;
    private View fragmentContainer;
    private Context context;
    private ConcurrentHashMap<UUID, AsyncImageLoader> tasksDictionary;

    private final static String TAG_FRAGMENT = "displayPhotoFragment";
    // images look ok even if they are twice as small (though x4 is already too much)
    private final static int SCALE_DOWN_IMAGE_FACTOR = 2;

    RecyclerAdapter(Context context, PhotosViewModel viewModel, int requiredWidth,
                    FragmentManager fragmentManager, View fragmentContainer) {
        this.context = context;
        this.viewModel = viewModel;
        this.fragmentManager = fragmentManager;
        this.requiredWidth = requiredWidth;
        this.fragmentContainer = fragmentContainer;
        tasksDictionary = new ConcurrentHashMap<>();
        photosPaths = new ArrayList<>();
    }

    void updatePhotosPaths(ArrayList<String> paths) {
        photosPaths = paths;
    }

    void addItem(int position, String path) {
        photosPaths.add(position, path);
        notifyItemInserted(position);
    }

    void removeItems() {
        ArrayList<String> paths = viewModel.photosProvider.findRemoved(viewModel.getPhotosObject().getValue());
        for (String path : paths) {
            int position = photosPaths.indexOf(path);
            photosPaths.remove(path);
            notifyItemRemoved(position);
            viewModel.getStoredGridImages().remove(path); // remove from cache
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final UUID key;
        private ImageView imageView;
        private String path;

        ViewHolder(ImageView v) {
            super(v);
            v.setOnClickListener(this);
            imageView = v;
            key = UUID.randomUUID();
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public void onClick(View v) {
            showFragment(path);
        }
    }

    private void showFragment(String path) {
        Bundle bundle = createBundle(path);
        DisplayPhotoFragment fragment = new DisplayPhotoFragment();
        fragment.setArguments(bundle);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment_container, fragment, TAG_FRAGMENT);
        ft.addToBackStack(null);
        ft.commit();
        fragmentContainer.setVisibility(View.VISIBLE);
        viewModel.getIsFragmentDisplayed().postValue(true);
    }

    private Bundle createBundle(String path) {
        Bundle bundle = new Bundle();
        bundle.putString(DisplayPhotoFragment.PHOTO_PATH, path);
        return bundle;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        LayoutParams lp = new LayoutParams(requiredWidth, requiredWidth);
        imageView.setLayoutParams(lp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setPadding(1, 1, 1, 1);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(R.mipmap.ic_launcher_round);
        String path = photosPaths.get(position);
        holder.setPath(path);

        AsyncImageLoader task = new AsyncImageLoader(holder);
        tasksDictionary.put(holder.key, task);
        try {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch(RejectedExecutionException ex) {
            // ignore -
            // this exception doesn't really seems to create any problem, and the alternative is
            // to use a task.execute() which is single threaded... or creating a new thread pool
            // executer which is quite complicated (I tried, and it doesn't work well)
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);

        viewModel.getStoredGridImages().remove(holder.path);

        tasksDictionary.remove(holder.key); // memory leak without it ... :-(

        AsyncImageLoader previousTask = tasksDictionary.get(holder.key);
        if (previousTask != null) {
            previousTask.shouldShow = previousTask.cancel(true);
        }
    }

    @SuppressLint("StaticFieldLeak") // memory leaks are cautioned for
    private class AsyncImageLoader extends AsyncTask<Void, Void, Bitmap> {
        private ViewHolder viewHolder;
        private boolean shouldShow = true;

        AsyncImageLoader(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            // first check the cache
            Bitmap image = viewModel.getStoredGridImages().get(viewHolder.path);
            // if empty, create new
            if (image == null) {
                Bitmap bitmap = ImageManipulations.getScaledDownImage(viewHolder.path,
                        requiredWidth / SCALE_DOWN_IMAGE_FACTOR,
                        requiredWidth / SCALE_DOWN_IMAGE_FACTOR);
                image = ImageManipulations.rotateImageByExif(bitmap, viewHolder.path);
            }
            return image;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if (!this.isCancelled() && shouldShow && bitmap != null) {
                viewHolder.imageView.setImageBitmap(bitmap);
                viewModel.getStoredGridImages().put(viewHolder.path, bitmap);
                tasksDictionary.remove(viewHolder.key); // memory leak without it ... :-(
            }
        }
    }

    @Override
    public int getItemCount() {
        return photosPaths.size();
    }
}
