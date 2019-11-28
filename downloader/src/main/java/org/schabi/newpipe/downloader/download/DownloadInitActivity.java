package org.schabi.newpipe.downloader.download;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.schabi.newpipe.downloader.R;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.fragments.detail.VideoDetailFragment;
import org.schabi.newpipe.util.ListHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.util.List;

public class DownloadInitActivity extends AppCompatActivity {

    StreamInfo currentInfo = null;
    List<VideoStream> sortedVideoStreams = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.setTheme(this);
        setContentView(R.layout.activity_download_init);

        Intent intent = getIntent();

        try {
            Log.d("Whoa", "Here we go?");

            //currentInfo = (StreamInfo) ObjectSideChannel.getInstance().get("1");
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

            FragmentManager fm = getSupportFragmentManager();

            downloadDialog.show(fm, "downloadDialog");

            fm.executePendingTransactions();

            //downloadDialog.getDialog().setOnDismissListener(dialog -> finish());
            //finish();
            downloadDialog.setDialogListener(dialog -> finish());
        } catch (Exception e) {
            Log.d("Whoa", "there");
            Log.d("Whoa", e.toString());
            /*
            ErrorActivity.ErrorInfo info = ErrorActivity.ErrorInfo.make(UserAction.UI_ERROR,
                    ServiceList.all()
                            .get(currentInfo
                                    .getServiceId())
                            .getServiceInfo()
                            .getName(), "",
                    R.string.could_not_setup_download_menu);

            ErrorActivity.reportError(getActivity(),
                    e,
                    getActivity().getClass(),
                    getActivity().findViewById(android.R.id.content), info);*/
        }
    }
}
