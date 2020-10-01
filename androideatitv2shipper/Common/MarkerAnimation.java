package com.ds.androideatitv2shipper.Common;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.os.Build;
import android.util.Property;
import android.view.animation.Interpolator;
import android.os.Handler;
import android.os.SystemClock;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class MarkerAnimation {
    public static void animateMarkerToGB(final Marker marker,
                                         LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator)
    {
        LatLng startPosition = marker.getPosition();
        Handler handler = new Handler();
        long start = SystemClock.uptimeMillis();
        Interpolator interpolator = new AccelerateDecelerateInterpolator();
        float durationInMs = 3000; // 3 sec

        handler.post(new Runnable() {
            long elapsed;
            float t,v;

            @Override
            public void run() {
                elapsed = SystemClock.uptimeMillis() - start;
                t=elapsed/durationInMs;
                v = interpolator.getInterpolation(t);

                marker.setPosition(latLngInterpolator.interpolate(v,startPosition,finalPosition));

                //Repeat til progress is complete
                if (t<1)
                {
                    //Post again 16ms later
                    handler.postDelayed(this, 16);
                }
            }
        });
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void animateMarkerToHC(final Marker marker,
                                         LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator)
    {
        LatLng startLocation = marker.getPosition();

        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            LatLng newPostion = latLngInterpolator.interpolate(v, startLocation,finalPosition);
            marker.setPosition(newPostion);
        });

        valueAnimator.setFloatValues(0,1);
        valueAnimator.setDuration(3000);
        valueAnimator.start();
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public static void animateMarkerToICS(final Marker marker,
                                         LatLng finalPosition,
                                         LatLngInterpolator latLngInterpolator)
    {
        TypeEvaluator<LatLng> TypeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };

        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, TypeEvaluator, finalPosition);
        animator.setDuration(30000);
        animator.start();
    }

}
