package com.example.etherapyii;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.wit.witsdk.modular.sensor.example.ble5.Bwt901ble;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<Bwt901ble> sensor1 = new MutableLiveData<>();
    private final MutableLiveData<Bwt901ble> sensor2 = new MutableLiveData<>();

    public void setSensor1(Bwt901ble sensor) {
        this.sensor1.setValue(sensor);
    }

    public LiveData<Bwt901ble> getSensor1() {
        return sensor1;
    }

    public void setSensor2(Bwt901ble sensor) {
        this.sensor2.setValue(sensor);
    }

    public LiveData<Bwt901ble> getSensor2() {
        return sensor2;
    }
}
