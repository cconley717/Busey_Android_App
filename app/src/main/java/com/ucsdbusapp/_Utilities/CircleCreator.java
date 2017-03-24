package com.ucsdbusapp._Utilities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by Chris on 6/23/2016.
 */
public class CircleCreator
{
    public static Bitmap createColoredCircle(int radius)
    {
        Bitmap b = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);

        Paint paint = new Paint();
        paint.setStrokeWidth(10);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        paint.setColor(Color.rgb(255, 255, 0));
        canvas.drawCircle(radius, radius, radius, paint);

        paint.setColor(Color.rgb(0, 0, 255));
        canvas.drawCircle(radius, radius, ((float)radius) / 2.5f, paint);

        return b;
    }
}
