package com.mashiro.mobile.audio;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;
        KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        if (event != null && event.getAction() != KeyEvent.ACTION_DOWN) return;
        Playback playback = Playback.get();
        if (playback != null) {
            playback.toggle();
        } else {
            PlayerService.start(context);
        }
    }
}
