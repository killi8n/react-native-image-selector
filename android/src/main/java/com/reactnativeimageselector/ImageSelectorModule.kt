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
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*


class ImageSelectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), PermissionListener {

  companion object {
    val LIBRARY_PERMISSION_REQUEST_CODE: Int = 7878
    val CAMERA_PERMISSION_REQUEST_CODE: Int = 8787
    val IMAGE_CAPTURE_REQUEST_CODE: Int = 8989
    val PICK_IMAGE_REQUEST_CODE: Int = 9898
    var cameraCaptureURI: Uri? = null
    var cameraCaptureFile: File? = null

    object ErrorCode {
      val cameraPermissionDenied: Int = 100
      val libraryPermissionDenied: Int = 101
    }

    object ErrorMessage {
      val cameraPermissionDenied: String = "CAMERA_PERMISSION_DENIED"
      val libraryPermissionDenied: String = "LIBRARY_PERMISSION_DENIED"
    }

  }



  private var globalCallback: Callback? = null
  private var options: ReadableMap? = null


  override fun getName(): String {
    return "ImageSelector"
  }

  private fun launchCamera() {
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
      currentActivity.let { activity ->
        if (activity != null) {
          takePictureIntent.resolveActivity(activity.packageManager).also {
            cameraCaptureFile = File(Environment.getExternalStorageDirectory(), "react-native-image-selector_" + UUID.randomUUID().toString() + ".jpg")
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
    Intent(Intent.ACTION_GET_CONTENT).also { galleryIntent ->
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
    this.options = options
    this.globalCallback = callback

    var title: String? = "Pick Photos"
    var takePhotoButtonTitle: String? = "Take Photos"
    var chooseFromLibraryButtonTitle: String? = "Open Photo Gallery"
    var cancelButtonTitle: String? = "Cancel"

    this.options.let { option ->
      if (option != null) {
        if (option.getString("title") != null) {
          title = option.getString("title")
        }
        if (option.getString("takePhotoButtonTitle") != null) {
          takePhotoButtonTitle = option.getString("takePhotoButtonTitle")
        }
        if (option.getString("chooseFromLibraryButtonTitle") != null) {
          chooseFromLibraryButtonTitle = option.getString("chooseFromLibraryButtonTitle")
        }
        if (option.getString("cancelButtonTitle") != null) {
          cancelButtonTitle = option.getString("cancelButtonTitle")
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
      val baos = ByteArrayOutputStream()
      bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
      val b: ByteArray = baos.toByteArray()
      //Base64.de
      return Base64.encodeToString(b, Base64.NO_WRAP)
    }

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(activity, requestCode, resultCode, data)
//      resultCode == Activity.RESULT_CANCELED
      if (requestCode == IMAGE_CAPTURE_REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK) {
          cameraCaptureFile.let { cameraCaptureFile ->
            if (cameraCaptureFile != null) {
              val path: String? = Uri.fromFile(cameraCaptureFile).path
              val uriString = "file://$path"
              val fileSize: Long = cameraCaptureFile.length()
              val type: String = cameraCaptureFile.extension
              val fileName: String = cameraCaptureFile.name
              var base64EncodedString: String? = null
              path.let { parsedPath ->
                if (parsedPath != null) {
                  base64EncodedString = this.encodeImage(parsedPath)
                }
              }
              this.callbackInvoker.let { callback ->
                if (callback != null) {
                  val response = Arguments.createMap()
                  response.putString("path", path)
                  response.putString("uri", uriString)
                  response.putDouble("fileSize", fileSize.toDouble())
                  response.putString("type", "image/$type")
                  response.putString("fileName", fileName)
                  response.putString("data", base64EncodedString)
                  callback.invoke(null, response)
                  this.callbackInvoker = null
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

      if (requestCode == PICK_IMAGE_REQUEST_CODE) {
        if (resultCode == Activity.RESULT_OK) {
          data.let { parsedData ->
            if (parsedData != null) {
              val uri = parsedData.data
              uri.let { parsedUri ->
                if (parsedUri != null) {
                  this.callbackInvoker.let { callback ->
                    if (callback != null) {
                      this.context.let { parsedContext ->
                        if (parsedContext != null) {
                          val cursor = parsedContext.contentResolver.query(parsedUri, null, null, null, null)
                          cursor.let { parsedCursor ->
                            if (parsedCursor != null) {
                              if (parsedCursor.moveToFirst()) {
                                val sizeColumnIndex = parsedCursor.getColumnIndex(OpenableColumns.SIZE)
                                val displayNameColumnIndex = parsedCursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                                val fileSize = parsedCursor.getLong(sizeColumnIndex)
                                val fileName = parsedCursor.getString(displayNameColumnIndex)
                                val path = PathManager.getPathFromURI(parsedContext, parsedUri)
                                val uriString = "file://$path"
                                val type = MimeTypeMap.getFileExtensionFromUrl(uriString)
                                var base64EncodedString: String? = null
                                path.let { parsedPath ->
                                  if (parsedPath != null) {
                                    base64EncodedString = this.encodeImage(parsedPath)
                                  }
                                }
                                val response = Arguments.createMap()
                                response.putString("path", path)
                                response.putString("uri", uriString)
                                response.putDouble("fileSize", fileSize.toDouble())
                                response.putString("type", "image/$type")
                                response.putString("fileName", fileName)
                                response.putString("data", base64EncodedString)
                                callback.invoke(null, response)
                                this.callbackInvoker = null
                                parsedCursor.close()
                              }
                            }
                          }
                        }
                      }
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

