package org.schabi.newpipe.manager

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder

import com.google.android.play.core.splitinstall.SplitInstallManagerFactory

import android.util.Log
import androidx.work.*
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.android.play.core.tasks.OnFailureListener
import com.google.android.play.core.tasks.OnSuccessListener
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager
import android.widget.Toast
import java.lang.Exception


class CheckWorker(context: Context, workerParameters: WorkerParameters):
        Worker(context, workerParameters){

    override fun doWork(): Result {
        ModuleManager.checkStorage(applicationContext)
        ModuleManager.checkPackage(applicationContext)
        return Result.success()
    }
}

class ModuleManager : Service() {

    private val storageReceiver:BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null)
                handleStorageCheck(context, intent)
        }
    }

    private val packageReceiver:BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null) {
                checkPackage(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val request = PeriodicWorkRequest
                .Builder(CheckWorker::class.java, 15, TimeUnit.MINUTES)
                .setConstraints(Constraints.NONE)
                .build()
        WorkManager.getInstance(this).enqueue(request)

        val storageFilter = IntentFilter()
        storageFilter.addAction(Intent.ACTION_DEVICE_STORAGE_LOW)
        storageFilter.addAction(Intent.ACTION_DEVICE_STORAGE_OK)
        registerReceiver(storageReceiver, storageFilter)

        val packageFilter = IntentFilter()
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageFilter.addDataScheme("package")
        registerReceiver(packageReceiver, packageFilter)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private var storageWasLow = false;

        private const val DOWNLOAD_INIT_ACTIVITY_NAME =
                "org.schabi.newpipe.downloader.download.DownloadInitActivity"
        private const val DOWNLOAD_STUB_ACTIVITY_NAME =
                "org.schabi.newpipe.download.DownloadStubActivity"
        private const val DOWNLOAD_ACTIVITY_NAME =
                "org.schabi.newpipe.downloader.download.DownloadActivity"
        private const val LOAD_YOUTUBE_SERVICE_NAME =
                "org.schabi.newpipe.youtubesupport.LoadYoutubeSupportService"
        const val DOWNLOADER_MODULE =
                "downloader"
        const val YOUTUBE_SUPPORT_MODULE =
                "youtubesupport"
        const val YOUTUBE_ORIG_APP =
                //"com.something.that.does.not.exist"
                "org.schabi.newpipe.debug"

        @Volatile
        private var restartCallback: Runnable? = null

        @JvmStatic fun loadExtServices(context: Context) {
            val manager = SplitInstallManagerFactory.create(context)
            if (manager.installedModules.contains(YOUTUBE_SUPPORT_MODULE)) {
                val loadYoutubeIntent = Intent().setClassName(context.packageName, LOAD_YOUTUBE_SERVICE_NAME).putExtra("RESET", true)
                context.startService(loadYoutubeIntent)
            } else
                NewPipe.reset()
        }

        @JvmStatic fun start(context: Context) {
            val startIntent = Intent().setClass(context, ModuleManager::class.java)
            context.startService(startIntent)
        }

        @JvmOverloads @JvmStatic fun tryInstallModule(context: Context, name: String,
                                                      onSuccessListener : OnSuccessListener<Int>? = null,
                                                      onFailureListener: OnFailureListener? = null) {
            val manager = SplitInstallManagerFactory.create(context)
            val requestBuilder = SplitInstallRequest.newBuilder()
            requestBuilder.addModule(name)
            val request = requestBuilder.build()
            val install = manager.startInstall(request)
            if (onSuccessListener != null)
                install.addOnSuccessListener(onSuccessListener)
            if (onFailureListener != null)
                install.addOnFailureListener(onFailureListener)
        }

        @JvmStatic fun tryRemoveModules(context: Context, modules: List<String>) {
            val manager = SplitInstallManagerFactory.create(context)
            manager.deferredUninstall(modules)
        }

        @JvmStatic fun tryRemoveModule(context: Context, module: String) {
            tryRemoveModules(context, listOf(module))
        }

        @JvmStatic fun handleStorageCheck(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_DEVICE_STORAGE_LOW)
                tryRemoveModule(context, DOWNLOADER_MODULE)
            if (intent.action == Intent.ACTION_DEVICE_STORAGE_OK)
                tryInstallModule(context, DOWNLOADER_MODULE,
                        object: OnSuccessListener<Int> {
                            override fun onSuccess(result: Int) {
                                Toast.makeText(context, "Successfully installed, $result", Toast.LENGTH_LONG).show()
                            }
                        },
                        object: OnFailureListener {
                            override fun onFailure(e: Exception?) {
                                Toast.makeText(context, "Adaptation error: $e", Toast.LENGTH_LONG).show()
                            }
                        })

        }

        @JvmStatic fun checkStorage(context: Context) {
            val intent = Intent()
            if (context.cacheDir.usableSpace * 100 / context.cacheDir.totalSpace <= 10) {
                intent.action = Intent.ACTION_DEVICE_STORAGE_LOW
                storageWasLow = true
            } else {
                if (storageWasLow) {
                    intent.action = Intent.ACTION_DEVICE_STORAGE_OK
                    storageWasLow = false
                }
            }
            handleStorageCheck(context, intent)
        }

        @JvmStatic fun checkPackage(context: Context) {
            val packageManager: PackageManager = context.packageManager
            var installed = false
            try {
                packageManager.getPackageInfo(YOUTUBE_ORIG_APP, 0)
                installed = true
            } catch (e: PackageManager.NameNotFoundException) {
                installed = false
            }
            if (SplitInstallManagerFactory.create(context).installedModules.contains(YOUTUBE_SUPPORT_MODULE)) {
                if (!installed)
                    tryRemoveModule(context, YOUTUBE_SUPPORT_MODULE)
            } else {
                if (installed)
                    tryInstallModule(context, YOUTUBE_SUPPORT_MODULE,
                            object: OnSuccessListener<Int> {
                                override fun onSuccess(result: Int) {
                                    Toast.makeText(context, "Successfully installed, $result", Toast.LENGTH_LONG).show()
                                }
                            },
                            object: OnFailureListener {
                                override fun onFailure(e: Exception?) {
                                    Toast.makeText(context, "Adaptation error: $e", Toast.LENGTH_LONG).show()

                                }
                            })
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
