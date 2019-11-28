package org.schabi.newpipe.manager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory

import android.util.Log

class ModuleManager : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

    }

    fun registerRestart(callback: Runnable) {
        restartCallback = callback
    }

    companion object {
        private const val DOWNLOAD_INIT_ACTIVITY_NAME =
                "org.schabi.newpipe.downloader.download.DownloadInitActivity"
        private const val DOWNLOAD_STUB_ACTIVITY_NAME =
                "org.schabi.newpipe.download.DownloadStubActivity"
        private const val DOWNLOAD_ACTIVITY_NAME =
                "org.schabi.newpipe.downloader.download.DownloadActivity"
        private const val DOWNLOADER_MODULE =
                "downloader"

        @Volatile
        private var restartCallback: Runnable? = null

        @JvmStatic fun yes(context: Context) {
            val manager = SplitInstallManagerFactory.create(context)
            Log.d("Whoa", manager.installedModules.toString())
            //restartCallback.run();
            Log.d("Whoa", "Hi 1")
            //Intent ytservInt = new Intent();
            //ytservInt.setClassName("org.schabi.newpipe.youtubesupport", "org.schabi.newpipe.youtubesupport.LoadSupportService");
            //context.startService(ytservInt);
            if (manager.installedModules.contains("youtubesupport")) {
                val i1 = Intent()
                i1.setClassName(context.packageName, "org.schabi.newpipe.youtubesupport.LoadYoutubeSupportService")
                context.startService(i1)
            }
        }

        @JvmStatic private fun getDownloadDialogName(context: Context): String {
            return if (SplitInstallManagerFactory.create(context).installedModules.contains(DOWNLOADER_MODULE))
                DOWNLOAD_INIT_ACTIVITY_NAME
            else
                DOWNLOAD_STUB_ACTIVITY_NAME
        }

        @JvmStatic fun getDownloadDialogIntent(packageContext: Context): Intent {
            return Intent().setClassName(packageContext.packageName, getDownloadDialogName(packageContext))
        }

        @JvmStatic private fun getDownloadActivityName(context: Context): String {
            return if (SplitInstallManagerFactory.create(context).installedModules.contains(DOWNLOADER_MODULE))
                DOWNLOAD_ACTIVITY_NAME
            else
                DOWNLOAD_STUB_ACTIVITY_NAME
        }

        @JvmStatic fun getDownloadActivityIntent(packageContext: String, context: Context): Intent {
            return Intent().setClassName(packageContext, getDownloadActivityName(context))
        }

        @JvmStatic fun getDownloadActivityIntent(packageContext: Context): Intent {
            return getDownloadActivityIntent(packageContext.packageName, packageContext)
        }
    }
}
