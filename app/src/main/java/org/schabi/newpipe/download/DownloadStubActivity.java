package org.schabi.newpipe.download;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;

import org.schabi.newpipe.R;
import org.schabi.newpipe.manager.ModuleManager;
import org.schabi.newpipe.util.ThemeHelper;

public class DownloadStubActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);
        setContentView(R.layout.activity_download_stub);
        Button button = findViewById(R.id.button);
        button.setOnClickListener((view) -> {
            ModuleManager.tryInstallModule(this, ModuleManager.DOWNLOADER_MODULE,
                    (result) -> {
                        Toast.makeText(this, "Go" + result, Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    (e) -> {
                        Toast.makeText(this, "Hmmm" + e, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    );
        });
    }
}
