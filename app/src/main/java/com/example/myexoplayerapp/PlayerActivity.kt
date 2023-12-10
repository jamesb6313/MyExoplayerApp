package com.example.myexoplayerapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C.TRACK_TYPE_TEXT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
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

//import kotlinx.android.synthetic.main.activity_player.container
//import kotlinx.android.synthetic.main.activity_player.buttonDownloadFile

/**
 * A fullscreen activity to play audio or video streams.
 */
var audioList: ArrayList<AudioSongs>? = null
private const val TAG = "PlayerActivity"
class PlayerActivity : AppCompatActivity() {
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private val controller: MediaController?
        get() = if (controllerFuture.isDone) controllerFuture.get() else null

    private lateinit var playerView: PlayerView
    //private var player: ExoPlayer? = null
    //private val MY_PERMISSIONS_READ_STORAGE = 42
    private var myMsgResult = false
    //var serviceBound = false
    //var initialSongIndex = 0
    //val Broadcast_PLAY_NEW_AUDIO = "com.example.audio2023.PlayNewAudio"
    private lateinit var sessionToken : SessionToken


/*    companion object {
        var PERMISSIONS = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    }*/
companion object {
    const val PERMISSION_REQUEST_STORAGE = 0
}
    private val permissions = arrayOf(Manifest.permission.READ_MEDIA_AUDIO)

    //private val playbackStateListener: Player.Listener = playbackStateListener()

    private val viewBinding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityPlayerBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        playerView = viewBinding.videoView
//        playerView.setOnClickListener { _ ->
//            run {
//                val controller = this.controller ?: return@run
//                if (controller.currentMediaItemIndex == position) {
//                    controller.playWhenReady = !controller.playWhenReady
//                    if (controller.playWhenReady) {
//                        playerView.hideController()
//                    }
//                } else {
//                    controller.seekToDefaultPosition(/* mediaItemIndex= */ position)
//                    mediaItemListAdapter.notifyDataSetChanged()
//                }
//            }
//        }
        audioList = ArrayList<AudioSongs>()

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
        loadAudio()
        viewBinding.container.showSnackbar(R.string.loadingAudio, Snackbar.LENGTH_LONG)
    }

//    //**********************
//// Register the permissions callback, which handles the user's response to the
//    // system permissions dialog. Save the return value, an instance of
//    // ActivityResultLauncher. You can use either a val, as shown in this snippet,
//    // or a lateinit var in your onAttach() or onCreate() method.
//    private val requestPermissionLauncher =
//        registerForActivityResult(
//            ActivityResultContracts.RequestPermission()
//        ) { isGranted: Boolean ->
//            if (isGranted) {
//                // Permission is granted. Continue the action or workflow in your
//                // app.
//                Log.i(TAG, "Permission is granted")
//                loadAudio()
//            } else {
//                // Explain to the user that the feature is unavailable because the
//                // feature requires a permission that the user has denied. At the
//                // same time, respect the user's decision. Don't link to system
//                // settings in an effort to convince the user to change their
//                // decision.
//                Log.i(TAG, "Permission is denied - Cannot access Music Folder")
//                myShowErrorDlg(getString(R.string.permissions_required))
//            }
//        }
//
//    private fun requestPermission() {
//        when {
//            ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                // You can use the API that requires the permission.
//                Toast.makeText(this@PlayerActivity, "Permission was Granted", Toast.LENGTH_LONG).show()
//
//                loadAudio()
//            }
//
//            ActivityCompat.shouldShowRequestPermissionRationale(
//                this,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) -> {
//                //Additional rationale should be displayed
//
//                Toast.makeText(this@PlayerActivity, "Permission require", Toast.LENGTH_LONG).show()
////                    myShowErrorDlg(getString(R.string.permissions_required))
////                    if (myMsgResult) {
////                        // Permission has not been asked yet
//                        requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) //Ask permission
////                    }
//            } else -> {
//                //Toast.makeText(this@MainActivity, "Ask for Permission", Toast.LENGTH_SHORT).show()
//                // Permission has not been asked yet
//                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE) //Ask permission
//
//            }
//        }
//    }
    //

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

                Log.i("SONG data = ", data)     // example data string :"/storage/emulated/0/Music/**.mp3"
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

    fun myShowErrorDlg(errMsg: String) {
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

    //loadAudio
    //////////////////////////////////////////////////////////
    fun loadAudio() {
        try {
            myNewGetAudioFileCount()
        } catch (e: Exception) {
            myShowErrorDlg("Error = " + e.message)
// Cannot use Toast in catch stmt - Toast.makeText(this, " Error = " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    //***********************

// See: https://stackoverflow.com/questions/47046321/exoplayer-not-playing-local-file-on-local-storage?rq=3
//    @androidx.media3.common.util.UnstableApi
//    private fun buildMediaSource(uri: Uri): MediaSource? {
//        val mediaItem = MediaItem.fromUri(uri)
//        return ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context))
//            .createMediaSource(mediaItem)
//    }

    private fun initializeMediaList() {    //(songs : ArrayList<AudioSongs>) {
        val sList : MutableList<MediaItem> = ArrayList()

        for ((cnt, _) in audioList!!.withIndex())
        {
            val fs = audioList?.get(cnt)
            val fsUri = Uri.parse(fs?.data)

            sList.add(MediaItem.fromUri(fsUri))
        }
    }

    @androidx.media3.common.util.UnstableApi
    private fun initializeController() {
        controllerFuture =
            MediaController.Builder(
                this,
                sessionToken
            )
                .buildAsync()
        controllerFuture.addListener({ setController() }, MoreExecutors.directExecutor())
    }

    private fun releaseController() {
        MediaController.releaseFuture(controllerFuture)
    }

    @androidx.media3.common.util.UnstableApi
    private fun setController() {
        val controller = this.controller ?: return

        playerView.player = controller

//        updateCurrentPlaylistUI()
//        updateMediaMetadataUI(controller.mediaMetadata)
        playerView.setShowSubtitleButton(controller.currentTracks.isTypeSupported(TRACK_TYPE_TEXT))
//
        controller.addListener(
            object : Player.Listener {
//                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
//                    updateMediaMetadataUI(mediaItem?.mediaMetadata ?: MediaMetadata.EMPTY)
//                }

                override fun onTracksChanged(tracks: Tracks) {
                    playerView.setShowSubtitleButton(tracks.isTypeSupported(TRACK_TYPE_TEXT))
                    Log.i(TAG, "new controller - PlayerListener")
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateString: String = when (playbackState) {
                        Player.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                        Player.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                        Player.STATE_READY -> "ExoPlayer.STATE_READY     -"
                        Player.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                        else -> "UNKNOWN_STATE             -"
                    }
                    Log.i(TAG, "changed state to $stateString")
                }
            }
        )
    }


    @androidx.media3.common.util.UnstableApi
    public override fun  onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            if (audioList != null && audioList!!.isNotEmpty()) {
                initializeMediaList()
                sessionToken = SessionToken(this,ComponentName(this, PlaybackService::class.java))
                initializeController()
            }

        }
    }

    @androidx.media3.common.util.UnstableApi
    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if (Util.SDK_INT <= 23 ) {
            initializeController()
            Log.i(TAG,"Activity onResume() controller re-initialize")
        }
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, viewBinding.videoView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    @androidx.media3.common.util.UnstableApi
    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            //releasePlayer()
            //TODO test with next 2 lines - would need to be SDK <= 23
            releaseController()
            MediaController.releaseFuture(controllerFuture)

            Log.i(TAG,"Activity onPause() - release controller,  SDK <= 23")
        }
        if (isFinishing) {
            Log.i(TAG,"Activity onPause() - isFinishing == true")
        }
    }

    @androidx.media3.common.util.UnstableApi
    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            //releasePlayer()
            releaseController()
            MediaController.releaseFuture(controllerFuture)

            Log.i(TAG,"Activity onStop() - release controller")
        }
    }
    @androidx.media3.common.util.UnstableApi
    public override fun onDestroy() {
        stopService(Intent(this@PlayerActivity, PlaybackService::class.java))
        Log.i(TAG,"Activity onDestroy()")
        super.onDestroy()
    }

//    private var playWhenReady = true
//    private var mediaItemIndex = 0
//    private var playbackPosition = 0L

//    private fun releasePlayer() {
//        player?.let { exoPlayer ->
//            playbackPosition = exoPlayer.currentPosition
//            mediaItemIndex = exoPlayer.currentMediaItemIndex
//            playWhenReady = exoPlayer.playWhenReady
//            exoPlayer.removeListener(playbackStateListener)
//            exoPlayer.release()
//        }
//        player = null
//    }


}