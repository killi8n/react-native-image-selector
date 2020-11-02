package com.reactnativeimageselector

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.loader.content.CursorLoader
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.io.File

class ImageSelectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), PermissionListener {

    private val READ_EXTERNAL_STORAGE_REQUEST_CODE = 100
    private var openLibraryPromise: Promise? = null

    override fun getName(): String {
      return "ImageSelector"
    }

    fun requestPermission() {
      val activity: PermissionAwareActivity = currentActivity as PermissionAwareActivity
      activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_EXTERNAL_STORAGE_REQUEST_CODE, this)
    }

    fun getImages(promise: Promise) {
      val projections = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATA
      )
      val deviceImages = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
      val cursor = reactApplicationContext.contentResolver.query(
        deviceImages,
        projections,
        null,
        null,
        null
      )

      val images = WritableNativeArray()
      if (cursor.moveToFirst()) {
        do {
          val image = Arguments.createMap()

          val _ID_COLUMN_INDEX = cursor.getColumnIndex(MediaStore.Images.Media._ID)
          val BUCKET_DISPLAY_NAME_INDEX = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
          val SIZE_INDEX = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
          val DATA_INDEX = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

          val _id = cursor.getString(_ID_COLUMN_INDEX)
          val bucketDisplayName = cursor.getString(BUCKET_DISPLAY_NAME_INDEX)
          val size = cursor.getDouble(SIZE_INDEX)
          val data = cursor.getString(DATA_INDEX)

          val file = File(data)
          val fileURI = Uri.fromFile(File(data)).toString()
          var base64Encoded = Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)

          image.putString("_id", _id)
          image.putString("fileName", bucketDisplayName)
          image.putString("type", data.substring(data.lastIndexOf(".") + 1))
          image.putDouble("fileSize", size)
          image.putString("uri", fileURI)
          image.putString("data", base64Encoded)

          images.pushMap(image)
        } while (cursor.moveToNext())
      }

      promise.resolve(images)
    }

    @ReactMethod
    fun openLibrary(promise: Promise) {
      this.openLibraryPromise = promise
      val res = reactApplicationContext.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
      if (res == PackageManager.PERMISSION_GRANTED) {
        this.getImages(promise)
      } else {
        this.requestPermission()
      }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
      if (requestCode == READ_EXTERNAL_STORAGE_REQUEST_CODE && grantResults != null) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          this.openLibraryPromise.let { it
            if (it != null) {
              this.getImages(it)
            }
          }
        }
      }
      return true
    }
}
