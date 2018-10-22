package com.example.drefaeli.myphotos;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    private PhotosViewModel viewModel;
    private FragmentManager fragmentManager;
    private int gridImageWidth;
    private Context activityContext;
    private View fragmentContainer;
    final static String TAG_FRAGMENT = "TAG_FRAGMENT";
    final static int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 31000;
    final int INITIAL_GRID_IMAGE_WIDTH = 360;
    final String BUTTON_STATE = "BUTTON_STATE";
    final String FRAGMENT_STATE = "FRAGMENT_STATE";
    final String RECYCLERVIEW_POSITION = "RECYCLERVIEW_POSITION";
    boolean photosAlreadyLoaded;
    private ContentObserver externalContentObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityContext = this;
        photosAlreadyLoaded = false;

        fragmentContainer = findViewById(R.id.fragment_container);
        fragmentContainer.setVisibility(View.INVISIBLE);
        fragmentManager = getSupportFragmentManager();

        recyclerView = findViewById(R.id.photos_grid_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        viewModel = ViewModelProviders.of(this).get(PhotosViewModel.class);

        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                // calculate image width and photos-per-row
                int viewWidth = recyclerView.getWidth();
                int photosPerRow = viewWidth / INITIAL_GRID_IMAGE_WIDTH;
                gridImageWidth = INITIAL_GRID_IMAGE_WIDTH + (viewWidth - photosPerRow * INITIAL_GRID_IMAGE_WIDTH) / photosPerRow;

                recyclerView.setLayoutManager(new GridLayoutManager(activityContext, photosPerRow));
                recyclerAdapter = new RecyclerAdapter(activityContext, viewModel, gridImageWidth, fragmentManager, fragmentContainer);
                recyclerView.setAdapter(recyclerAdapter);

                // update state of photos
                viewModel.getPhotosObject().observe(MainActivity.this, photos -> {
                    recyclerAdapter.updatePhotosPaths(photos);
                    recyclerAdapter.notifyDataSetChanged();
                });

                // update state of show-photos button on rotate
                viewModel.getShowPhotosButtonState().observe(MainActivity.this, isShowPressed -> {
                    if (isShowPressed && !photosAlreadyLoaded && isPermissionGranted()) {
                        viewModel.loadPhotos();
                    }
                });

                if (savedInstanceState != null) {
                    // update state of recycler position
                    recyclerView.getLayoutManager().
                            onRestoreInstanceState(savedInstanceState.getParcelable(RECYCLERVIEW_POSITION));
                    // update state of fragment
                    if (viewModel.getIsFragmentDisplayed().getValue()) {
                        fragmentContainer.setVisibility(View.VISIBLE);
                    }
                }
            }
        });

        externalContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                String path = getRealPathFromURI(MainActivity.this, uri);

                if (path != null) { // add
                    ArrayList<String> photosInApp = viewModel.getPhotosObject().getValue();
                    if (!photosInApp.contains(path)) {
                        int position = ((LinearLayoutManager) recyclerView.getLayoutManager())
                                .findFirstVisibleItemPosition();
                        recyclerAdapter.addItem(0, path);
                        if (position == 0) {
                            recyclerView.scrollToPosition(0);
                        }
                    }
                } else { //remove
                    // removed images come with "empty" uris: "content://media" - so query will fail
                    // added images come with external name:  "content://media/external/images/media/..."
                    recyclerAdapter.removeItems();
                }
                super.onChange(selfChange, uri);
            }
        };

        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, externalContentObserver);
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (Exception ex) {
            // removed images come with "empty" uris: "content://media" - so query will fail
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    protected void onDestroy() {
        getContentResolver().unregisterContentObserver(externalContentObserver); // memory leak if forget to unregister
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(BUTTON_STATE, viewModel.getShowPhotosButtonState().getValue());
        outState.putBoolean(FRAGMENT_STATE, viewModel.getIsFragmentDisplayed().getValue());
        outState.putParcelable(RECYCLERVIEW_POSITION, recyclerView.getLayoutManager().onSaveInstanceState());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        viewModel.getShowPhotosButtonState().setValue(savedInstanceState.getBoolean(BUTTON_STATE));
        viewModel.getIsFragmentDisplayed().setValue(savedInstanceState.getBoolean(FRAGMENT_STATE));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            fragmentContainer.setVisibility(View.INVISIBLE); // hide fragment view
            viewModel.getIsFragmentDisplayed().postValue(false); // update view-model
        }
    }

    public void showPhotos(View view) {
        if (isPermissionGranted()) {
            photosAlreadyLoaded = true;
            viewModel.getShowPhotosButtonState().postValue(true);
            viewModel.loadPhotos();
        } else {
            requestPermission();
        }
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.myCoordinatorLayout), "We need to access your photos",
                    Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("Ok", view -> doRequestPermission());
            snackbar.show();
        } else {
            doRequestPermission();
        }
    }

    private void doRequestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    private boolean isPermissionGranted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadPhotos();
                } else {
                    Toast.makeText(this, "No permission. Abort", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
