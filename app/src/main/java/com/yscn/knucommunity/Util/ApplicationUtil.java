package com.yscn.knucommunity.Util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by GwonHyeok on 2015. 1. 23..
 */
public class ApplicationUtil {

    private static ApplicationUtil instance;
    private static Typeface normalTypeface, boldTypeface;
    private static ArrayList<Activity> activities;

    private ApplicationUtil() {
    }

    public synchronized static ApplicationUtil getInstance() {
        if (instance == null) {
            instance = new ApplicationUtil();
        }
        checkFontInited();
        return instance;
    }

    private static void checkFontInited() {
        if (normalTypeface == null) {
            normalTypeface = Typeface.createFromAsset(ApplicationContextProvider.getContext().getAssets(), "fonts/NanumBarunGothic.otf");
        }

        if (boldTypeface == null) {
            boldTypeface = Typeface.createFromAsset(ApplicationContextProvider.getContext().getAssets(), "fonts/NanumBarunGothicBold.otf");
        }
    }

    public void finishAllActivity() {
        if (activities == null) {
            return;
        }
        for (Activity activity : activities) {
            if (activity != null) {
                activity.finish();
            }
        }
    }

    public ArrayList<Activity> getActivities() {
        return activities;
    }

    public void addActivity(Activity activity) {
        if (activities == null) {
            activities = new ArrayList<>();
        }
        activities.add(activity);
    }

    public Typeface getTypeFace(int style) {
        if (style == Typeface.BOLD) {
            return boldTypeface;
        } else if (style == Typeface.NORMAL) {
            return normalTypeface;
        } else {
            return normalTypeface;
        }
    }

    public void setTypeFace(View rootView) {
        try {
            if (rootView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) rootView;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    setTypeFace(viewGroup.getChildAt(i));
                }
            } else if (rootView instanceof TextView) {
                TextView textView = (TextView) rootView;
                if (textView.getTypeface() == null) {
                    textView.setTypeface(normalTypeface);
                } else if (textView.getTypeface().isBold()) {
                    textView.setTypeface(boldTypeface);
                } else {
                    textView.setTypeface(normalTypeface);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isOnlineNetwork() {
        Context context = ApplicationContextProvider.getContext();
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();

        if (networkInfos == null) {
            return false;
        }

        for (NetworkInfo networkInfo : networkInfos) {
            NetworkInfo.State state = networkInfo.getState();
            if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                return true;
            }
        }
        return false;
    }

    public Bitmap decodeSampledBitmap(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public int getScreenHeight() {
        Context context = ApplicationContextProvider.getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.y;
        } else {
            return display.getHeight();  // deprecated
        }
    }

    public int getScreenWidth() {
        Context context = ApplicationContextProvider.getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            return size.x;
        } else {
            return display.getWidth();  // deprecated
        }
    }

    public String UriToPath(Uri uri) {
        Context context = ApplicationContextProvider.getContext();

        if (isGooglePhotoUri(uri)) {
            return uri.getLastPathSegment();
        }

        if (Build.VERSION.SDK_INT >= 19) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;

                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            return null;
                        case "audio":
                            return null;
                    }

                    final String selection = MediaStore.Images.Media._ID + "=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            }
        }

        // content://media/external/images/media/....
        // 안드로이드 버전에 관계없이 경로가 com.android... 형식으로 집히지 않을 수 도 있음. [ 겔럭시S4 테스트 확인 ]
        if (isPathSDCardType(uri)) {

            final String selection = MediaStore.Images.Media._ID + "=?";
            final String[] selectionArgs = new String[]{
                    uri.getLastPathSegment()
            };

            return getDataColumn(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection, selectionArgs);
        }
        // File 접근일 경우
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isGooglePhotoUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private boolean isPathSDCardType(Uri uri) {
        return "external".equals(uri.getPathSegments().get(0));
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                ApplicationContextProvider.getContext().getResources().getDisplayMetrics());
    }
}
