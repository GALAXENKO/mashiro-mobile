package com.mashiro.mobile;

import android.app.Application;

import com.mashiro.mobile.api.YandexApi;
import com.mashiro.mobile.audio.Playback;

public class App extends Application {
    public static YandexApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        Store.init(this);
        ImageLoader.init(this);
        api = new YandexApi();
        api.setToken(Store.token(), Store.uid());
        api.quality = Store.quality();
        Playback.init(this, api);
    }

    public static void applySession() {
        if (api == null) return;
        api.setToken(Store.token(), Store.uid());
        api.quality = Store.quality();
    }
}
