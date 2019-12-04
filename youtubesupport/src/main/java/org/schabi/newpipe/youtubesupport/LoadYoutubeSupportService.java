package org.schabi.newpipe.youtubesupport;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractorext.NewPipeExt;

public class LoadYoutubeSupportService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getBooleanExtra("RESET", false))
            NewPipe.reset();
        NewPipe.addServices(NewPipeExt.getAddList());
        stopSelf();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
