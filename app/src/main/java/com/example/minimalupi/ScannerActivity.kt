package com.example.minimalupi

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.minimalupi.databinding.ActivityScannerBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScannerBinding

    private val scanned = AtomicBoolean(false)
    private val decodeExecutor = Executors.newSingleThreadExecutor()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSize: Size = Size(1280, 720)
    private var cameraId: String? = null

    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    private var frameSkipCounter = 0

    private val qrReader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            maybeStartCamera()
        } else {
            Toast.makeText(this, "Camera needed for QR scan", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.scannerTitle.alpha = 0f
        binding.scannerTitle.animate().alpha(1f).setDuration(280L).start()
        binding.paymentPreviewCard.alpha = 0f
        binding.paymentPreviewCard.translationY = 24f
        binding.paymentPreviewCard.animate().alpha(1f).translationY(0f).setDuration(320L).start()
    }

    override fun onResume() {
        super.onResume()
        startCameraThread()
        ensureCameraPermission()
    }

    override fun onPause() {
        closeCamera()
        stopCameraThread()
        super.onPause()
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            maybeStartCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun maybeStartCamera() {
        if (binding.cameraTexture.isAvailable) {
            openCamera()
        } else {
            binding.cameraTexture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) = Unit
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
            }
        }
    }

    private fun startCameraThread() {
        if (cameraThread != null) return
        cameraThread = HandlerThread("scanner-camera").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    private fun stopCameraThread() {
        cameraThread?.quitSafely()
        cameraThread?.join()
        cameraThread = null
        cameraHandler = null
    }

    private fun openCamera() {
        if (cameraDevice != null) return
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val selectedId = chooseBackCamera(manager) ?: return
        cameraId = selectedId

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        manager.openCamera(selectedId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, cameraHandler)
    }

    private fun chooseBackCamera(manager: CameraManager): String? {
        for (id in manager.cameraIdList) {
            val chars = manager.getCameraCharacteristics(id)
            val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: continue
                val sizes = map.getOutputSizes(SurfaceTexture::class.java)
                previewSize = sizes.firstOrNull { it.width <= 1280 && it.height <= 720 }
                    ?: sizes.firstOrNull()
                    ?: Size(1280, 720)
                return id
            }
        }
        return null
    }

    private fun createSession() {
        val camera = cameraDevice ?: return
        val texture = binding.cameraTexture.surfaceTexture ?: return
        texture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(texture)

        imageReader?.close()
        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.YUV_420_888, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (scanned.get()) {
                image.close()
                return@setOnImageAvailableListener
            }
            frameSkipCounter = (frameSkipCounter + 1) % 2
            if (frameSkipCounter != 0) {
                image.close()
                return@setOnImageAvailableListener
            }
            decodeExecutor.execute {
                decodeImage(image)
            }
        }, cameraHandler)

        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface)
            addTarget(imageReader!!.surface)
            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        }

        camera.createCaptureSession(
            listOf(previewSurface, imageReader!!.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    session.setRepeatingRequest(request.build(), null, cameraHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) = Unit
            },
            cameraHandler
        )
    }

    private fun decodeImage(image: Image) {
        try {
            val nv21 = yuv420ToNv21(image)
            val source = PlanarYUVLuminanceSource(
                nv21,
                image.width,
                image.height,
                0,
                0,
                image.width,
                image.height,
                false
            )
            val bitmap = BinaryBitmap(HybridBinarizer(source))
            val result = runCatching { qrReader.decodeWithState(bitmap) }.getOrNull()
            qrReader.reset()

            val raw = result?.text
            val parsed = if (raw != null) UpiParser.parse(raw) else null
            if (parsed != null && scanned.compareAndSet(false, true)) {
                runOnUiThread {
                    vibrateOnce()
                    binding.paymentPreviewCard.animate().alpha(0.92f).setDuration(120L).withEndAction {
                        val intent = Intent(this, PaymentActivity::class.java).apply {
                            putExtra(PaymentActivity.EXTRA_UPI_ID, parsed.upiId)
                            putExtra(PaymentActivity.EXTRA_NAME, parsed.payeeName.orEmpty())
                            putExtra(PaymentActivity.EXTRA_AMOUNT, parsed.amount.orEmpty())
                        }
                        startActivity(intent)
                        finish()
                    }.start()
                }
            }
        } catch (_: Throwable) {
            // Keep scanning on decode failure.
        } finally {
            image.close()
        }
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 4
        val nv21 = ByteArray(ySize + uvSize * 2)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        copyPlane(yPlane, width, height, nv21, 0, 1)
        copyPlane(vPlane, width / 2, height / 2, nv21, ySize, 2)
        copyPlane(uPlane, width / 2, height / 2, nv21, ySize + 1, 2)
        return nv21
    }

    private fun copyPlane(
        plane: Image.Plane,
        width: Int,
        height: Int,
        out: ByteArray,
        offset: Int,
        pixelStrideOut: Int
    ) {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var outputOffset = offset
        val rowData = ByteArray(rowStride)

        for (row in 0 until height) {
            val length = if (pixelStride == 1 && pixelStrideOut == 1) width else (width - 1) * pixelStride + 1
            if (row == height - 1) {
                buffer.get(rowData, 0, minOf(length, buffer.remaining()))
            } else {
                buffer.get(rowData, 0, length)
                if (rowStride > length) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }

            var col = 0
            while (col < width) {
                out[outputOffset] = rowData[col * pixelStride]
                outputOffset += pixelStrideOut
                col++
            }
        }
    }

    private fun closeCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
        imageReader?.close()
        imageReader = null
        binding.cameraTexture.surfaceTextureListener = null
    }

    private fun vibrateOnce() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        decodeExecutor.shutdown()
    }
}
