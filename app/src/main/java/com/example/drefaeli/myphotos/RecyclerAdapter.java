package com.example.drefaeli.myphotos;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;

public class RecyclerAdapter extends ListAdapter<String, RecyclerAdapter.ViewHolder> {
    private PhotosViewModel viewModel;
    private int requiredWidth;
    private AppCompatActivity context;
    private OnPhotoClickListener photoListener;

    public interface OnPhotoClickListener {
        void onPhotoClick(String path);
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener){
        this.photoListener = listener;
    }

    RecyclerAdapter(AppCompatActivity context, PhotosViewModel viewModel, int requiredWidth) {
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
        this.requiredWidth = requiredWidth;

    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView imageView;
        private String path;
        private OnPhotoClickListener photoClickListener;

        ViewHolder(ImageView v, OnPhotoClickListener photoClickListener) {
            super(v);
            this.photoClickListener = photoClickListener;
            v.setOnClickListener(this);
            imageView = v;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public void onClick(View v) {
            if (photoClickListener != null) {
                photoClickListener.onPhotoClick(path);
            }
//            showFragment(path);
        }
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        LayoutParams lp = new LayoutParams(requiredWidth, requiredWidth);
        imageView.setLayoutParams(lp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setPadding(1, 1, 1, 1);
        return new ViewHolder(imageView, photoListener);
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
