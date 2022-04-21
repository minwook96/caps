/* Copyright 2015 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.demo.env;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;

public class ImageUtils {

  static {
    try {
      System.loadLibrary("tensorflow_demo");
    } catch (UnsatisfiedLinkError e) {
    }
  }

  public static int getYUVByteSize(final int width, final int height) {
    final int ySize = width * height;

    final int uvSize = ((width + 1) / 2) * ((height + 1) / 2) * 2;

    return ySize + uvSize;
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   */
  public static void saveBitmap(final Bitmap bitmap) {
    saveBitmap(bitmap, "preview.png");
  }

  /**
   * Saves a Bitmap object to disk for analysis.
   *
   * @param bitmap The bitmap to save.
   * @param filename The location to save the bitmap to.
   */
  public static void saveBitmap(final Bitmap bitmap, final String filename) {
    final String root =
        Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "tensorflow";
    final File myDir = new File(root);

    if (!myDir.mkdirs()) {
    }

    final String fname = filename;
    final File file = new File(myDir, fname);
    if (file.exists()) {
      file.delete();
    }
    try {
      final FileOutputStream out = new FileOutputStream(file);
      bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
      out.flush();
      out.close();
    } catch (final Exception e) {
    }
  }

  static final int kMaxChannelValue = 262143;

  private static boolean useNativeConversion = true;

  public static void convertYUV420SPToARGB8888(
      byte[] input,
      int width,
      int height,
      int[] output) {
    if (useNativeConversion) {
      try {
        ImageUtils.convertYUV420SPToARGB8888(input, output, width, height, false);
        return;
      } catch (UnsatisfiedLinkError e) {
        useNativeConversion = false;
      }
    }

    final int frameSize = width * height;
    for (int j = 0, yp = 0; j < height; j++) {
      int uvp = frameSize + (j >> 1) * width;
      int u = 0;
      int v = 0;

      for (int i = 0; i < width; i++, yp++) {
        int y = 0xff & input[yp];
        if ((i & 1) == 0) {
          v = 0xff & input[uvp++];
          u = 0xff & input[uvp++];
        }

        output[yp] = YUV2RGB(y, u, v);
      }
    }
  }

  private static int YUV2RGB(int y, int u, int v) {
    y = (y - 16) < 0 ? 0 : (y - 16);
    u -= 128;
    v -= 128;

    int y1192 = 1192 * y;
    int r = (y1192 + 1634 * v);
    int g = (y1192 - 833 * v - 400 * u);
    int b = (y1192 + 2066 * u);

    r = r > kMaxChannelValue ? kMaxChannelValue : (r < 0 ? 0 : r);
    g = g > kMaxChannelValue ? kMaxChannelValue : (g < 0 ? 0 : g);
    b = b > kMaxChannelValue ? kMaxChannelValue : (b < 0 ? 0 : b);

    return 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
  }


  public static void convertYUV420ToARGB8888(
      byte[] yData,
      byte[] uData,
      byte[] vData,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      int[] out) {
    if (useNativeConversion) {
      try {
        convertYUV420ToARGB8888(
            yData, uData, vData, out, width, height, yRowStride, uvRowStride, uvPixelStride, false);
        return;
      } catch (UnsatisfiedLinkError e) {
        useNativeConversion = false;
      }
    }

    int yp = 0;
    for (int j = 0; j < height; j++) {
      int pY = yRowStride * j;
      int pUV = uvRowStride * (j >> 1);

      for (int i = 0; i < width; i++) {
        int uv_offset = pUV + (i >> 1) * uvPixelStride;

        out[yp++] = YUV2RGB(
            0xff & yData[pY + i],
            0xff & uData[uv_offset],
            0xff & vData[uv_offset]);
      }
    }
  }


  /**
   * Converts YUV420 semi-planar data to ARGB 8888 data using the supplied width and height. The
   * input and output must already be allocated and non-null. For efficiency, no error checking is
   * performed.
   *
   * @param input The array of YUV 4:2:0 input data.
   * @param output A pre-allocated array for the ARGB 8:8:8:8 output data.
   * @param width The width of the input image.
   * @param height The height of the input image.
   * @param halfSize If true, downsample to 50% in each dimension, otherwise not.
   */
  private static native void convertYUV420SPToARGB8888(
      byte[] input, int[] output, int width, int height, boolean halfSize);

  /**
   * Converts YUV420 semi-planar data to ARGB 8888 data using the supplied width
   * and height. The input and output must already be allocated and non-null.
   * For efficiency, no error checking is performed.
   *
   * @param y
   * @param u
   * @param v
   * @param uvPixelStride
   * @param width The width of the input image.
   * @param height The height of the input image.
   * @param halfSize If true, downsample to 50% in each dimension, otherwise not.
   * @param output A pre-allocated array for the ARGB 8:8:8:8 output data.
   */
  private static native void convertYUV420ToARGB8888(
      byte[] y,
      byte[] u,
      byte[] v,
      int[] output,
      int width,
      int height,
      int yRowStride,
      int uvRowStride,
      int uvPixelStride,
      boolean halfSize);

  /**
   * Converts YUV420 semi-planar data to RGB 565 data using the supplied width
   * and height. The input and output must already be allocated and non-null.
   * For efficiency, no error checking is performed.
   *
   * @param input The array of YUV 4:2:0 input data.
   * @param output A pre-allocated array for the RGB 5:6:5 output data.
   * @param width The width of the input image.
   * @param height The height of the input image.
   */
  private static native void convertYUV420SPToRGB565(
      byte[] input, byte[] output, int width, int height);

  /**
   * Converts 32-bit ARGB8888 image data to YUV420SP data.  This is useful, for
   * instance, in creating data to feed the classes that rely on raw camera
   * preview frames.
   *
   * @param input An array of input pixels in ARGB8888 format.
   * @param output A pre-allocated array for the YUV420SP output data.
   * @param width The width of the input image.
   * @param height The height of the input image.
   */
  private static native void convertARGB8888ToYUV420SP(
      int[] input, byte[] output, int width, int height);

  /**
   * Converts 16-bit RGB565 image data to YUV420SP data.  This is useful, for
   * instance, in creating data to feed the classes that rely on raw camera
   * preview frames.
   *
   * @param input An array of input pixels in RGB565 format.
   * @param output A pre-allocated array for the YUV420SP output data.
   * @param width The width of the input image.
   * @param height The height of the input image.
   */
  private static native void convertRGB565ToYUV420SP(
      byte[] input, byte[] output, int width, int height);

  /**
   * Returns a transformation matrix from one reference frame into another.
   * Handles cropping (if maintaining aspect ratio is desired) and rotation.
   *
   * @param srcWidth Width of source frame.
   * @param srcHeight Height of source frame.
   * @param dstWidth Width of destination frame.
   * @param dstHeight Height of destination frame.
   * @param applyRotation Amount of rotation to apply from one frame to another.
   *  Must be a multiple of 90.
   * @param maintainAspectRatio If true, will ensure that scaling in x and y remains constant,
   * cropping the image if necessary.
   * @return The transformation fulfilling the desired requirements.
   */
  public static Matrix getTransformationMatrix(
      final int srcWidth,
      final int srcHeight,
      final int dstWidth,
      final int dstHeight,
      final int applyRotation,
      final boolean maintainAspectRatio) {
    final Matrix matrix = new Matrix();

    if (applyRotation != 0) {
      if (applyRotation % 90 != 0) {
      }

      matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

      matrix.postRotate(applyRotation);
    }

    final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;

    final int inWidth = transpose ? srcHeight : srcWidth;
    final int inHeight = transpose ? srcWidth : srcHeight;

    if (inWidth != dstWidth || inHeight != dstHeight) {
      final float scaleFactorX = dstWidth / (float) inWidth;
      final float scaleFactorY = dstHeight / (float) inHeight;

      if (maintainAspectRatio) {
        final float scaleFactor = Math.max(scaleFactorX, scaleFactorY);
        matrix.postScale(scaleFactor, scaleFactor);
      } else {
        matrix.postScale(scaleFactorX, scaleFactorY);
      }
    }

    if (applyRotation != 0) {
      matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
    }

    return matrix;
  }
}
