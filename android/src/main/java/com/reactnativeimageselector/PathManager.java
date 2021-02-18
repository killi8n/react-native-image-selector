package com.reactnativeimageselector;

import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class PathManager {
  public static String getPathFromURI(final ReactContext context, final Uri uri, final ReadableMap options) {

    final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

    // DocumentProvider
    if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
      // ExternalStorageProvider
      if (isExternalStorageDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        if ("primary".equalsIgnoreCase(type)) {
          return Environment.getExternalStorageDirectory() + "/" + split[1];
        }
      }
      // DownloadsProvider
      else if (isDownloadsDocument(uri)) {

        final String id = DocumentsContract.getDocumentId(uri);
        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

        return getDataColumn(context, contentUri, null, null);
      }
      // MediaProvider
      else if (isMediaDocument(uri)) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];

        Uri contentUri = null;
        if ("image".equals(type)) {
          contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if ("video".equals(type)) {
          contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else if ("audio".equals(type)) {
          contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        final String selection = "_id=?";
        final String[] selectionArgs = new String[] {
          split[1]
        };

        return getDataColumn(context, contentUri, selection, selectionArgs);
      } else {
        return PathManager.getImagePathFromInputStreamUri(context, uri, options);
      }
    }
    // MediaStore (and general)
    else if ("content".equalsIgnoreCase(uri.getScheme())) {
      String res = getDataColumn(context, uri, null, null);
      // Return the remote address
      if (res == null && isGooglePhotosUri(uri)) {
        return PathManager.getImagePathFromInputStreamUri(context, uri, options);
      } else {
        return res;
      }
    }
    // File
    else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }

    return PathManager.getImagePathFromInputStreamUri(context, uri, options);
  }

  public static String getExtensionFromString(String uriString) {
    try {
      String type = MimeTypeMap.getFileExtensionFromUrl(uriString);
      if (type.equals("") && uriString.contains(".")) {
        type = uriString.substring(uriString.lastIndexOf(".") + 1);
      }
      return type;
    } catch (Exception e) {
      return "jpg";
    }
  }

  public static String getDataColumn(ReactContext context, Uri uri, String selection, String[] selectionArgs) {
    Cursor cursor = null;
    final String column = MediaStore.Images.Media.DATA;
    final String[] projection = {
      column,
    };

    String dataColumnString = null;

    try {

      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
        null);
      if (cursor != null && cursor.moveToFirst()) {
        final int column_index = cursor.getColumnIndexOrThrow(column);
        dataColumnString = cursor.getString(column_index);
      }
    } catch (Exception e) {
        dataColumnString = null;
    } finally {
      if (cursor != null) {
        cursor.close();
      }
      return dataColumnString;
    }
  }

  public static boolean isExternalStorageDocument(Uri uri) {
    return "com.android.externalstorage.documents".equals(uri.getAuthority());
  }

  public static boolean isDownloadsDocument(Uri uri) {
    return "com.android.providers.downloads.documents".equals(uri.getAuthority());
  }

  public static boolean isMediaDocument(Uri uri) {
    return "com.android.providers.media.documents".equals(uri.getAuthority());
  }

  /**
   * @param uri The Uri to check.
   * @return Whether the Uri authority is Google Photos.
   * @author paulburke
   */
  public static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority()) ||
      "com.google.android.apps.photos.contentprovider".equals(uri.getAuthority());
  }

  public static String getImagePathFromInputStreamUri(ReactContext context, Uri uri, final ReadableMap options) {
    InputStream inputStream = null;
    String filePath = null;

    if (uri.getAuthority() != null) {
      try {
        inputStream = context.getContentResolver().openInputStream(uri); // context needed
        File photoFile = PathManager.writeFileFromInputStream(context, inputStream, options);

        filePath = photoFile.getPath();

      } catch (FileNotFoundException e) {
        // log
        e.printStackTrace();
      } catch (IOException e) {
        // log
        e.printStackTrace();
      }finally {
        try {
          inputStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return filePath;
  }

  public static File writeFileFromInputStream(ReactContext context, InputStream inputStream, final ReadableMap options) throws IOException {
    File targetFile = null;

    if (inputStream != null) {
      int read;
      byte[] buffer = new byte[8 * 1024];

      targetFile = createCacheFile(context, options);
      OutputStream outputStream = new FileOutputStream(targetFile);
      while ((read = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, read);
      }
      outputStream.flush();

      try {
        outputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return targetFile;
  }

  @NotNull
  private static File createCacheFile(ReactContext context, final ReadableMap options) {
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
}
