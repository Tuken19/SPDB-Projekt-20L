package com.example.spdb.ui.properties;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PropertiesViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public PropertiesViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is properties fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}