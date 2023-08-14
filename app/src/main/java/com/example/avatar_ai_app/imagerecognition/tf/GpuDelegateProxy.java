// 
// Decompiled by Procyon v0.5.36
// 

package com.example.avatar_ai_app.imagerecognition.tf;

import android.util.Log;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.tensorflow.lite.Delegate;

import java.io.Closeable;
import java.io.IOException;

class GpuDelegateProxy implements Delegate, Closeable
{
    private static final String TAG = "GpuDelegateProxy";
    private final Delegate proxiedDelegate;
    private final Closeable proxiedCloseable;

    @Nullable
    public static GpuDelegateProxy maybeNewInstance() {
        try {
            final Class<?> clazz = Class.forName("org.tensorflow.lite.gpu.GpuDelegate");
            final Object instance = clazz.getDeclaredConstructor((Class<?>[])new Class[0]).newInstance(new Object[0]);
            return new GpuDelegateProxy((Closeable) instance);
        }
        catch (ReflectiveOperationException e) {
            Log.e("GpuDelegateProxy", "Failed to create the GpuDelegate dynamically.", (Throwable)e);
            return null;
        }
    }

    public void close() {
        try {
            this.proxiedCloseable.close();
        }
        catch (IOException e) {
            Log.e("GpuDelegateProxy", "Failed to close the GpuDelegate.", (Throwable)e);
        }
    }

    public long getNativeHandle() {
        return this.proxiedDelegate.getNativeHandle();
    }

    private GpuDelegateProxy(final Object instance) {
        this.proxiedCloseable = (Closeable)instance;
        this.proxiedDelegate = (Delegate)instance;
    }
}
