package com.reactnativeimageselector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.core.content.FileProvider
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.Exception


class ImageSelectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), PermissionListener {

  companion object {
    val LIBRARY_PERMISSION_REQUEST_CODE: Int = 7878
    val CAMERA_PERMISSION_REQUEST_CODE: Int = 8787
    val IMAGE_CAPTURE_REQUEST_CODE: Int = 8989
    val PICK_IMAGE_REQUEST_CODE: Int = 9898
    var cameraCaptureURI: Uri? = null
    var cameraCaptureFile: File? = null

    var globalOptions: ReadableMap? = null

    object ErrorCode {
      val cameraPermissionDenied: Int = 100
      val libraryPermissionDenied: Int = 101
      val notValidPath: Int = 105
      val failPickImage: Int = 106
    }

    object ErrorMessage {
      val cameraPermissionDenied: String = "CAMERA_PERMISSION_DENIED"
      val libraryPermissionDenied: String = "LIBRARY_PERMISSION_DENIED"
      val notValidPath: String = "NOT_VALID_PATH"
      val failPickImage: String = "FAIL_TO_PICK_IMAGE"
    }
  }

  private var globalCallback: Callback? = null

  override fun getName(): String {
    return "ImageSelector"
  }

  private fun launchCamera() {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
      currentActivity.let { activity ->
        if (activity != null) {
          takePictureIntent.resolveActivity(activity.packageManager).also {
            cameraCaptureFile = File(reactApplicationContext.getExternalFilesDir(null)?.absolutePath, "react-native-image-selector_" + UUID.randomUUID().toString() + ".jpg")
            cameraCaptureFile.let { file ->
              if (file != null) {
                cameraCaptureURI = FileProvider.getUriForFile(reactApplicationContext, StringBuilder(reactApplicationContext.packageName).append(".fileprovider").toString(), file)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraCaptureURI)
                this.globalCallback.let { callback ->
                  if (callback != null) {
                    reactApplicationContext.addActivityEventListener(ImageSelectorActivityListener(callback, reactApplicationContext))
                    activity.startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST_CODE)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  private fun requestCameraPermission() {
    val activity: PermissionAwareActivity = currentActivity as PermissionAwareActivity
    activity.requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), CAMERA_PERMISSION_REQUEST_CODE, this)
  }

  private fun checkCameraPermission() {
    currentActivity.let { it
      if (it != null) {
        val cameraResult = it.checkCallingOrSelfPermission(Manifest.permission.CAMERA)
        val writeExternalStorageResult = it.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (cameraResult == PackageManager.PERMISSION_GRANTED && writeExternalStorageResult == PackageManager.PERMISSION_GRANTED) {
          this.launchCamera()
        } else {
          this.requestCameraPermission()
        }
      }
    }
  }

  private fun launchLibrary() {
    Intent(Intent.ACTION_PICK).also { galleryIntent ->
      galleryIntent.type = "image/*"
      currentActivity.let { currentActivity ->
        this.globalCallback.let { callback ->
          if (callback != null) {
            reactApplicationContext.addActivityEventListener(ImageSelectorActivityListener(callback, reactApplicationContext))
            currentActivity?.startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
          }
        }
      }
    }
  }

  private fun requestLibraryPermission() {
    var activity: PermissionAwareActivity = currentActivity as PermissionAwareActivity
    activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), LIBRARY_PERMISSION_REQUEST_CODE, this)
  }

  private fun checkLibraryPermission() {
    currentActivity.let { activity ->
      if (activity != null) {
        val libraryResult = activity.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (libraryResult == PackageManager.PERMISSION_GRANTED) {
          this.launchLibrary()
        } else {
          this.requestLibraryPermission()
        }
      }
    }
  }

  @ReactMethod
  fun launchPicker(options: ReadableMap?, callback: Callback) {
    globalOptions = options
    this.globalCallback = callback

    var title: String? = "Pick Photos"
    var takePhotoButtonTitle: String? = "Take Photos"
    var chooseFromLibraryButtonTitle: String? = "Open Photo Gallery"
    var cancelButtonTitle: String? = "Cancel"

    globalOptions.let { option ->
      if (option != null) {
        if (option.hasKey("title")) {
          if (option.getString("title") != null) {
            title = option.getString("title")
          }
        }
        if (option.hasKey("takePhotoButtonTitle")) {
          if (option.getString("takePhotoButtonTitle") != null) {
            takePhotoButtonTitle = option.getString("takePhotoButtonTitle")
          }
        }

        if (option.hasKey("chooseFromLibraryButtonTitle")) {
          if (option.getString("chooseFromLibraryButtonTitle") != null) {
            chooseFromLibraryButtonTitle = option.getString("chooseFromLibraryButtonTitle")
          }
        }
        if (option.hasKey("cancelButtonTitle")) {
          if (option.getString("cancelButtonTitle") != null) {
            cancelButtonTitle = option.getString("cancelButtonTitle")
          }
        }
      }
    }

    currentActivity.let {
      it
      if (it != null) {
        if (!it.isFinishing) {
          val dialogBuilder = AlertDialog.Builder(it)
            .setTitle(title)
            .setItems(arrayOf(takePhotoButtonTitle, chooseFromLibraryButtonTitle)) { _, which ->
              if (which == 0) {
                this.checkCameraPermission()
              }
              if (which == 1) {
                this.checkLibraryPermission()
              }
            }
            .setNeutralButton(cancelButtonTitle) { _, _ ->
              val response = Arguments.createMap()
              response.putBoolean("didCancel", true)
              callback.invoke(null, response)
              this.globalCallback = null
            }
          dialogBuilder.show()
        }
      }
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
    if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults != null) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        this.launchCamera()
      } else {
        this.globalCallback.let { callback ->
          if (callback != null) {
            val error = Arguments.createMap()
            error.putInt("code", ErrorCode.cameraPermissionDenied)
            error.putString("message", ErrorMessage.cameraPermissionDenied)
            callback.invoke(error)
            this.globalCallback = null
          }
        }
      }
    }

    if (requestCode == LIBRARY_PERMISSION_REQUEST_CODE && grantResults != null) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        this.launchLibrary()
      } else {
        this.globalCallback.let { callback ->
          if (callback != null) {
            val error = Arguments.createMap()
            error.putInt("code", ErrorCode.libraryPermissionDenied)
            error.putString("message", ErrorMessage.libraryPermissionDenied)
            callback.invoke(error)
            this.globalCallback = null
          }
        }
      }
    }
    return true
  }

  private class ImageSelectorActivityListener(callback: Callback, context: ReactApplicationContext): BaseActivityEventListener() {
    private var callbackInvoker: Callback? = callback
    private var context: ReactApplicationContext? = context

    private fun encodeImage(path: String): String? {
      val imagefile = File(path)
      var fis: FileInputStream? = null
      try {
        fis = FileInputStream(imagefile)
      } catch (e: FileNotFoundException) {
        e.printStackTrace()
      }
      val bm = BitmapFactory.decodeStream(fis)
      return if (bm == null) {
//        file size == 0byte
        ""
      } else {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b: ByteArray = baos.toByteArray()
        Base64.encodeToString(b, Base64.NO_WRAP)
      }
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(activity, requestCode, resultCode, data)
      if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK) {
          cameraCaptureFile.let { cameraCaptureFile ->
            if (cameraCaptureFile != null) {
              if (activity != null) {
                val response = FileManager.createCacheFile(activity.applicationContext, Uri.fromFile(cameraCaptureFile), globalOptions)
                this.callbackInvoker.let { callback ->
                  if (callback != null) {
                    callback.invoke(null, response)
                    this.callbackInvoker = null
                  }
                }
              };
            }
          }
        }
        if (resultCode == Activity.RESULT_CANCELED) {
          this.callbackInvoker.let { callback ->
            if (callback != null) {
              val response = Arguments.createMap()
              response.putBoolean("didCancel", true)
              callback.invoke(null, response)
              this.callbackInvoker = null
            }
          }
        }
      }

      if (requestCode == PICK_IMAGE_REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK) {
          data.let { parsedData ->
            if (parsedData != null) {
              val uri = parsedData.data
              uri?.let { parsedUri ->
                activity?.let { availableActivity ->
                  try {
                    var realPathFromUri: String? = PathManager.getPathFromURI(context, parsedUri, globalOptions)
                    realPathFromUri.let { realPath ->
                      if (realPath == null) {
                        throw NullPointerException("realPathFromUri is Null")
                      }
                      if (!realPath.startsWith("file://")) {
                        realPathFromUri = "file://${realPathFromUri}"
                      }
                      val response = FileManager.createCacheFile(availableActivity.applicationContext, Uri.parse(realPathFromUri), globalOptions)
                      this.callbackInvoker?.let { invoker ->
                        invoker.invoke(null, response)
                        this.callbackInvoker = null
                      }
                    }
                  } catch (e: NullPointerException) {
                    this.callbackInvoker?.let {
                      val error = Arguments.createMap()
                      error.putInt("code", ErrorCode.notValidPath)
                      error.putString("message", "${ErrorMessage.notValidPath}, ${e.message}")
                      it.invoke(error)
                      this.callbackInvoker = null
                    }
                  } catch (e: Exception) {
                    this.callbackInvoker?.let {
                      val error = Arguments.createMap()
                      error.putInt("code", ErrorCode.failPickImage)
                      error.putString("message", ErrorMessage.failPickImage)
                      it.invoke(error)
                      this.callbackInvoker = null
                    }
                  }
                }
              }
            }
          }
        }

        if (resultCode == Activity.RESULT_CANCELED) {
          this.callbackInvoker.let { callback ->
            if (callback != null) {
              val response = Arguments.createMap()
              response.putBoolean("didCancel", true)
              callback.invoke(null, response)
              this.callbackInvoker = null
            }
          }
        }
      }
    }
  }
}
