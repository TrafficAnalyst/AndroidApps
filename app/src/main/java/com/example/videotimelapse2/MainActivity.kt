package com.example.videotimelapse2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_VIDEO_CAPTURE = 1
        const val SERVER_URL = "http://192.168.0.105:5000/upload_video"
    }

    private lateinit var videoView: VideoView
    private lateinit var uploadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val uploadButton = findViewById<Button>(R.id.uploadButton)

        uploadButton.setOnClickListener {
            try {
                dispatchTakeVideoIntent()
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Aplikasi perekaman video tidak ditemukan", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun dispatchTakeVideoIntent() {
        val takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (takeVideoIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == Activity.RESULT_OK) {
            val videoUri: Uri? = data?.data

            val contentURI: Uri? = data?.data
            contentURI?.let {
                val realPath = getRealPathFromURI(it)
                // Lakukan sesuatu dengan realPath, pastikan menangani nilai null jika diperlukan
                val videoUri: Uri = Uri.parse(realPath)
                // Upload video ke server Python

                videoUri?.let { uploadVideoToServer(videoUri) }
            }

        }
    }

    private fun getRealPathFromURI(contentURI: Uri): String? {
        val result: String?
        val cursor = contentResolver.query(contentURI, null, null, null, null)
        if (cursor == null) {
            result = contentURI.path
        } else {
            cursor.moveToFirst()
            val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    }
    private fun uploadVideoToServer(videoUri: Uri) {
        val file = File(videoUri.path)
        val requestFile: RequestBody = file.asRequestBody("video/*".toMediaTypeOrNull())

        val requestBody: RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("video", file.name, requestFile)
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Log.e("UploadVideo", "Gagal mengunggah video: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("UploadVideo", "Respons dari server: $response")
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.d("UploadVideo", "Respons dari server: $responseData")
                    // ...
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Gagal mengunggah video 2: ${response.code}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    Log.e("UploadVideo", "Gagal mengunggah video 3. Kode respons: ${response.code}")
                }
            }
        })
    }
}