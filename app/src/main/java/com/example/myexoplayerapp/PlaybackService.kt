package com.example.myexoplayerapp

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import kotlin.system.exitProcess

private const val TAG = "myInfo"
@androidx.media3.common.util.UnstableApi
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var customCommands: List<CommandButton>
    private var mediaItemIndex = 0
    private var playbackPosition = 0L

    // Create
    // Binder given to clients
    //private val iBinder: IBinder = LocalBinder()

    companion object {
//        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON =
//            "android.media3.session.demo.SHUFFLE_ON"
//        private const val CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF =
//            "android.media3.session.demo.SHUFFLE_OFF"
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "demo_session_notification_channel_id"
        private const val IMMUTABLE_FLAG = FLAG_IMMUTABLE

        private var running = false

//        @JvmStatic
//        fun start(context: Context) {
//            Log.i(TAG, "PlaybackService companion object start()" )
//            context.startService(Intent(context, PlaybackService::class.java))
//        }

        @JvmStatic
        fun stop(context: Context) {
            Log.i(TAG, "PlaybackService companion object stop()" )
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }

    private var mediaSession: MediaSession? = null

    // If desired, validate the controller before returning the media session
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()
        //start(this)
        running = true

//        customCommands =
//            listOf(
//                getShuffleCommandButton(
//                    SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON, Bundle.EMPTY)
//                ),
//                getShuffleCommandButton(
//                    SessionCommand(CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_OFF, Bundle.EMPTY)
//                )
//            )
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
                exoPlayer.setMediaItems(sList, mediaItemIndex, playbackPosition)
            }
        mediaSession = MediaSession.Builder(this, player).build()
    }

//    private fun getShuffleCommandButton(sessionCommand: SessionCommand): CommandButton {
//        val isOn = sessionCommand.customAction == CUSTOM_COMMAND_TOGGLE_SHUFFLE_MODE_ON
//        return CommandButton.Builder()
//            .setDisplayName(
//                getString(
//                    if (isOn) R.string.exo_controls_shuffle_on_description
//                    else R.string.exo_controls_shuffle_off_description
//                )
//            )
//            .setSessionCommand(sessionCommand)
//            .build()
//    }

//    inner class LocalBinder : Binder() {
//        //val service: PlaybackService
//        //    get() = this@PlaybackService
//        fun getService(): PlaybackService = this@PlaybackService
//    }

//    override fun onBind(intent: Intent?): IBinder {
//        super.onBind(intent)
//        return iBinder
//    }

    // This seems to allow Service OnDestroy() only if onTaskedRemoved() is called - 3-29-2024
    // which is what I want
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        //Service.startForeground(0, notification, FOREGROUND_SERVICE_TYPE)
        Log.i(TAG, "onStartCommand() - return START_STICKY")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {

        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
            Log.i(TAG, "onTaskRemoved() Media Service has been released.")
        } finally {
            stopSelf()
            Log.i(TAG, "onTaskRemoved() call stopSelf()")

            super.onTaskRemoved(rootIntent)

            // try exit in onDestroy() - might not even need it now with START_STICK
//            Log.i(TAG, "onTaskRemoved() call to exitProcess(-1) will be made")
//            exitProcess(-1)

        }
    }

    // Remember to release the player and media session in onDestroy - does no get called
    override fun onDestroy() {
        super.onDestroy()
        running = false
        stop(this)

        try {
            Log.i(TAG, "OnDestroy() - running = false")
        } finally {
            super.onDestroy()
            Log.i(TAG, "OnDestroy() call to exitProcess(-1) will be made")
            exitProcess(-1)
        }
    }
}