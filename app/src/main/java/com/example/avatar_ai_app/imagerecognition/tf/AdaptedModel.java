// 
// Decompiled by Procyon v0.5.36
// 

package com.example.avatar_ai_app.imagerecognition.tf;

import android.content.Context;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.support.common.SupportPreconditions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.Map;

public class AdaptedModel
{
    private final Interpreter interpreter;
    private final String modelPath;
    private final MappedByteBuffer byteModel;
    private final GpuDelegateProxy gpuDelegateProxy;

    public static AdaptedModel createModel(@NonNull final Context context, @NonNull final String modelPath) throws IOException {
        return createModel(context, modelPath, new Options.Builder().build());
    }

    public static AdaptedModel createModel(@NonNull final Context context, @NonNull final String modelPath, @NonNull final Options options) throws IOException {
        SupportPreconditions.checkNotEmpty(modelPath, "Model path in the asset folder cannot be empty.");
        final MappedByteBuffer byteModel = AdaptedFileUtil.loadMappedFile(context, modelPath);
        return createModel(byteModel, modelPath, options);
    }

    public static AdaptedModel createModel(@NonNull final MappedByteBuffer byteModel, @NonNull final String modelPath, @NonNull final Options options) {
        final Interpreter.Options interpreterOptions = new Interpreter.Options();
        GpuDelegateProxy gpuDelegateProxy = null;
        switch (options.device) {
            case NNAPI: {
                interpreterOptions.setUseNNAPI(true);
                break;
            }
            case GPU: {
                gpuDelegateProxy = GpuDelegateProxy.maybeNewInstance();
                SupportPreconditions.checkArgument(gpuDelegateProxy != null, "Cannot inference with GPU. Did you add \"tensorflow-lite-gpu\" as dependency?");
                interpreterOptions.addDelegate((Delegate)gpuDelegateProxy);
                break;
            }
        }
        interpreterOptions.setNumThreads(options.numThreads);
        final Interpreter interpreter = new Interpreter((ByteBuffer)byteModel, interpreterOptions);
        return new AdaptedModel(modelPath, byteModel, interpreter, gpuDelegateProxy);
    }


    @NonNull
    public MappedByteBuffer getData() {
        return this.byteModel;
    }

    @NonNull
    public String getPath() {
        return this.modelPath;
    }

    public Tensor getInputTensor(final int inputIndex) {
        return this.interpreter.getInputTensor(inputIndex);
    }

    public Tensor getOutputTensor(final int outputIndex) {
        return this.interpreter.getOutputTensor(outputIndex);
    }

    public int[] getOutputTensorShape(final int outputIndex) {
        return this.interpreter.getOutputTensor(outputIndex).shape();
    }

    public void run(@NonNull final Object[] inputs, @NonNull final Map<Integer, Object> outputs) {
        this.interpreter.runForMultipleInputsOutputs(inputs, (Map)outputs);
    }

    public void close() {
        if (this.interpreter != null) {
            this.interpreter.close();
        }
        if (this.gpuDelegateProxy != null) {
            this.gpuDelegateProxy.close();
        }
    }

    private AdaptedModel(@NonNull final String modelPath, @NonNull final MappedByteBuffer byteModel, @NonNull final Interpreter interpreter, @Nullable final GpuDelegateProxy gpuDelegateProxy) {
        this.modelPath = modelPath;
        this.byteModel = byteModel;
        this.interpreter = interpreter;
        this.gpuDelegateProxy = gpuDelegateProxy;
    }
    
    public enum Device
    {
        CPU, 
        NNAPI, 
        GPU;
    }
    
    public static class Options
    {
        private final Device device;
        private final int numThreads;
        
        private Options(final Builder builder) {
            this.device = builder.device;
            this.numThreads = builder.numThreads;
        }
        
        public static class Builder
        {
            private Device device;
            private int numThreads;
            
            public Builder() {
                this.device = Device.CPU;
                this.numThreads = 1;
            }
            
            public Builder setDevice(final Device device) {
                this.device = device;
                return this;
            }
            
            public Builder setNumThreads(final int numThreads) {
                this.numThreads = numThreads;
                return this;
            }
            
            public Options build() {
                return new Options(this);
            }
        }
    }
    
    @Deprecated
    public static class Builder
    {
        private Device device;
        private int numThreads;
        private final String modelPath;
        private final MappedByteBuffer byteModel;
        
        @NonNull
        public Builder(@NonNull final Context context, @NonNull final String modelPath) throws IOException {
            this.device = Device.CPU;
            this.numThreads = 1;
            this.modelPath = modelPath;
            this.byteModel = AdaptedFileUtil.loadMappedFile(context, modelPath);
        }
        
        @NonNull
        public Builder setDevice(final Device device) {
            this.device = device;
            return this;
        }
        
        @NonNull
        public Builder setNumThreads(final int numThreads) {
            this.numThreads = numThreads;
            return this;
        }
        
        @NonNull
        public AdaptedModel build() {
            final Options options = new Options.Builder().setNumThreads(this.numThreads).setDevice(this.device).build();
            return AdaptedModel.createModel(this.byteModel, this.modelPath, options);
        }
    }
}
