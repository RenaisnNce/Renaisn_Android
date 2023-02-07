package com.renaisn.reader.help

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import com.renaisn.reader.R
import splitties.systemservices.audioManager

object MediaHelp {

    const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            or PlaybackStateCompat.ACTION_REWIND
            or PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_FAST_FORWARD
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SEEK_TO
            or PlaybackStateCompat.ACTION_SET_RATING
            or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
            or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
            or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
            or PlaybackStateCompat.ACTION_PLAY_FROM_URI
            or PlaybackStateCompat.ACTION_PREPARE
            or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
            or PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
            or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
            or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
            or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
            or PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED)

    fun buildAudioFocusRequestCompat(
        audioFocusChangeListener: AudioManager.OnAudioFocusChangeListener
    ): AudioFocusRequestCompat {
        val mPlaybackAttributes = AudioAttributesCompat.Builder()
            .setUsage(AudioAttributesCompat.USAGE_MEDIA)
            .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
            .build()
        return AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setAudioAttributes(mPlaybackAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener)
            .build()
    }


    /**
     * @return 音频焦点
     */
    fun requestFocus(focusRequest: AudioFocusRequestCompat): Boolean {
        val request = AudioManagerCompat.requestAudioFocus(audioManager, focusRequest)
        return request == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 播放静音音频,用来获取音频焦点
     */
    fun playSilentSound(mContext: Context) {
        kotlin.runCatching {
            // Stupid Android 8 "Oreo" hack to make media buttons work
            val mMediaPlayer = MediaPlayer.create(mContext, R.raw.silent_sound)
            mMediaPlayer.setOnCompletionListener { mMediaPlayer.release() }
            mMediaPlayer.start()
        }
    }
}