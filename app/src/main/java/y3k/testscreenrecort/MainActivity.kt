package y3k.testscreenrecort

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Button
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var mediaRecorder: MediaRecorder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener {
            projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 1234)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1234 -> {
                startProjectionRecord(projection = projectionManager.getMediaProjection(resultCode, data))
                findViewById<Button>(R.id.button).apply {
                    text = this@MainActivity.getString(R.string.btn_stop)
                    setOnClickListener {
                        this@MainActivity.finish()
                    }
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    private fun startProjectionRecord(projection: MediaProjection) {
        Log.d("MediaProjection", "startProjectionRecord")
        this.projection = projection
        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(10 * 1024 * 1024)
            setVideoFrameRate(30)
            setVideoSize(outMetrics.widthPixels, outMetrics.heightPixels)
            val file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path + "/" + Date().time + ".mp4"
            Log.d("MediaRecorder", file)
            setOutputFile(file)
            prepare()
        }

        val callback = object : MediaProjection.Callback() {
            override fun onStop() {
                mediaRecorder.stop()
                super.onStop()
            }
        }

        projection.apply {
            Log.d("MyScreen", ".apply")
            projection.registerCallback(callback, Handler())
            virtualDisplay = createVirtualDisplay("MyScreen", outMetrics.widthPixels, outMetrics.heightPixels, outMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface, object : VirtualDisplay.Callback() {
                override fun onPaused() {
                    super.onPaused()
                    Log.d("MyScreen", "onPaused()")
                }

                override fun onResumed() {
                    super.onResumed()
                    Log.d("MyScreen", "onResumed()")
                }

                override fun onStopped() {
                    super.onStopped()
                    Log.d("MyScreen", "onStopped()")
                }
            }, Handler())
            mediaRecorder.start()
        }
    }

    override fun onDestroy() {
        mediaRecorder.apply {
            stop()
            release()
        }
        virtualDisplay.release()
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))))
        super.onDestroy()
    }
}
