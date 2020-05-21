package com.example.bluswapmyface

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.divyanshu.draw.widget.DrawView
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceContour
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private var isLandScape: Boolean = false
    private var imageUri: Uri? = null
    private var drawView: DrawView? = null
    private var drawView2: DrawView? = null
    private var buttidtakepic = 0
    lateinit var myBitmap1:Bitmap
    lateinit var myBitmap2:Bitmap
    lateinit var resizedBitmap1:Bitmap
    lateinit var resizedBitmap2:Bitmap
    private var loadcheck1 = 0
    private var loadcheck2 = 0
    private var imageblurred1 = 0
    private var imageblurred2 = 0
    private var face1left:Int = 0
    private var face1right:Int = 0
    private var face1top:Int = 0
    private var face1bottom:Int = 0
    private var face2left:Int = 0
    private var face2right:Int = 0
    private var face2top:Int = 0
    private var face2bottom:Int = 0
    private var face1detected = 0
    private var face2detected = 0

    private val myViewModel : BluSwapViewModel by lazy {
        ViewModelProviders.of(this).get(BluSwapViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //myViewModel.getData1().observe(this, Observer{})

        if (myViewModel.resizedBitmap1 != null) {
            resizedBitmap1 = myViewModel.resizedBitmap1!!
        }
        if (myViewModel.resizedBitmap2 != null) {
            resizedBitmap2 = myViewModel.resizedBitmap2!!
        }

        face1detected = myViewModel.face1detected
        face2detected = myViewModel.face2detected
        imageblurred1 = 0
        imageblurred2 = 0
        loadcheck1 = myViewModel.loadcheck1
        loadcheck2 = myViewModel.loadcheck2
        face1left = myViewModel.face1left
        face1right = myViewModel.face1right
        face1top = myViewModel.face1top
        face1bottom = myViewModel.face1bottom
        face2left = myViewModel.face2left
        face2right = myViewModel.face2right
        face2top = myViewModel.face2top
        face2bottom = myViewModel.face2bottom

        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
        isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        savedInstanceState?.let {
            imageUri = it.getParcelable(KEY_IMAGE_URI)
        }

        drawView = findViewById(R.id.draw_view)
        drawView?.setStrokeWidth(10.0f)
        drawView?.setColor(Color.WHITE)

        drawView2 = findViewById(R.id.draw_view2)
        drawView2?.setStrokeWidth(10.0f)
        drawView2?.setColor(Color.BLUE)

        if (myViewModel.resizedBitmap1 != null) {
            drawView?.background = BitmapDrawable(resources, resizedBitmap1)
        }
        if (myViewModel.resizedBitmap2 != null) {
            drawView2?.background = BitmapDrawable(resources, resizedBitmap2)
        }
        // Setup classification trigger so that it classify after every stroke drew
        drawView?.setOnTouchListener { _, event ->
            // As we have interrupted DrawView's touch event,
            // we first need to pass touch events through to the instance for the drawing to show up
            drawView?.onTouchEvent(event)
            // Then if user finished a touch event, run classification
            if (event.action == MotionEvent.ACTION_UP) {
            }
            true
        }

        drawView2?.setOnTouchListener { _, event ->
            drawView2?.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
            }
            true
        }
    }



    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }
        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
        }
    }

    fun startCameraIntentForResult(view:View) {
        // Clean up last time's image
        imageUri = null
        //drawView?.background = BitmapDrawable(resources, null)
        val butpressed:Button = view as Button
        when(butpressed.id) {
            R.id.bt_takepic1 -> buttidtakepic = 1
            R.id.bt_takepic2 -> buttidtakepic = 2
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    fun startChooseImageIntentForResult(view:View) {
        val butpressed:Button = view as Button
        when(butpressed.id) {
            R.id.bt_choosepic1 -> buttidtakepic = 1
            R.id.bt_choosepic2 -> buttidtakepic = 2
        }

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDetectInImage()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            tryReloadAndDetectInImage()
        }
    }

    private fun tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri!!)
                ImageDecoder.decodeBitmap(source)
            }

            if (buttidtakepic == 1) {
                face1detected = 0
                imageblurred1 = 0
                myBitmap1 = imageBitmap.copy(Bitmap.Config.ARGB_8888 , true)
                resizedBitmap1 = Bitmap.createScaledBitmap(myBitmap1, 360, 480, true)
                drawView?.background = BitmapDrawable(resources, resizedBitmap1)
                loadcheck1 = 1
                myViewModel.updateBitmap1(resizedBitmap1)
                myViewModel.updatechecks1(face1detected,imageblurred1,loadcheck1)
                faceDetection(resizedBitmap1)
                //drawView?.foreground = BitmapDrawable(resources, imageBitmap)
                //imageView.setImageBitmap(returnedbitmap)
            } else
            if (buttidtakepic == 2) {
                face2detected = 0
                imageblurred2 = 0
                myBitmap2 = imageBitmap.copy(Bitmap.Config.ARGB_8888 , true)
                resizedBitmap2 = Bitmap.createScaledBitmap(myBitmap2, 360, 480, true)
                drawView2?.background = BitmapDrawable(resources, resizedBitmap2)
                loadcheck2 = 1
                myViewModel.updateBitmap2(resizedBitmap2)
                myViewModel.updatechecks2(face2detected,imageblurred1,loadcheck1)
                faceDetection(resizedBitmap2)
                //drawView2?.foreground = BitmapDrawable(resources, imageBitmap)
                //val returnedbitmap = bitmapBlur(imageBitmap, 1.0, 1)
                //drawView?.background = BitmapDrawable(resources, returnedbitmap)
            }
        }
        catch (e: IOException) {
        }
    }

    fun blur(view: View) {
        val butpressed:Button = view as Button
        when(butpressed.id) {
            R.id.bt_blur1 -> buttidtakepic = 1
            R.id.bt_blur2 -> buttidtakepic = 2
        }

        var width1 = face1right - face1left
        var height1 = face1bottom - face1top
        var width2 = face2right - face2left
        var height2 = face2bottom - face2top

        if (buttidtakepic == 1 && loadcheck1 == 1 && face1detected == 1) {
            var bitpart1 = resizedBitmap1.copy(Bitmap.Config.ARGB_8888,true)
            var newBitmap1 = Bitmap.createBitmap(bitpart1, face1left, face1top, width1, height1)
            var returnedbitmap = bitmapBlur(newBitmap1, 0.5F, 20)
            var scaledBitmap1 = Bitmap.createScaledBitmap(returnedbitmap!!, width1, height1, true)
            var intArray1 = IntArray(width1 * height1)
            scaledBitmap1.getPixels(intArray1, 0, width1, 0, 0, width1, height1)
            bitpart1.setPixels(intArray1,0, width1, face1left, face1top, width1, height1)
            drawView?.background = BitmapDrawable(resources, bitpart1)
            imageblurred1 = 1
            myViewModel.updatechecks1(face1detected,imageblurred1,loadcheck1)
        } else if (buttidtakepic == 1 && loadcheck1 == 0) {
            Toast.makeText(this, "Upload a Picture", Toast.LENGTH_SHORT).show()
        } else if (buttidtakepic == 1 && face1detected == 0) {
            Toast.makeText(this, "Upload Picture with a Face", Toast.LENGTH_SHORT).show()
        }
        if (buttidtakepic == 2 && loadcheck2 == 1 && face2detected == 1) {
            var bitpart2 = resizedBitmap2.copy(Bitmap.Config.ARGB_8888,true)
            var newBitmap2 = Bitmap.createBitmap(bitpart2, face2left, face2top, width2, height2)
            var returnedbitmap = bitmapBlur(newBitmap2, 0.5F, 20)
            var scaledBitmap2 = Bitmap.createScaledBitmap(returnedbitmap!!, width2, height2, true)
            var intArray2 = IntArray(width2 * height2)
            scaledBitmap2.getPixels(intArray2, 0, width2, 0, 0, width2, height2)
            bitpart2.setPixels(intArray2,0, width2, face2left, face2top, width2, height2)
            drawView2?.background = BitmapDrawable(resources, bitpart2)
            imageblurred2 = 1
            myViewModel.updatechecks2(face2detected,imageblurred2,loadcheck2)
        } else if (buttidtakepic == 2 && loadcheck2 == 0) {
            Toast.makeText(this, "Upload a Picture", Toast.LENGTH_SHORT).show()
        } else if (buttidtakepic == 2 && face2detected == 0) {
            Toast.makeText(this, "Upload Picture with a Face", Toast.LENGTH_SHORT).show()
        }
    }

    fun reset(view: View) {
        val butpressed: Button = view as Button
        when(butpressed.id) {
        R.id.bt_reset1 -> buttidtakepic = 1
        R.id.bt_reset2 -> buttidtakepic = 2
        }
        if (buttidtakepic == 1) {
            drawView?.clearCanvas()
            if (loadcheck1 == 1) {
                drawView?.background = BitmapDrawable(resources, resizedBitmap1)
                imageblurred1 = 0
                myViewModel.updatechecks2(face2detected,imageblurred1,loadcheck2)
            }
        } else
        if (buttidtakepic == 2) {
            drawView2?.clearCanvas()
            if (loadcheck2 == 1) {
                drawView2?.background = BitmapDrawable(resources, resizedBitmap2)
                imageblurred2 = 0
                myViewModel.updatechecks2(face2detected,imageblurred2,loadcheck2)
            }
        }
    }

    private fun faceDetection(imageBitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(imageBitmap)
        val options = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions. ALL_LANDMARKS)
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            //.setMinFaceSize(0.60f)
            .build()
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)
        val result = detector.detectInImage(image)
            .addOnSuccessListener { faces ->
                processFaces(faces)
            }
            .addOnFailureListener { e -> // Task failed with an exception
                Toast.makeText(this, "ProcessFaces call failed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
    }

    private fun processFaces (faces: List<FirebaseVisionFace>) {
    // Task completed successfully
        if (faces.size == 0) {
            Toast.makeText(this, "No Face detected !! Take picture with a Face", Toast.LENGTH_SHORT).show()
            return
        }
        val face = faces[0]

        //val contour = face.getContour(FirebaseVisionFaceContour.ALL_POINTS)
        //    for (point in contour.points) {
        //        val px = point.x
        //        val py = point.y
        //    }

        if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            val smileProb = face.smilingProbability
            if (smileProb > 0.8) {
                Toast.makeText(this, "Awesome Smile :D", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Why So Serious? Smile Please :)", Toast.LENGTH_SHORT).show()
            }
        }

        if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            val rightEyeOpenProb = face.rightEyeOpenProbability
            if (rightEyeOpenProb < 0.8) {
                Toast.makeText(this, "Open Your Right Eye", Toast.LENGTH_SHORT).show()
            }
        }
        if (face.leftEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
            val leftEyeOpenProb = face.leftEyeOpenProbability
            if (leftEyeOpenProb < 0.8) {
                Toast.makeText(this, "Open Your Left Eye", Toast.LENGTH_SHORT).show()
            }
        }

        if (buttidtakepic == 1 && loadcheck1 == 1) {
            face1left = face.boundingBox.left
            face1right = face.boundingBox.right
            face1top = face.boundingBox.top
            face1bottom = face.boundingBox.bottom
            myViewModel.updateface1paras(face1left, face1right, face1top, face1bottom)
            //Toast.makeText(this, "$face1left", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face1right", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face1top", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face1bottom", Toast.LENGTH_SHORT).show()
            face1detected = 1
        } else
        if (buttidtakepic == 2 && loadcheck2 == 1) {
            face2left = face.boundingBox.left
            face2right = face.boundingBox.right
            face2top = face.boundingBox.top
            face2bottom = face.boundingBox.bottom
            myViewModel.updateface2paras(face2left, face2right, face2top, face2bottom)
            //Toast.makeText(this, "$face2left", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face2right", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face2top", Toast.LENGTH_SHORT).show()
            //Toast.makeText(this, "$face2bottom", Toast.LENGTH_SHORT).show()
            face2detected = 1
        }
    }

    fun swapFaces (view: View) {
        if (loadcheck1 == 1 && loadcheck2 == 1 && face1detected == 1 && face2detected == 1 && imageblurred1 == 0 && imageblurred2 == 0) {
            var bitpart1 = resizedBitmap1.copy(Bitmap.Config.ARGB_8888,true)
            var bitpart2 = resizedBitmap2.copy(Bitmap.Config.ARGB_8888,true)

            var width1 = face1right - face1left
            var height1 = face1bottom - face1top
            var width2 = face2right - face2left
            var height2 = face2bottom - face2top

            var newBitmap1 = Bitmap.createBitmap(bitpart1, face1left, face1top, width1, height1)
            var newBitmap2 = Bitmap.createBitmap(bitpart2, face2left, face2top, width2, height2)

            var scaledBitmap1 = Bitmap.createScaledBitmap(newBitmap1, width2, height2, true)
            var scaledBitmap2 = Bitmap.createScaledBitmap(newBitmap2, width1, height1, true)

            var intArray1 = IntArray(width2 * height2)
            var intArray2 = IntArray(width1 * height1)

            scaledBitmap1.getPixels(intArray1, 0, width2, 0, 0, width2, height2)
            scaledBitmap2.getPixels(intArray2, 0, width1, 0, 0, width1, height1)

            bitpart1.setPixels(intArray2,0, width1, face1left, face1top, width1, height1)
            bitpart2.setPixels(intArray1,0, width2, face2left, face2top, width2, height2)

            draw_view?.background = BitmapDrawable(resources, bitpart1)
            draw_view2?.background = BitmapDrawable(resources, bitpart2)
        }
        else if (loadcheck1 == 0 || loadcheck2 == 0) {
            Toast.makeText(this, "Upload Both Pictures", Toast.LENGTH_SHORT).show()
            return
        }
        else if (face1detected == 0 || face2detected == 0) {
            Toast.makeText(this, "Upload Both Pictures with a Face", Toast.LENGTH_SHORT).show()
            return
        }
        else if (imageblurred1 == 1 || imageblurred2 == 1) {
            Toast.makeText(this, "Image Blurred. Press Reset and try again", Toast.LENGTH_SHORT).show()
            return
        }
    }

    private fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)
        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = yi
        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int
        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - Math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }


    companion object {
        private const val KEY_IMAGE_URI = "com.example.bluswapmyface.KEY_IMAGE_URI"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val PERMISSION_REQUESTS = 1
    }
}
