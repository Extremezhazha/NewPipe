package org.schabi.newpipe.download;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.schabi.newpipe.R;
import org.schabi.newpipe.util.ThemeHelper;

public class DownloadStubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);
        setContentView(R.layout.activity_download_stub);
    }
}
