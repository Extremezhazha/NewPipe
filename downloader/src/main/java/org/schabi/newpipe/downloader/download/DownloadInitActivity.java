package org.schabi.newpipe.downloader.download;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.schabi.newpipe.RouterActivity;
import org.schabi.newpipe.downloader.R;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.fragments.detail.VideoDetailFragment;
import org.schabi.newpipe.report.ErrorActivity;
import org.schabi.newpipe.report.UserAction;
import org.schabi.newpipe.util.ListHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.List;

public class DownloadInitActivity extends AppCompatActivity {

    StreamInfo currentInfo = null;
    List<VideoStream> sortedVideoStreams = null;

    boolean isFromRouter = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);
        setContentView(R.layout.activity_download_init);

        Intent intent = getIntent();

        isFromRouter = intent.getBooleanExtra(RouterActivity.IS_FROM_ROUTER, false);

        try {
            currentInfo = (StreamInfo) intent.getSerializableExtra(VideoDetailFragment.CURRENT_INFO_TAG);
            sortedVideoStreams = ListHelper.getSortedStreamVideosList(
                    this,
                    currentInfo.getVideoStreams(),
                    currentInfo.getVideoOnlyStreams(),
                    false);

            int selectedVideoStreamIndex = intent.getIntExtra(VideoDetailFragment.SELECTED_INDEX_TAG, 0);

            DownloadDialog downloadDialog = DownloadDialog.newInstance(this, currentInfo);
            downloadDialog.setVideoStreams(sortedVideoStreams);
            downloadDialog.setAudioStreams(currentInfo.getAudioStreams());
            downloadDialog.setSelectedVideoStream(selectedVideoStreamIndex);
            downloadDialog.setSubtitleStreams(currentInfo.getSubtitles());

            downloadDialog.show(getSupportFragmentManager(), "downloadDialog");

            downloadDialog.setDialogListener(dialog -> {
                if (isFromRouter && RouterActivity.instance != null)
                    RouterActivity.instance.finish();
                finish();
            });
        } catch (Exception e) {

            ErrorActivity.ErrorInfo info = ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                    ServiceList.all()
                            .get(currentInfo
                                    .getServiceId())
                            .getServiceInfo()
                            .getName(), "",
                    org.schabi.newpipe.R.string.could_not_setup_download_menu);

            ErrorActivity.reportError(this,
                    e,
                    this.getClass(),
                    this.findViewById(android.R.id.content), info);
        }
    }
}
