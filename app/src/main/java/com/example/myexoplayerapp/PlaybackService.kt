package com.example.myexoplayerapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
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

private const val TAG = "myInfo"
@androidx.media3.common.util.UnstableApi
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var customCommands: List<CommandButton>
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "demo_session_notification_channel_id"
        private const val IMMUTABLE_FLAG = FLAG_IMMUTABLE

        private var running = false

        @JvmStatic
        fun stop(context: Context) {
            Log.i(TAG, "SERVICE - PlaybackService companion Call to stop()" )
            running = false
            context.stopService(Intent(context, PlaybackService::class.java))
        }
        @JvmStatic
        fun serviceIsRunning() : Boolean {
            Log.i(TAG, "PlaybackService companion object SERVICE RUNNING = $running")
            return running
        }
    }

    private var mediaSession: MediaSession? = null

    // If desired, validate the controller before returning the media session
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

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

    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        //start(this)
        running = true
        Log.i(TAG,"SERVICE - onCreate()")

        initializeSessionAndPlayer()    // assign player = ExoPlayer... & mediaSession
        setListener(serviceListener())

        Log.i(TAG,"SERVICE onCreate() - PlaybackService running = $running ")
    }

    private fun serviceListener() = object : Listener {
        @SuppressLint("MissingPermission")
        override fun onForegroundServiceStartNotAllowedException() {
            Log.i(TAG,"In onForegroundServiceStartNotAllowedException() which calls TaskBuilder...")
            val notificationManagerCompat = NotificationManagerCompat.from(this@PlaybackService)
            ensureNotificationChannel(notificationManagerCompat)

            //TODO - this looks like were some of my errors/crashes are occurring
            val pendingIntent =
                TaskStackBuilder.create(this@PlaybackService).run {
                    addNextIntent(Intent(this@PlaybackService, PlayerActivity::class.java))
                    getPendingIntent(0, IMMUTABLE_FLAG or FLAG_UPDATE_CURRENT)
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
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    // assign player = ExoPlayer... & mediaSession
    // populate MediaItems
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
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { exoPlayer ->
                exoPlayer.setMediaItems(sList, mediaItemIndex, playbackPosition)
            }
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player!!
        if (player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED)
        {
            stopSelf()
            Log.i(TAG, "onTaskRemoved() test player state satisfied - call stopSelf()")
        }
        else
            Log.i(TAG, "onTaskRemoved() test player state NOT SATISFIED - DO NOT call stopSelf()")
    }

    // Remember to release the player and media session in onDestroy - does no get called
    override fun onDestroy() {
        super.onDestroy()
        running = false

        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            Log.i(TAG, "SERVICE - onDestroy() Media Service has been released.")
        } finally {
            super.onDestroy()
            Log.i(TAG, "SERVICE - OnDestroy() call super.onDestroy()")
        }
    }
}