package com.ruthwikkk.clearkeydrmsample

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.drm.*
import com.google.android.exoplayer2.source.dash.DashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), Player.EventListener {

    private var exoPlayer: SimpleExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    var drmSessionManager: DefaultDrmSessionManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        trackSelector = DefaultTrackSelector(this)
        exoPlayer = SimpleExoPlayer.Builder(this)
                .setTrackSelector(trackSelector!!)
                .build()

        exoPlayer?.addListener(this)

        player_view.player = exoPlayer

        edt_manifest_url.setText(Constants.SAMPLE_MPD_URL_CLEAR_KEY)
        edt_key_id.setText(Constants.SAMPLE_KEY_ID_CLEAR_KEY)
        edt_key_value.setText(Constants.SAMPLE_KEY_VALUE_CLEAR_KEY)

        playVideo(Constants.SAMPLE_MPD_URL_CLEAR_KEY, Constants.SAMPLE_KEY_ID_CLEAR_KEY,Constants.SAMPLE_KEY_VALUE_CLEAR_KEY)

        btn_play.setOnClickListener {
            tv_message.text = ""
            if(verifyInput()){
                playVideo(
                        edt_manifest_url.text.toString(),
                        edt_key_id.text.toString(),
                        edt_key_value.text.toString()
                )
            }
        }
    }

    private fun verifyInput(): Boolean{
        if(edt_manifest_url.text.toString().isEmpty()){
            showLongToast("URL Empty")
            return false
        }
        if(edt_key_id.text.toString().isEmpty()){
            showLongToast("Key ID Empty")
            return false
        }
        if(edt_key_value.text.toString().isEmpty()){
            showLongToast("Key Empty")
            return false
        }
        return true
    }

    fun playVideo(url: String, id: String, value: String){
        try {
            drmSessionManager =
                    Util.getDrmUuid(C.CLEARKEY_UUID.toString())?.let { buildDrmSessionManager(
                            it,
                            true,
                            id,
                            value
                    ) }
        } catch (e: UnsupportedDrmException) {
            e.printStackTrace()
        }
        exoPlayer?.prepare(buildDashMediaSource(Uri.parse(url)))

        exoPlayer?.playWhenReady = true

        val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build()
        exoPlayer?.setAudioAttributes(audioAttributes, true)

        // tv_message.text = Util.MODEL + "\n" + Util.DEVICE_DEBUG_INFO
    }

    @Throws(UnsupportedDrmException::class)
    private fun buildDrmSessionManager(uuid: UUID, multiSession: Boolean, id: String, value: String): DefaultDrmSessionManager {
        val drmCallback = LocalMediaDrmCallback("{\"keys\":[{\"kty\":\"oct\",\"k\":\"${value}\",\"kid\":\"${id}\"}],\"type\":\"temporary\"}".toByteArray())
        val mediaDrm = FrameworkMediaDrm.newInstance(uuid)
        return DefaultDrmSessionManager(uuid, mediaDrm, drmCallback, null, multiSession)
    }

    private fun buildDashMediaSource(uri: Uri): DashMediaSource {
        val userAgent = "App-Drm"

        val dashChunkSourceFactory: DashChunkSource.Factory = DefaultDashChunkSource.Factory(
            DefaultHttpDataSourceFactory(userAgent, DefaultBandwidthMeter())
        )

        val manifestDataSourceFactory = DefaultHttpDataSourceFactory(userAgent)

        val mediaItem = MediaItem.Builder().setUri(uri).build()

        return DashMediaSource.Factory(dashChunkSourceFactory, manifestDataSourceFactory)
                .setDrmSessionManager(drmSessionManager ?: DrmSessionManager.DUMMY)
                .createMediaSource(mediaItem)
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {

                if (playWhenReady) {
                    player_view.visibility = View.VISIBLE
                    ll_loading.visibility = View.GONE
                }
            }
            Player.STATE_BUFFERING -> {
                player_view.visibility = View.GONE
                ll_loading.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        exoPlayer?.stop()
        exoPlayer?.release()
        super.onDestroy()
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        tv_message.text = error.message
    }

    fun Context.showLongToast(message: String) =
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}