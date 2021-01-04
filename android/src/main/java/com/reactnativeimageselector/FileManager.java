package com.reactnativeimageselector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class FileManager {
  public static WritableMap createCacheFile(Context context, Uri fileUri, ReadableMap options) {

    InputStream inputStream = FileManager.makeInputStream(context, fileUri);

    if (inputStream == null) {
      return null;
    }

    File cacheFile = FileManager.writeFileFromInputStream(inputStream, FileManager.makeRandomCacheFile(context, options), fileUri);
    if (cacheFile == null) {
      return null;
    }

    WritableMap response = Arguments.createMap();

    String photoPath = cacheFile.getPath();
    double fileSize = (double) cacheFile.length();
    String type = "image/jpeg";
    String fileName = cacheFile.getName();

    response.putString("path", photoPath);
    response.putString("uri", "file://" + photoPath);
    String base64EncodedString = FileManager.encodeBase64(photoPath);
    response.putDouble("fileSize", fileSize);
    response.putString("type", type);
    response.putString("fileName", fileName);
    response.putString("data", base64EncodedString);
    return response;
  }

  public static File makeRandomCacheFile(Context context, final ReadableMap options) {
    String uuid = UUID.randomUUID().toString() + ".jpg";

    try {
      ReadableMap storageOptions = options.getMap("storageOptions");
      if (storageOptions == null) {
        throw new NoSuchKeyException("storageOptions null");
      }
      String middlePath = storageOptions.getString("path");
      if (middlePath == null) {
        throw new NoSuchKeyException("path null");
      }
      File path = new File(context.getExternalCacheDir(), middlePath);
      File result = new File(path, uuid);
      if (path.exists() || path.mkdir()) {
        return result;
      } else {
        return new File(context.getExternalCacheDir(), uuid);
      }
    } catch (NoSuchKeyException e) {
      return new File(context.getExternalCacheDir(), uuid);
    }
  }

  public static InputStream makeInputStream(Context context, Uri fileUri) {
    InputStream inputStream = null;
    if (fileUri.getAuthority() != null) {
      try {
        inputStream = context.getContentResolver().openInputStream(fileUri);
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }
    }
    return inputStream;
  }

  public static File writeFileFromInputStream(InputStream inputStream, File targetFile, Uri fileUri) {
    int read;
    byte[] buffer = new byte[8 * 1024];
    OutputStream outputStream = null;
    try {
      outputStream = new FileOutputStream(targetFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

    if (outputStream == null) {
      return null;
    }

    try {
      int originalOrientation = getOrientation(fileUri);

      Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

      switch (originalOrientation) {
        case ExifInterface.ORIENTATION_ROTATE_90:
          bitmap = rotateImage(bitmap, 90);
          break;
        case ExifInterface.ORIENTATION_ROTATE_180:
          bitmap = rotateImage(bitmap, 180);
          break;
        case ExifInterface.ORIENTATION_ROTATE_270:
          bitmap = rotateImage(bitmap, 270);
          break;
        case  ExifInterface.ORIENTATION_NORMAL:
        default:
          break;
      }

      bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      outputStream.flush();
      outputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return targetFile;
  }

  public static String encodeBase64(String path) {
    File imageFile = new File(path);
    FileInputStream fileInputStream = null;

    try {
      fileInputStream = new FileInputStream(imageFile);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
    if (bitmap == null) {
      return null;
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
    byte[] byteArray = baos.toByteArray();
    return Base64.encodeToString(byteArray, Base64.NO_WRAP);
  }

  public static int getOrientation(Uri fileUri) {
    try {
      ExifInterface exif = new ExifInterface(fileUri.getPath());
      return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
    } catch (IOException e) {
      e.printStackTrace();
      return ExifInterface.ORIENTATION_UNDEFINED;
    }
  }


  public static Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }
}
