
package com.smartpay.ai;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.Iterator;

public class SmartPayTfidf {

    private static final String TAG = "SMARTPAY_TFIDF";

    private final Map<String, Integer> vocabulary =
            new HashMap<>();

    private double[] idf;

    private int featureCount = 0;

    // Android-compatible English word tokenizer
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("\\b[a-zA-Z0-9_]{2,}\\b");

    // --------------------------------
    // LOAD TF-IDF JSON
    // --------------------------------

    public boolean initialize(Context context) {

        try {

            InputStream input =
                    context.getAssets().open("tfidf_data.json");

            ByteArrayOutputStream buffer =
                    new ByteArrayOutputStream();

            byte[] data = new byte[4096];

            int length;

            while ((length = input.read(data)) != -1) {
                buffer.write(data, 0, length);
            }

            input.close();

            String jsonText =
                    new String(
                            buffer.toByteArray(),
                            StandardCharsets.UTF_8
                    );

            JSONObject root = new JSONObject(jsonText);

            // Load vocabulary
            JSONObject vocabJson =
                    root.getJSONObject("vocabulary");

            Iterator<String> keys = vocabJson.keys();

            while (keys.hasNext()) {

                String key = keys.next();

                vocabulary.put(
                        key,
                        vocabJson.getInt(key)
                );
            }

            // Load IDF
            JSONArray idfArray =
                    root.getJSONArray("idf");

            featureCount = idfArray.length();

            idf = new double[featureCount];

            for (int i = 0; i < featureCount; i++) {

                idf[i] = idfArray.getDouble(i);
            }

            // Validate vocabulary indexes
            for (Integer index : vocabulary.values()) {

                if (index < 0 || index >= featureCount) {

                    throw new IllegalStateException(
                            "Invalid vocabulary index: " + index
                    );
                }
            }

            Log.d(TAG,
                    "TF-IDF LOADED SUCCESSFULLY");

            Log.d(TAG,
                    "Vocabulary size: " + vocabulary.size());

            Log.d(TAG,
                    "Feature count: " + featureCount);

            return true;

        } catch (Exception e) {

            featureCount = 0;
            idf = null;
            vocabulary.clear();

            Log.e(TAG,
                    "TF-IDF LOAD FAILED",
                    e);

            return false;
        }
    }

    // --------------------------------
    // CONVERT SMS TO TF-IDF VECTOR
    // --------------------------------

    public float[] convert(String message) {

        float[] vector = new float[featureCount];

        if (idf == null ||
                message == null ||
                message.trim().isEmpty()) {

            return vector;
        }

        String text =
                message.toLowerCase(Locale.ROOT);

        Matcher matcher =
                TOKEN_PATTERN.matcher(text);

        ArrayList<String> tokens =
                new ArrayList<>();

        // Tokenization
        while (matcher.find()) {

            tokens.add(matcher.group());
        }

        // Unigrams
        for (String token : tokens) {

            addFeature(token, vector);
        }

        // Bigrams
        for (int i = 0; i < tokens.size() - 1; i++) {

            String bigram =
                    tokens.get(i) + " " + tokens.get(i + 1);

            addFeature(bigram, vector);
        }

        // L2 Normalization
        double squaredSum = 0;

        for (float value : vector) {

            squaredSum += value * value;
        }

        double norm =
                Math.sqrt(squaredSum);

        if (norm > 0) {

            for (int i = 0; i < vector.length; i++) {

                vector[i] /= (float) norm;
            }
        }

        return vector;
    }

    // --------------------------------
    // ADD VOCABULARY FEATURE
    // --------------------------------

    private void addFeature(
            String token,
            float[] vector
    ) {

        Integer index =
                vocabulary.get(token);

        if (index != null &&
                index >= 0 &&
                index < featureCount) {

            vector[index] += (float) idf[index];
        }
    }

    // --------------------------------
    // GET FEATURE COUNT
    // --------------------------------

    public int getFeatureCount() {

        return featureCount;
    }
}
