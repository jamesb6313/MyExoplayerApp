package com.example.myexoplayerapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand

private const val TAG = "PlaybackService"
@androidx.media3.common.util.UnstableApi
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSessionService : MediaSessionService
    private lateinit var customCommands: List<CommandButton>

    companion object {
        private const val SEARCH_QUERY_PREFIX_COMPAT = "androidx://media3-session/playFromSearch"
        private const val SEARCH_QUERY_PREFIX = "androidx://media3-session/setMediaUri"
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON =
            "android.media3.session.demo.SHUFFLE_ON"
        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF =
            "android.media3.session.demo.SHUFFLE_OFF"
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "demo_session_notification_channel_id"
        private val immutableFlag = FLAG_IMMUTABLE
    }

    private var mediaSession: MediaSession? = null

    // If desired, validate the controller before returning the media session
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Create
    // Binder given to clients
    private val iBinder: IBinder = LocalBinder()

    // Create your Player and MediaSession in the onCreate lifecycle event
    @androidx.media3.common.util.UnstableApi
    override fun onCreate() {
        super.onCreate()
        customCommands =
            listOf(
                getShuffleCommandButton(
                    SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON, Bundle.EMPTY)
                ),
                getShuffleCommandButton(
                    SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF, Bundle.EMPTY)
                )
            )
        initializeSessionAndPlayer()
        setListener(serviceListener())

    }
    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (Util.SDK_INT < 26 || notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }

    private fun serviceListener() = object : Listener {
        @SuppressLint("MissingPermission")
        override fun onForegroundServiceStartNotAllowedException() {
            val notificationManagerCompat = NotificationManagerCompat.from(this@PlaybackService)
            ensureNotificationChannel(notificationManagerCompat)
            val pendingIntent =
                TaskStackBuilder.create(this@PlaybackService).run {
                    addNextIntent(Intent(this@PlaybackService, PlayerActivity::class.java))
                    getPendingIntent(0, immutableFlag or FLAG_UPDATE_CURRENT)
                }
            val builder =
                NotificationCompat.Builder(this@PlaybackService, CHANNEL_ID)
                    .setContentIntent(pendingIntent)
                    //.setSmallIcon(R.drawable.media3_notification_small_icon)
                    .setContentTitle(getString(R.string.notification_content_title))
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_content_text))
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
//            if (ActivityCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.POST_NOTIFICATIONS
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return
//            }
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private var playWhenReady = true
    private var mediaItemIndex = 0
    private var playbackPosition = 0L
//    private val playbackStateListener: Player.Listener = playbackStateListener()

//    private fun playbackStateListener() = object : Player.Listener {
//        override fun onPlaybackStateChanged(playbackState: Int) {
//            val stateString: String = when (playbackState) {
//                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
//                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
//                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
//                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
//                else -> "UNKNOWN_STATE             -"
//            }
//            Log.d(TAG, "changed state to $stateString")
//        }
//    }

    private fun initializeSessionAndPlayer() {    //(songs : ArrayList<AudioSongs>) {
        val sList : MutableList<MediaItem> = ArrayList()

        for ((cnt) in audioList!!.withIndex())
        {
            val fs = audioList?.get(cnt)
            val fsUri = Uri.parse(fs?.data)

            sList.add(MediaItem.fromUri(fsUri))
        }


        player = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus */ true)
            .build()
            .also { exoPlayer ->
//                viewBinding.videoView.player = exoPlayer
//                //val mediaItem = MediaItem.fromUri(fsUri)
//
//                //val mediaItem = MediaItem.fromUri(getString(R.string.media_url_mp4))
//                //val secondMediaItem = MediaItem.fromUri(getString(R.string.media_url_mp3))
//                //exoPlayer.setMediaItem(mediaItem)
//
                exoPlayer.setMediaItems(sList, mediaItemIndex, playbackPosition)
//                //exoPlayer.setMediaItems()
//
//                exoPlayer.playWhenReady = playWhenReady
//                exoPlayer.addListener(playbackStateListener)
//                exoPlayer.prepare()
            }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun getSingleTopActivity(): PendingIntent {
        return getActivity(
            this,
            0,
            Intent(this, PlayerActivity::class.java),
            immutableFlag or FLAG_UPDATE_CURRENT
        )
    }

//    private fun getBackStackedActivity(): PendingIntent {
//        return TaskStackBuilder.create(this).run {
//            addNextIntent(Intent(this@PlaybackService, MainActivity::class.java))
//            addNextIntent(Intent(this@PlaybackService, PlayerActivity::class.java))
//            getPendingIntent(0, immutableFlag or FLAG_UPDATE_CURRENT)
//        }
//    }

    private fun getShuffleCommandButton(sessionCommand: SessionCommand): CommandButton {
        val isOn = sessionCommand.customAction == CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON
        return CommandButton.Builder()
            .setDisplayName(
                getString(
                    if (isOn) R.string.exo_controls_shuffle_on_description
                    else R.string.exo_controls_shuffle_off_description
                )
            )
            .setSessionCommand(sessionCommand)
            //.setIconResId(if (isOn) R.drawable.exo_icon_shuffle_off else R.drawable.exo_icon_shuffle_on)
            .build()
    }

    inner class LocalBinder : Binder() {
        val service: PlaybackService
            get() = this@PlaybackService
    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {

        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        Log.i(TAG, "Media Service has been Destroyed - OnDestroy()")
        super.onDestroy()
    }
}