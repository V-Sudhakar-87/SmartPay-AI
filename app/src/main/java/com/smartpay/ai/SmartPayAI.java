
package com.smartpay.ai;

import android.content.Context;
import android.util.Log;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import ai.onnxruntime.OnnxSequence;
import ai.onnxruntime.OnnxMap;

public class SmartPayAI {

    private static final String TAG = "SMARTPAY_AI";

    private OrtEnvironment environment;
    private OrtSession session;

    // LabelEncoder class order from Python training
    private static final String[] LABELS = {
            "EXPENSE",
            "FAILED",
            "INCOME",
            "REVERSAL",
            "TRANSFER"
    };

    public static class Prediction {

        public String transactionType;
        public float confidence;
        public int classIndex;

        public Prediction(
                String transactionType,
                float confidence,
                int classIndex
        ) {
            this.transactionType = transactionType;
            this.confidence = confidence;
            this.classIndex = classIndex;
        }
    }

    // --------------------------------
    // INITIALIZE MODEL
    // --------------------------------

    public boolean initialize(Context context) {

        try {

            environment = OrtEnvironment.getEnvironment();

            InputStream inputStream =
                    context.getAssets().open(
                            "smartpay_lightgbm.onnx"
                    );

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] data = new byte[4096];

            int length;

            while ((length = inputStream.read(data)) != -1) {
                buffer.write(data, 0, length);
            }

            inputStream.close();

            byte[] modelBytes = buffer.toByteArray();

            OrtSession.SessionOptions options =
                    new OrtSession.SessionOptions();

            try {
                session = environment.createSession(
                        modelBytes,
                        options
                );
            } finally {
                options.close();
            }

            Log.d(TAG, "AI MODEL LOADED SUCCESSFULLY");

            for (Map.Entry<String, ai.onnxruntime.NodeInfo> entry :
                    session.getInputInfo().entrySet()) {

                Log.d(TAG,
                        "INPUT: " + entry.getKey()
                                + " | " + entry.getValue().getInfo());
            }

            for (Map.Entry<String, ai.onnxruntime.NodeInfo> entry :
                    session.getOutputInfo().entrySet()) {

                Log.d(TAG,
                        "OUTPUT: " + entry.getKey()
                                + " | " + entry.getValue().getInfo());
            }

            return true;

        } catch (Exception e) {

            Log.e(TAG, "AI MODEL LOAD FAILED", e);
            return false;
        }
    }

    // --------------------------------
    // PREDICT TRANSACTION TYPE
    // --------------------------------

    public Prediction predict(float[] features) {

        if (session == null) {
            Log.e(TAG, "MODEL NOT INITIALIZED");
            return null;
        }

        if (features == null || features.length != 245) {

            Log.e(TAG, "INVALID FEATURE VECTOR");

            return null;
        }

        try {

            String inputName =
                    session.getInputNames().iterator().next();

            try (OnnxTensor inputTensor =
                         OnnxTensor.createTensor(
                                 environment,
                                 FloatBuffer.wrap(features),
                                 new long[]{1, features.length}
                         )) {

                Map<String, OnnxTensor> inputs =
                        new HashMap<>();

                inputs.put(inputName, inputTensor);

                try (OrtSession.Result result =
                             session.run(inputs)) {

                    // Read predicted label
                    Object labelValue =
                            result.get("label")
                                    .get()
                                    .getValue();

                    int predictedIndex =
                            extractLabelIndex(labelValue);

                    if (predictedIndex < 0 ||
                            predictedIndex >= LABELS.length) {

                        Log.e(TAG,
                                "INVALID PREDICTED LABEL: "
                                        + predictedIndex);

                        return null;
                    }

                    // Read class probabilities
                    OnnxValue probabilityValue =
                            result.get("probabilities").get();

                    float confidence =
                            extractProbability(
                                    probabilityValue,
                                    predictedIndex
                            );

                    Prediction prediction =
                            new Prediction(
                                    LABELS[predictedIndex],
                                    confidence,
                                    predictedIndex
                            );

                    Log.d(TAG,
                            "PREDICTION: "
                                    + prediction.transactionType);

                    Log.d(TAG,
                            "CONFIDENCE: "
                                    + prediction.confidence);

                    return prediction;
                }
            }

        } catch (Exception e) {

            Log.e(TAG, "PREDICTION FAILED", e);

            return null;
        }
    }

    // --------------------------------
    // EXTRACT LABEL INDEX
    // --------------------------------

    private int extractLabelIndex(Object value) {

        if (value instanceof long[]) {

            return (int) ((long[]) value)[0];
        }

        if (value instanceof Long) {

            return ((Long) value).intValue();
        }

        if (value instanceof Number) {

            return ((Number) value).intValue();
        }

        if (value instanceof int[]) {

            return ((int[]) value)[0];
        }

        throw new IllegalArgumentException(
                "Unsupported label output: "
                        + value.getClass().getName()
        );
    }

    // --------------------------------
    // EXTRACT CLASS PROBABILITY
    // --------------------------------

    private float extractProbability(
            OnnxValue value,
            int predictedIndex
    ) {

        try {

            Map<?, ?> probabilityMap = null;

            // ONNX Sequence output
            if (value instanceof OnnxSequence) {

                OnnxSequence sequence =
                        (OnnxSequence) value;

                List<? extends OnnxValue> list =
                        sequence.getValue();

                if (!list.isEmpty()) {

                    OnnxValue firstValue = list.get(0);

                    if (firstValue instanceof OnnxMap) {

                        OnnxMap onnxMap =
                                (OnnxMap) firstValue;

                        probabilityMap =
                                onnxMap.getValue();
                    }
                }
            }

            // Direct ONNX Map output
            else if (value instanceof OnnxMap) {

                probabilityMap =
                        ((OnnxMap) value).getValue();
            }

            if (probabilityMap == null) {

                Log.w(TAG,
                        "Probability map not found");

                return 0f;
            }

            for (Map.Entry<?, ?> entry :
                    probabilityMap.entrySet()) {

                if (entry.getKey() instanceof Number &&
                        entry.getValue() instanceof Number) {

                    int index =
                            ((Number) entry.getKey()).intValue();

                    float probability =
                            ((Number) entry.getValue()).floatValue();

                    if (index == predictedIndex) {

                        Log.d(TAG,
                                "MATCHED CLASS: " + index);

                        Log.d(TAG,
                                "RAW CONFIDENCE: " + probability);

                        return probability;
                    }
                }
            }

            Log.w(TAG,
                    "Predicted class probability not found");

            return 0f;

        } catch (Exception e) {

            Log.e(TAG,
                    "CONFIDENCE EXTRACTION FAILED", e);

            return 0f;
        }
    }

    // --------------------------------
    // CLOSE MODEL
    // --------------------------------

    public void close() {

        try {

            if (session != null) {

                session.close();
                session = null;
            }

        } catch (Exception e) {

            Log.e(TAG, "AI CLOSE ERROR", e);
        }
    }
}
