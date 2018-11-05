// Copyright (c) 2018 Lightricks. All rights reserved.
// Created by David Refaeli.
package com.example.drefaeli.myphotos;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

public class PhotosViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private final PhotosProvider provider;

    public PhotosViewModelFactory(PhotosProvider provider) {
        this.provider = provider;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new PhotosViewModel(provider);
    }
}
