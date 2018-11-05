package com.example.drefaeli.myphotos;

import android.Manifest;
import android.app.ActivityManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
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
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerAdapter recyclerAdapter;
    private PhotosViewModel viewModel;
    private FragmentManager fragmentManager;
    private int gridImageWidth;
    private View fragmentContainer;
    boolean photosAlreadyLoaded;
    private ContentObserver externalContentObserver;

    private final static int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 0;
    private final static int INITIAL_GRID_IMAGE_WIDTH = 500;
    private final static int RECYCLER_VIEW_CACHE_SIZE = 20;
    private final static String BUTTON_STATE = "BUTTON_STATE";
    private final static String FRAGMENT_STATE = "FRAGMENT_STATE";
    private final static String RECYCLERVIEW_POSITION = "RECYCLERVIEW_POSITION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photosAlreadyLoaded = false;

        Button btn = findViewById(R.id.show_photos_btn);
        btn.setOnClickListener(this::showPhotos);

        initializeFragmentContainer();
        initializeRecyclerView();
        initializeViewModel();
        initializeSetttingsAfterGlobalLayout(savedInstanceState);

        registerContentObserver();
    }

    private void registerContentObserver() {
        externalContentObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String path = getRealPathFromURI(MainActivity.this, uri);

                List<String> photosInApp = viewModel.photosProvider.photosUri.getValue();
                if (path != null) { // add
                    if (!photosInApp.contains(path)) {
                        int position = ((LinearLayoutManager) recyclerView.getLayoutManager())
                                .findFirstVisibleItemPosition();
                        photosInApp.add(position, path);
                        recyclerAdapter.notifyItemInserted(0);
                        if (position == 0) {
                            recyclerView.scrollToPosition(0);
                        }
                    }
                } else { //remove
                    // removed images come with "empty" uris: "content://media" - so query will fail
                    // added images come with external name:  "content://media/external/images/media/..."
                    List<String> removedPaths = viewModel.photosProvider.findRemoved(viewModel.getPhotosObject().getValue());
                    for (String removedPath : removedPaths) {
                        int position = photosInApp.indexOf(removedPath);
                        photosInApp.remove(removedPath);
                        recyclerAdapter.notifyItemRemoved(position);
                    }
                }
            }
        };

        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true, externalContentObserver);
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        try (Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null)) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } catch (NullPointerException ex) {
            // removed images come with "empty" uris: "content://media" - so query will fail
            // and cursor = null
            return null;
        }
    }


    public void unregisterContentObserver() {
        getContentResolver().unregisterContentObserver(externalContentObserver); // memory leak if forget to unregister
    }

    private void initializeViewModel() {
        PhotosProvider photosProvider = new PhotosProvider(this, AsyncTask.THREAD_POOL_EXECUTOR);
        viewModel = ViewModelProviders.of(this, new PhotosViewModelFactory(photosProvider)).get(PhotosViewModel.class);
    }

    private void initializeRecyclerView() {
        recyclerView = findViewById(R.id.photos_grid_recycler_view);
        recyclerView.setHasFixedSize(true);

        ActivityManager.MemoryInfo memoryInfo = getAvailableMemory();
        if (!memoryInfo.lowMemory) {
            recyclerView.setItemViewCacheSize(RECYCLER_VIEW_CACHE_SIZE);
        }
    }

    private void initializeFragmentContainer() {
        fragmentContainer = findViewById(R.id.fragment_container);
        fragmentContainer.setVisibility(View.INVISIBLE);
        fragmentManager = getSupportFragmentManager();
    }

    private void initializeSetttingsAfterGlobalLayout(Bundle savedInstanceState) {
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                setRecyclerView();
                observePhotosChanges();
                observeShowPhotosState();

                if (savedInstanceState != null) {
                    updateRecyclerViewPosition();
                    updateFragmentVisibility();
                }
            }

            private void updateFragmentVisibility() {
                // update state of fragment
                if (viewModel.getIsFragmentDisplayed().getValue()) {
                    fragmentContainer.setVisibility(View.VISIBLE);
                }
            }

            private void updateRecyclerViewPosition() {
                // update state of recycler position
                recyclerView.getLayoutManager().
                        onRestoreInstanceState(savedInstanceState.getParcelable(RECYCLERVIEW_POSITION));
            }

            private void observeShowPhotosState() {
                // update state of show-photos button on rotate
                viewModel.getShowPhotosButtonState().observe(MainActivity.this, isShowPressed -> {
                    if (isShowPressed && !photosAlreadyLoaded && isPermissionGranted()) {
                        viewModel.loadPhotos();
                    }
                });
            }

            private void observePhotosChanges() {
                // update state of photos
                viewModel.getPhotosObject().observe(MainActivity.this, photos -> {
                    recyclerAdapter.submitList(photos);
                    recyclerAdapter.notifyDataSetChanged();
                });
            }

            private void setRecyclerView() {
                setRecyclerViewLayoutManager();
                setRecyclerViewAdapter();
            }

            private void setRecyclerViewAdapter() {
                recyclerAdapter = new RecyclerAdapter(MainActivity.this, viewModel, gridImageWidth,
                        fragmentManager, fragmentContainer);
                recyclerView.setAdapter(recyclerAdapter);
            }

            private void setRecyclerViewLayoutManager() {
                // calculate image width and photos-per-row
                int viewWidth = recyclerView.getWidth();
                int photosPerRow = viewWidth / INITIAL_GRID_IMAGE_WIDTH;
                gridImageWidth = getGridImageWidth(viewWidth, photosPerRow);

                recyclerView.setLayoutManager(new GridLayoutManager(getBaseContext(), photosPerRow));
            }

            private int getGridImageWidth(int viewWidth, int photosPerRow) {
                return INITIAL_GRID_IMAGE_WIDTH +
                        (viewWidth - photosPerRow * INITIAL_GRID_IMAGE_WIDTH) / photosPerRow;
            }
        });
    }

    private ActivityManager.MemoryInfo getAvailableMemory() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    @Override
    protected void onDestroy() {
        unregisterContentObserver();
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
            Snackbar snackbar = Snackbar.make(findViewById(R.id.coordinatorLayout),
                    "We need to access your photos", Snackbar.LENGTH_INDEFINITE);
            snackbar.setAction("Ok", view -> doRequestPermission());
            snackbar.show();
        } else {
            doRequestPermission();
        }
    }

    private void doRequestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadPhotos();
                } else {
                    Toast.makeText(this, "No permission. Abort", Toast.LENGTH_LONG).show();
                }
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
