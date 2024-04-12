package com.example.myexoplayerapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C.TRACK_TYPE_TEXT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.myexoplayerapp.databinding.ActivityPlayerBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

import com.example.myexoplayerapp.util.checkPermission
import com.example.myexoplayerapp.util.requestAllPermissions
import com.example.myexoplayerapp.util.shouldRequestPermissionRationale
import com.example.myexoplayerapp.util.showSnackbar
import com.example.myexoplayerapp.util.LogDump.Companion.writeLogCat

/**
 * A fullscreen activity to play audio or video streams.
 */
var audioList: ArrayList<AudioSongs>? = null
private const val TAG = "myInfo"
@UnstableApi class PlayerActivity : AppCompatActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null

    private lateinit var playerView: PlayerView
    private var myMsgResult = false
    private lateinit var sessionToken : SessionToken
//    private lateinit var mService : PlaybackService
//    private var mBound : Boolean = false

    companion object {
        const val PERMISSION_REQUEST_STORAGE = 0
    }

    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            Log.i(TAG,"permission value if < TIRAMISU not dealt with")
            TODO("VERSION.SDK_INT < TIRAMISU")
    }

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        Log.i(TAG,"viewBinding - lazy call layout inflater")
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    /** Defines callbacks for service binding, passed to bindService().  */
    //SEE: https://developer.android.com/develop/background-work/services/bound-services#Basics
//    private val connection = object : ServiceConnection {
//
//        override fun onServiceConnected(className: ComponentName, service: IBinder) {
//            // We've bound to LocalService, cast the IBinder and get LocalService instance.
//            val binder = service as PlaybackService.LocalBinder
//            mService = binder.getService()
//            mBound = true
//        }
//
//        override fun onServiceDisconnected(arg0: ComponentName) {
//            mBound = false
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.container)       //changed from .root
        Log.i(TAG, "\n\nonCreate() - Start of session - Dump Logcat\n")
        playerView = viewBinding.videoView

        audioList = ArrayList()

        //requestPermission()
        // Check if the storage permission has been granted
        if (checkPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted
            startDownloading()
        } else {
            //Requested permission.
            // Permission has not been granted and must be requested.
            requestStoragePermission()
        }
        //PlaybackService.LocalBinder = bindService()

        //SEE:https://stackoverflow.com/questions/72634225/onbackpressed-is-deprecated-what-is-the-alternative
//        if (Build.VERSION.SDK_INT >= 33) {
//            onBackInvokedDispatcher.registerOnBackInvokedCallback(
//                OnBackInvokedDispatcher.PRIORITY_DEFAULT
//            ) {
//                exitOnBackPressed()
//            }
//        } else {
//            onBackPressedDispatcher.addCallback(
//                this,
//                object : OnBackPressedCallback(true) {
//                    override fun handleOnBackPressed() {
//
//                        Log.i(TAG, "handleOnBackPressed: Exit")
//                        exitOnBackPressed()
//                    }
//                })
//        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            // Request for camera permission.
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownloading()
            } else {
                // Permission request was denied.
                viewBinding.container.showSnackbar(R.string.storage_permission_denied, Snackbar.LENGTH_SHORT)
            }
        }
    }

    private fun requestStoragePermission() {
        // Permission has not been granted and must be requested.
        if (shouldRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO)) {
            // Provide an additional rationale to the user if the permission was not granted
            viewBinding.container.showSnackbar(
                R.string.storage_access_required,
                Snackbar.LENGTH_INDEFINITE, R.string.ok
            ) {
                requestAllPermissions(permissions, PERMISSION_REQUEST_STORAGE)
            }
        } else {
            // Request the permission with array.
            requestAllPermissions(permissions, PERMISSION_REQUEST_STORAGE)
        }
    }

    private fun startDownloading() {
        // do download stuff here
        if (audioList != null)
            Log.i(TAG, "startDownLoading - loadAudio() called, audioList.count() = " + audioList!!.count())
        else
            Log.i(TAG, "startDownLoading - loadAudio() called, audioList == null")

        loadAudio()
        viewBinding.container.showSnackbar(R.string.loadingAudio, Snackbar.LENGTH_LONG)
    }

    @SuppressLint("Range")
    fun myNewGetAudioFileCount(): Int {
//See:https://stackoverflow.com/questions/11982195/how-to-access-music-files-from-android-programatically
//See:https://stackoverflow.com/questions/21403221/how-to-get-all-audio-video-files?rq=4

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.AudioColumns.DATA,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ALBUM,
            MediaStore.Audio.ArtistColumns.ARTIST
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " !=0 "
        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            null,
            null
        )

        if (cursor != null && cursor.count > 0) {
            while (cursor.moveToNext()) {
                val data =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                val title =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))
                val album =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))
                val artist =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))

                //Log.i("SONG data = ", data)     // example data string :"/storage/emulated/0/Music/**.mp3"
                // Save to audioList
                audioList!!.add(AudioSongs(data, title, album, artist))
            }
            audioList!!.shuffle()
        }

        val songCount = cursor!!.count
        //myShowErrorDlg("Songs found = $songCount")
        cursor.close()

        return songCount
    }

    private fun myShowErrorDlg(errMsg: String) {
        // build alert dialog
        val dialogBuilder = AlertDialog.Builder(this)

        // set message of alert dialog
        dialogBuilder.setMessage(errMsg)
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Grant permission", DialogInterface.OnClickListener {
                    _, _ ->
                myMsgResult = true//finish()
            })
            // negative button text and action
            .setNegativeButton("Deny Access", DialogInterface.OnClickListener {
                    dialog, id ->
                myMsgResult = false
                //dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("Application needs permission to access Music Folder")
        // show alert dialog
        alert.show()
    }

    private fun loadAudio() {
        try {
            myNewGetAudioFileCount()
        } catch (e: Exception) {
            myShowErrorDlg("Error = " + e.message)
            // Cannot use Toast in catch stmt - Toast.makeText(this, " Error = " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeMediaList() {    //(songs : ArrayList<AudioSongs>) {
        val sList : MutableList<MediaItem> = ArrayList()

        for ((cnt, _) in audioList!!.withIndex())
        {
            val fs = audioList?.get(cnt)
            val fsUri = Uri.parse(fs?.data)

            sList.add(MediaItem.fromUri(fsUri))
        }
    }

    private fun initializeController() {
        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            ).buildAsync()

        controllerFuture.addListener({ setController() }, MoreExecutors.directExecutor())
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    private fun setController() {
        val controller = this.controller ?: return

        playerView.player = controller

        //playerView.setShowSubtitleButton(controller.currentTracks.isTypeSupported(TRACK_TYPE_TEXT))

        controller.addListener(
            object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    playerView.setShowSubtitleButton(tracks.isTypeSupported(TRACK_TYPE_TEXT))
                    Log.i(TAG, "Activity() - setController(), onTracksChanged() Player.Listener")
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateString: String = when (playbackState) {
                        Player.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                        Player.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                        Player.STATE_READY -> "ExoPlayer.STATE_READY     -"
                        Player.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                        else -> "UNKNOWN_STATE             -"
                    }
                    Log.i(TAG, "Activity() - setController(), onPlaybackStateChanged() Player.Listener: changed state to $stateString")
                }
            }
        )
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    public override fun  onStart() {
        super.onStart()
//        // Bind to PlaybackService
//        Intent(this, PlaybackService::class.java).also { intent ->
//            bindService(intent, connection, Context.BIND_AUTO_CREATE)
//        }

        if (Util.SDK_INT > 23) {
            if (audioList != null && audioList!!.isNotEmpty()) {
                //initializeMediaList()
                sessionToken = SessionToken(this,ComponentName(this, PlaybackService::class.java))
                initializeController()

                Log.i(TAG, "1 Activity() - onStart(), create SessionToken & initializeController()")
            } else
            {
                if (audioList == null)
                {
                    audioList = ArrayList()
                    Log.i(TAG, "2 Activity() - onStart() audioList == null, therefore recreate it")
                }
                if (audioList!!.isEmpty())
                {
                    initializeMediaList()
                    sessionToken = SessionToken(this,ComponentName(this, PlaybackService::class.java))
                    initializeController()

                    Log.i(TAG, "3 Activity() - onStart(), initializeMediaList(), create SessionToken & initializeController()")
                }

            }

        }
    }

    public override fun onResume() {
        super.onResume()
        //hideSystemUi()
        if (Util.SDK_INT <= 23 ) {
            sessionToken = SessionToken(this,ComponentName(this, PlaybackService::class.java))  //added
            initializeController()

            Log.i(TAG,"Activity onResume() controller re-initialize")
        } else
            Log.i(TAG,"Activity onResume() doing nothing if SDK_INT > 23")
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releaseController()

            Log.i(TAG,"Activity onPause() - release controller,  SDK <= 23")
        } else
            Log.i(TAG,"Activity onPause() doing nothing if SDK_INT > 23")

//        if (isFinishing) {
//            Log.i(TAG,"Activity onPause() - isFinishing == true")
//        }

    }

    public override fun onStop() {
        super.onStop()

//        unbindService(connection)
//        mBound = false

        if (Util.SDK_INT > 23) {
            //releaseController()
            //PlaybackService.stop(this)
            Log.i(TAG,"Activity onStop() - releaseController() & MediaController.releaseFuture()")
        } else
            Log.i(TAG,"Activity onStop() - doing nothing if SDK_INT > 23")
    }

//    fun exitOnBackPressed() {
//        Log.i(TAG,"Activity exitOnBackPressed() - trying to override onDestroy()")
//    }

    public override fun onDestroy() {
        //stopService(Intent(this@PlayerActivity, PlaybackService::class.java))
        Log.i(TAG,"Activity onDestroy()")
        try {
            writeLogCat(this@PlayerActivity)
        } catch (e: Exception)
        {
            Log.e(TAG, "Exception thrown in writeLogCat")
        }

        //PlaybackService.stop(this)

        super.onDestroy()
    }
}