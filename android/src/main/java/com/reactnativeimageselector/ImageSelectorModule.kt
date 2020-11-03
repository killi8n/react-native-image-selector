package com.reactnativeimageselector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.io.File
import java.util.*


class ImageSelectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), PermissionListener {

  companion object {
    val LIBRARY_PERMISSION_REQUEST_CODE: Int = 1000000000
    val CAMERA_PERMISSION_REQUEST_CODE: Int = 1010000000
    val IMAGE_CAPTURE_REQUEST_CODE: Int = 1020000000
    val PICK_IMAGE_REQUEST_CODE: Int = 1030000000
    var cameraCaptureURI: Uri? = null
    var cameraCaptureFile: File? = null
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
  fun launchPicker(callback: Callback) {
    this.globalCallback = callback
    currentActivity.let {
      it
      if (it != null) {
        if (!it.isFinishing) {
          val dialogBuilder = AlertDialog.Builder(it)
            .setTitle("사진 선택")
            .setItems(arrayOf("사진 촬영", "앨범에서 가져오기")) { dialog, which ->
              if (which == 0) {
                this.checkCameraPermission()
              }
              if (which == 1) {
                this.checkLibraryPermission()
              }
            }
            .setNeutralButton("취소", { dialog, which ->
              val error = Arguments.createMap()
              error.putString("error", "USER_CACNEL")
              callback(error)
              this.globalCallback = null
            })
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
            error.putString("error", "USER_CACNEL")
            callback(error)
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
            error.putString("error", "USER_CACNEL")
            callback(error)
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

    override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(activity, requestCode, resultCode, data)
      if (requestCode == IMAGE_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
        cameraCaptureFile.let { cameraCaptureFile ->
          if (cameraCaptureFile != null) {
            val uriString: String = "file://" + Uri.fromFile(cameraCaptureFile).path
            val fileSize: Long = cameraCaptureFile.length()
            val type: String = cameraCaptureFile.extension
            val fileName: String = cameraCaptureFile.name
            this.callbackInvoker.let { callback ->
              if (callback != null) {
                var response = Arguments.createMap()
                response.putString("uri", uriString)
                response.putDouble("fileSize", fileSize.toDouble())
                response.putString("type", "image/" + type)
                response.putString("fileName", fileName)
                callback.invoke(null, response)
                this.callbackInvoker = null
              }
            }
          }
        }
      }

      if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
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
                              val realPath = "file://" + PathManager.getPathFromURI(parsedContext, parsedUri)
                              var type = MimeTypeMap.getFileExtensionFromUrl(realPath)
                              var response = Arguments.createMap()
                              response.putString("uri", realPath)
                              response.putDouble("fileSize", fileSize.toDouble())
                              response.putString("type", "image/" + type)
                              response.putString("fileName", fileName)
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
    }
  }
}

