package us.shandian.giga.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nononsenseapps.filepicker.Utils;

import org.schabi.newpipe.R;
import org.schabi.newpipe.settings.NewPipeSettings;
import org.schabi.newpipe.util.FilePickerActivityHelper;
import org.schabi.newpipe.util.ThemeHelper;

import java.io.File;
import java.io.IOException;

import us.shandian.giga.get.DownloadMission;
import us.shandian.giga.service.DownloadManager;
import us.shandian.giga.service.DownloadManagerService;
import us.shandian.giga.io.StoredFileHelper;
import us.shandian.giga.ui.adapter.MissionAdapter;

public class MissionsFragment extends Fragment {

    private static final int SPAN_SIZE = 2;
    private static final int REQUEST_DOWNLOAD_SAVE_AS = 0x1230;

    private SharedPreferences mPrefs;
    private boolean mLinear;
    private MenuItem mSwitch;
    private MenuItem mClear = null;
    private MenuItem mStart = null;
    private MenuItem mPause = null;

    private RecyclerView mList;
    private View mEmpty;
    private MissionAdapter mAdapter;
    private GridLayoutManager mGridManager;
    private LinearLayoutManager mLinearManager;
    private Context mContext;

    private DownloadManagerService.DownloadManagerBinder mBinder;
    private boolean mForceUpdate;

    private DownloadMission unsafeMissionTarget = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mBinder = (DownloadManagerService.DownloadManagerBinder) binder;
            mBinder.clearDownloadNotifications();

            mAdapter = new MissionAdapter(mContext, mBinder.getDownloadManager(), mEmpty);
            mAdapter.deleterLoad(getView());

            mAdapter.setRecover(MissionsFragment.this::recoverMission);

            setAdapterButtons();

            mBinder.addMissionEventListener(mAdapter);
            mBinder.enableNotifications(false);

            updateList();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // What to do?
        }


    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.missions, container, false);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mLinear = mPrefs.getBoolean("linear", false);

        // Bind the service
        mContext.bindService(new Intent(mContext, DownloadManagerService.class), mConnection, Context.BIND_AUTO_CREATE);

        // Views
        mEmpty = v.findViewById(R.id.list_empty_view);
        mList = v.findViewById(R.id.mission_recycler);

        // Init layouts managers
        mGridManager = new GridLayoutManager(getActivity(), SPAN_SIZE);
        mGridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case DownloadManager.SPECIAL_PENDING:
                    case DownloadManager.SPECIAL_FINISHED:
                        return SPAN_SIZE;
                    default:
                        return 1;
                }
            }
        });
        mLinearManager = new LinearLayoutManager(getActivity());

        setHasOptionsMenu(true);

        return v;
    }

    /**
     * Added in API level 23.
     */
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Bug: in api< 23 this is never called
        // so mActivity=null
        // so app crashes with null-pointer exception
        mContext = context;
    }

    /**
     * deprecated in API level 23,
     * but must remain to allow compatibility with api<23
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mContext = activity;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBinder == null || mAdapter == null) return;

        mBinder.removeMissionEventListener(mAdapter);
        mBinder.enableNotifications(true);
        mContext.unbindService(mConnection);
        mAdapter.deleterDispose(true);

        mBinder = null;
        mAdapter = null;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        mSwitch = menu.findItem(R.id.switch_mode);
        mClear = menu.findItem(R.id.clear_list);
        mStart = menu.findItem(R.id.start_downloads);
        mPause = menu.findItem(R.id.pause_downloads);

        if (mAdapter != null) setAdapterButtons();

        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.switch_mode:
                mLinear = !mLinear;
                updateList();
                return true;
            case R.id.clear_list:
                AlertDialog.Builder prompt = new AlertDialog.Builder(mContext);
                prompt.setTitle(R.string.clear_finished_download);
                prompt.setMessage(R.string.confirm_prompt);
                prompt.setPositiveButton(android.R.string.ok, (dialog, which) -> mAdapter.clearFinishedDownloads());
                prompt.setNegativeButton(R.string.cancel, null);
                prompt.create().show();
                return true;
            case R.id.start_downloads:
                item.setVisible(false);
                mBinder.getDownloadManager().startAllMissions();
                return true;
            case R.id.pause_downloads:
                item.setVisible(false);
                mBinder.getDownloadManager().pauseAllMissions(false);
                mAdapter.ensurePausedMissions();// update items view
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateList() {
        if (mLinear) {
            mList.setLayoutManager(mLinearManager);
        } else {
            mList.setLayoutManager(mGridManager);
        }

        // destroy all created views in the recycler
        mList.setAdapter(null);
        mAdapter.notifyDataSetChanged();

        // re-attach the adapter in grid/lineal mode
        mAdapter.setLinear(mLinear);
        mList.setAdapter(mAdapter);

        if (mSwitch != null) {
            boolean isLight = ThemeHelper.isLightThemeSelected(mContext);
            int icon;

            if (mLinear)
                icon = isLight ? R.drawable.ic_grid_black_24dp : R.drawable.ic_grid_white_24dp;
            else
                icon = isLight ? R.drawable.ic_list_black_24dp : R.drawable.ic_list_white_24dp;

            mSwitch.setIcon(icon);
            mSwitch.setTitle(mLinear ? R.string.grid : R.string.list);
            mPrefs.edit().putBoolean("linear", mLinear).apply();
        }
    }

    private void setAdapterButtons() {
        if (mClear == null || mStart == null || mPause == null) return;

        mAdapter.setClearButton(mClear);
        mAdapter.setMasterButtons(mStart, mPause);
    }

    private void recoverMission(@NonNull DownloadMission mission) {
        unsafeMissionTarget = mission;

        if (NewPipeSettings.useStorageAccessFramework(mContext)) {
            StoredFileHelper.requestSafWithFileCreation(
                    MissionsFragment.this,
                    REQUEST_DOWNLOAD_SAVE_AS,
                    mission.storage.getName(),
                    mission.storage.getType()
            );

        } else {
            File initialSavePath;
            if (DownloadManager.TAG_VIDEO.equals(mission.storage.getType()))
                initialSavePath = NewPipeSettings.getDir(Environment.DIRECTORY_MOVIES);
            else
                initialSavePath = NewPipeSettings.getDir(Environment.DIRECTORY_MUSIC);

            initialSavePath = new File(initialSavePath, mission.storage.getName());
            startActivityForResult(
                    FilePickerActivityHelper.chooseFileToSave(mContext, initialSavePath.getAbsolutePath()),
                    REQUEST_DOWNLOAD_SAVE_AS
            );
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mAdapter != null) {
            mAdapter.deleterDispose(false);
            mForceUpdate = true;
            mBinder.removeMissionEventListener(mAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdapter != null) {
            mAdapter.deleterResume();

            if (mForceUpdate) {
                mForceUpdate = false;
                mAdapter.forceUpdate();
            }

            mBinder.addMissionEventListener(mAdapter);
            mAdapter.checkMasterButtonsVisibility();
        }
        if (mBinder != null) mBinder.enableNotifications(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAdapter != null) mAdapter.onPaused();
        if (mBinder != null) mBinder.enableNotifications(true);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_DOWNLOAD_SAVE_AS || resultCode != Activity.RESULT_OK) return;

        if (unsafeMissionTarget == null || data.getData() == null) {
            return;
        }

        try {
            Uri fileUri = data.getData();
            if (fileUri.getAuthority() != null && FilePickerActivityHelper.isOwnFileUri(mContext, fileUri)) {
                fileUri = Uri.fromFile(Utils.getFileForUri(fileUri));
            }

            String tag = unsafeMissionTarget.storage.getTag();
            unsafeMissionTarget.storage = new StoredFileHelper(mContext, null, fileUri, tag);
            mAdapter.recoverMission(unsafeMissionTarget);
        } catch (IOException e) {
            Toast.makeText(mContext, R.string.general_error, Toast.LENGTH_LONG).show();
        }
    }
}
