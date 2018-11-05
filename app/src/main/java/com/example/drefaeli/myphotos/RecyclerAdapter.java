package com.example.drefaeli.myphotos;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

public class RecyclerAdapter extends ListAdapter<String, RecyclerAdapter.ViewHolder> {
    private PhotosViewModel viewModel;
    private FragmentManager fragmentManager;
    private int requiredWidth;
    private View fragmentContainer;
    private AppCompatActivity context;

    private final static String TAG_FRAGMENT = "displayPhotoFragment";

    RecyclerAdapter(AppCompatActivity context, PhotosViewModel viewModel, int requiredWidth,
                    FragmentManager fragmentManager, View fragmentContainer) {
        super(new DiffUtil.ItemCallback<String>() {
            @Override
            public boolean areItemsTheSame(@NonNull String s, @NonNull String t1) {
                return s.equals(t1);
            }

            @Override
            public boolean areContentsTheSame(@NonNull String s, @NonNull String t1) {
                return s.equals(t1);
            }
        });
        this.context = context;
        this.viewModel = viewModel;
        this.fragmentManager = fragmentManager;
        this.requiredWidth = requiredWidth;
        this.fragmentContainer = fragmentContainer;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView imageView;
        private String path;

        ViewHolder(ImageView v) {
            super(v);
            v.setOnClickListener(this);
            imageView = v;
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
        DisplayPhotoFragment fragment = DisplayPhotoFragment.createInstance(path);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.fragment_container, fragment, TAG_FRAGMENT);
        ft.addToBackStack(null);
        ft.commit();
        fragmentContainer.setVisibility(View.VISIBLE);
        viewModel.getIsFragmentDisplayed().postValue(true);
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
        final String path = getItem(position);
        holder.setPath(path);
        viewModel.getImage(path, requiredWidth).observe(context, bitmap -> {
            if (path.equals(getItem(position))) {
                holder.imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        viewModel.cancelTask(holder.path);
    }
}
