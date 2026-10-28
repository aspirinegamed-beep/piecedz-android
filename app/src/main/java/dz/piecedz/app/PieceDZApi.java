package dz.piecedz.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PieceDZApi {
    private static final ExecutorService EXECUTOR =
        Executors.newSingleThreadExecutor();
    private static final Handler MAIN =
        new Handler(Looper.getMainLooper());

    private PieceDZApi() {}

    public interface ListingsCallback {
        void onSuccess(JSONArray items);
        void onError(String message);
    }

    public static void getListings(String baseUrl, ListingsCallback callback) {
        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String base = baseUrl.trim().replaceAll("/+$", "");
                if (!base.startsWith("https://")) {
                    throw new Exception("HTTPS_REQUIRED");
                }

                URL url = new URL(base + "/api/listings");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestProperty("Accept", "application/json");

                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new Exception("HTTP_" + status);
                }

                StringBuilder response = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject json = new JSONObject(response.toString());
                JSONArray items = json.optJSONArray("items");
                if (items == null) {
                    throw new Exception("INVALID_RESPONSE");
                }

                MAIN.post(() -> callback.onSuccess(items));

            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null) message = "CONNECTION_FAILED";
                final String error = message;
                MAIN.post(() -> callback.onError(error));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
