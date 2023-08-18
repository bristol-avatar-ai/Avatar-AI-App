package com.example.avatar_ai_app.imagerecognition.tf;


/*Used from IntelliJ's automated code generation, with minor adaptation*/

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Integer;
import java.lang.Object;
import java.lang.String;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipException;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.CastOp;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.common.ops.QuantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

/**
 * Identify the most prominent object in the image from a set of n categories. */
public final class Model {
    @NonNull
    private final ImageProcessor imageProcessor;
    private int imageHeight;
    private ZipFile zipFile;
    private int imageWidth;
    @NonNull
    private final List<String> labels;
    @NonNull
    private final TensorProcessor probabilityPostProcessor;
    @NonNull
    private final AdaptedModel model;

    /**
     *
     * @param context
     * @param options
     * @param modelPath
     * @throws IOException
     */
    private Model(@NonNull Context context,
                  @NonNull AdaptedModel.Options options, String modelPath) throws IOException {
        model = AdaptedModel.createModel(context, modelPath, options);
        zipFile = createZipFile(model.getData());
        ImageProcessor.Builder imageProcessorBuilder = new ImageProcessor.Builder()
                .add(new ResizeOp(300, 300, ResizeMethod.NEAREST_NEIGHBOR))
                .add(new NormalizeOp(new float[] {0.0f}, new float[] {255.0f}))
                .add(new QuantizeOp(0f, 0.003921569f))
                .add(new CastOp(DataType.UINT8));
        imageProcessor = imageProcessorBuilder.build();
        TensorProcessor.Builder probabilityPostProcessorBuilder = new TensorProcessor.Builder()
                .add(new DequantizeOp((float)0, (float)0.00390625))
                .add(new NormalizeOp(new float[] {0.0f}, new float[] {1.0f}));
        probabilityPostProcessor = probabilityPostProcessorBuilder.build();
        labels = AdaptedFileUtil.loadLabels(getAssociatedFile("labels.txt"));
    }

    @NonNull
    public static Model newInstance(@NonNull Context context, String modelPath) throws IOException {
        return new Model(context, (new AdaptedModel.Options.Builder()).build(), modelPath);
    }

    @NonNull
    public static Model newInstance(@NonNull Context context,
                                    @NonNull AdaptedModel.Options options,
                                    String modelPath) throws IOException {
        return new Model(context, options, modelPath);
    }

    @NonNull
    public Outputs process(@NonNull TensorImage image) {
        imageHeight = image.getHeight();
        imageWidth = image.getWidth();
        TensorImage processedimage = imageProcessor.process(image);
        Outputs outputs = new Outputs(model);
        model.run(new Object[] {processedimage.getBuffer()}, outputs.getBuffer());
        return outputs;
    }

    private InputStream getAssociatedFile(String fileName) {
        return this.zipFile.getRawInputStream(fileName);
    }

    private static ZipFile createZipFile(ByteBuffer buffer) throws IOException {
        try {
            ByteBufferChannel byteBufferChannel = new ByteBufferChannel(buffer);
            return ZipFile.createFrom(byteBufferChannel);
        } catch (ZipException var2) {
            return null;
        }
    }

    public void close() {
        model.close();
    }

    @NonNull
    public Outputs process(@NonNull TensorBuffer image) {
        TensorBuffer processedimage = image;
        Outputs outputs = new Outputs(model);
        model.run(new Object[] {processedimage.getBuffer()}, outputs.getBuffer());
        return outputs;
    }

    public class Outputs {
        private TensorBuffer probability;

        private Outputs(AdaptedModel model) {
            this.probability = TensorBuffer.createFixedSize(model.getOutputTensorShape(0), DataType.UINT8);
        }

        @NonNull
        public List<Category> getProbabilityAsCategoryList() {
            return new TensorLabel(labels, probabilityPostProcessor.process(probability)).getCategoryList();
        }

        @NonNull
        public TensorBuffer getProbabilityAsTensorBuffer() {
            return probabilityPostProcessor.process(probability);
        }

        @NonNull
        private Map<Integer, Object> getBuffer() {
            Map<Integer, Object> outputs = new HashMap<>();
            outputs.put(0, probability.getBuffer());
            return outputs;
        }
    }
}