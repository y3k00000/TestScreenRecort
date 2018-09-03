package y3k.testscreenrecort

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.widget.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager
    private lateinit var projection: MediaProjection
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var mediaRecorder: MediaRecorder

    private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener {
            projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(projectionManager.createScreenCaptureIntent(), 1234)
        }
        if (!requiredPermissions.all { ActivityCompat.checkSelfPermission(this@MainActivity, it) == PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this@MainActivity, requiredPermissions, 5678)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            1234 -> {
                startProjectionRecord(projection = projectionManager.getMediaProjection(resultCode, data))
                findViewById<Button>(R.id.button).apply {
                    text = this@MainActivity.getString(R.string.btn_stop)
                    setOnClickListener {
                        this@MainActivity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(
                                Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)),
                                "resource/folder"))
                        this@MainActivity.finish()
                    }
                }
            }
            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            5678 -> {
                if (!grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Toast.makeText(this@MainActivity, "Require Permissions to continue", Toast.LENGTH_LONG).show()
                    this@MainActivity.finish()
                }
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    private fun startProjectionRecord(projection: MediaProjection) {
        this.projection = projection

        val outMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(outMetrics)

        val bitrate = findViewById<Spinner>(R.id.spinner_bitrate).selectedItem as String

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.DEFAULT)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncodingBitRate(bitrate.toInt() * 1024 * 1024)
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
        }

        mediaRecorder.start()

        findViewById<TextView>(R.id.text_view_bitrate).text = resources.getString(R.string.bitrate_text,bitrate)
        findViewById<Spinner>(R.id.spinner_bitrate).apply {
            (parent as LinearLayout).removeView(this)
        }

        this@MainActivity.moveTaskToBack(true)
    }

    override fun onDestroy() {
        if (this@MainActivity::virtualDisplay.isInitialized) {
            mediaRecorder.apply {
                try {
                    stop()
                    release()
                } catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
        if (this@MainActivity::virtualDisplay.isInitialized) {
            try {
                virtualDisplay.release()
            } catch (e:Exception){
                e.printStackTrace()
            }
        }
        sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES))))
        super.onDestroy()
    }
}
