package com.example.hanming.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ImagePHash {

    // 项目根目录路径
    private int size = 32;
    private int smallerSize = 8;

    public ImagePHash() {
        initCoefficients();
    }

    public ImagePHash(int size, int smallerSize) {
        this.size = size;
        this.smallerSize = smallerSize;

        initCoefficients();
    }

    private int distance(String s1, String s2) {
        Log.e("xhsun", "s1 = " + s1 + "\ns2 = " + s2);
        int counter = 0;
        for (int k = 0; k < s1.length(); k++) {
            if (s1.charAt(k) != s2.charAt(k)) {
                counter++;
            }
        }
        return counter;
    }


    /**
     * 返回一个二进制字符串，方便计算汉明距离（like. 001010111011100010）
     *
     * @param is
     * @return
     * @throws Exception
     */
    private String getHash(InputStream is) throws Exception {
        Bitmap bitmap = BitmapFactory.decodeStream(is);

        /*
         * 1. Reduce size. Like Average Hash, pHash starts with a small image.
         * However, the image is larger than 8x8; 32x32 is a good size. This is
         * really done to simplify the DCT computation and not because it is
         * needed to reduce the high frequencies.
         */
        bitmap = resize(bitmap, size, size);

        /*
         * 2. Reduce color. The image is reduced to a grayscale just to further
         * simplify the number of computations.
         */
//        bitmap = grayScale(bitmap);

        bitmap = bitmap2Gray(bitmap);

        double[][] vals = new double[size][size];

        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        Log.e("xhsun", "bitmap width = " + bitmapWidth + "\nheight = " + bitmapHeight);
        for (int x = 0; x < bitmapWidth; x++) {
            for (int y = 0; y < bitmapHeight; y++) {
                vals[x][y] = getBlue(bitmap, x, y);
            }
        }

        /*
         * 3. Compute the DCT. The DCT separates the image into a collection of
         * frequencies and scalars. While JPEG uses an 8x8 DCT, this algorithm
         * uses a 32x32 DCT.
         */
        double[][] dctVals = applyDCT(vals);

        /*
         * 4. Reduce the DCT. This is the magic step. While the DCT is 32x32,
         * just keep the top-left 8x8. Those represent the lowest frequencies in
         * the picture.
         */
        /*
         * 5. Compute the average value. Like the Average Hash, compute the mean
         * DCT value (using only the 8x8 DCT low-frequency values and excluding
         * the first term since the DC coefficient can be significantly
         * different from the other values and will throw off the average).
         */
        double total = 0;

        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                total += dctVals[x][y];
            }
        }
        total -= dctVals[0][0];
        double avg = total / (double) ((smallerSize * smallerSize) - 1);

        /*
         * 6. Further reduce the DCT. This is the magic step. Set the 64 hash
         * bits to 0 or 1 depending on whether each of the 64 DCT values is
         * above or below the average value. The result doesn't tell us the
         * actual low frequencies; it just tells us the very-rough relative
         * scale of the frequencies to the mean. The result will not vary as
         * long as the overall structure of the image remains the same; this can
         * survive gamma and color histogram adjustments without a problem.
         */
        StringBuilder hash = new StringBuilder();

        for (int x = 0; x < smallerSize; x++) {
            for (int y = 0; y < smallerSize; y++) {
                if (x != 0 && y != 0) {
                    hash.append(dctVals[x][y] > avg ? "1" : "0");
                }
            }
        }

        return hash.toString();
    }


    private Bitmap resize(Bitmap bitmap, int width, int height) {

        return Bitmap.createScaledBitmap(bitmap,width, height,true);
    }

    public static Bitmap bitmap2Gray(Bitmap bmSrc) {
        int width, height;
        height = bmSrc.getHeight();
        width = bmSrc.getWidth();
        Bitmap bmpGray = null;

        bmpGray = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGray);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmSrc, 0, 0, paint);

        return bmpGray;
    }

    private static int getBlue(Bitmap bitmap, int x, int y) {
        int pixel = bitmap.getPixel(x, y);
        return pixel & 0xff;
    }
    // DCT function stolen from
    // http://stackoverflow.com/questions/4240490/problems-with-dct-and-idct-algorithm-in-java

    private double[] c;

    private void initCoefficients() {
        c = new double[size];

        for (int i = 1; i < size; i++) {
            c[i] = 1;
        }
        c[0] = 1 / Math.sqrt(2.0);
    }

    private double[][] applyDCT(double[][] f) {
        int N = size;

        double[][] F = new double[N][N];
        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += Math.cos(((2 * i + 1) / (2.0 * N)) * u * Math.PI)
                                * Math.cos(((2 * j + 1) / (2.0 * N)) * v * Math.PI) * (f[i][j]);
                    }
                }
                sum *= ((c[u] * c[v]) / 4.0);
                F[u][v] = sum;
            }
        }
        return F;
    }


    public Map<String, Object> startCompare(String path1, String path2) {
        Log.e("xhsun", "path 1 = " + path1 + "\npath 2 = " + path2);
        // 项目根目录路径

        initCoefficients();

        String image1;
        String image2;
        Map<String, Object> resultMap = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            image1 = getHash(new FileInputStream(new File(path1)));
            image2 = getHash(new FileInputStream(new File(path2)));

            int hanming = distance(image1, image2);
            long endTime = System.currentTimeMillis();
            long costTime = endTime - startTime;

            float result = (64 - hanming) / (float) 64;

            System.out.println("图像不同像素点个数：" + hanming);
            System.out.println("相似度为：" + result);
            System.out.println("运行时间：" + costTime + "ms");

            resultMap.put("diff", hanming);
            resultMap.put("result", result);
            resultMap.put("time", costTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultMap;
    }

}