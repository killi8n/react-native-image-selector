package com.reactnativeimageselector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

    File cacheFile = FileManager.writeFileFromInputStream(context, inputStream, FileManager.makeRandomCacheFile(context, options));
    if (cacheFile == null) {
      return null;
    }

    WritableMap response = Arguments.createMap();

    response.putString("path", cacheFile.getPath());
    response.putString("uri", "file://" + cacheFile.getPath());
    response.putDouble("fileSize", (double) cacheFile.length());
    response.putString("type", "image/jpeg");
    response.putString("fileName", cacheFile.getName());
    String base64EncodedString = FileManager.encodeBase64(cacheFile.getPath());
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

  public static File writeFileFromInputStream(Context context, InputStream inputStream, File targetFile) {
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

    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);

    try {
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
}
