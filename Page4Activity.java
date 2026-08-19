package lws.mynote;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Page4Activity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // 單線程執行器
    private boolean isDeviceInfoUpdated = false; // 防止重複更新的標誌

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page4);

        // 初始化 Python 平台
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // 獲取 UI 元素
        TextView authResultText = findViewById(R.id.auth_result_text);
        TextView deviceInfoText = findViewById(R.id.device_info_text);
        Button backButton = findViewById(R.id.backButton);

        // 返回按鈕
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Page4Activity.this, MainActivity.class);
            startActivity(intent);
        });

        // 子線程執行耗時操作
        executor.execute(() -> {
            try {
                Python python = Python.getInstance();
                PyObject authModule = python.getModule("Authenticator");

                // 調用 Python 方法獲取設備信息
                PyObject deviceInfo = authModule.callAttr("get_device_info");
                if (deviceInfo == null) {
                    throw new RuntimeException("Python 方法 'get_device_info' 返回了 null");
                }

                // 將 Python 返回的 Map<PyObject, PyObject> 轉換為 Map<String, PyObject>
                Map<String, PyObject> deviceInfoMap = new HashMap<>();
                for (Map.Entry<PyObject, PyObject> entry : deviceInfo.asMap().entrySet()) {
                    deviceInfoMap.put(entry.getKey().toString(), entry.getValue());
                }

                // 提取設備信息
                String macAddress = deviceInfoMap.containsKey("mac_address") && deviceInfoMap.get("mac_address") != null
                        ? deviceInfoMap.get("mac_address").toString()
                        : "未知 MAC 地址";

                String ipAddress = deviceInfoMap.containsKey("ip_address") && deviceInfoMap.get("ip_address") != null
                        ? deviceInfoMap.get("ip_address").toString()
                        : "未知內部 IP";

                String publicIp = deviceInfoMap.containsKey("public_ip") && deviceInfoMap.get("public_ip") != null
                        ? deviceInfoMap.get("public_ip").toString()
                        : "未知公共 IP";

                // 更新 UI
                String finalMacAddress = macAddress;
                String finalIpAddress = ipAddress;
                String finalPublicIp = publicIp;

                runOnUiThread(() -> {
                    if (!isDeviceInfoUpdated) {
                        String deviceInfoTextStr = "MAC 地址: " + finalMacAddress + "\n" +
                                "內部 IP: " + finalIpAddress + "\n" +
                                "公共 IP: " + finalPublicIp;

                        System.out.println("即將更新到 UI 的設備信息：" + deviceInfoTextStr);

                        deviceInfoText.setText(deviceInfoTextStr);
                        isDeviceInfoUpdated = true; // 防止重複更新
                    }
                });

                // 驗證設備是否授權
                PyObject isAuthorized = authModule.callAttr("is_authorized_user");
                boolean authorized = isAuthorized != null && isAuthorized.toBoolean();

                // 更新 UI：顯示授權結果
                runOnUiThread(() -> authResultText.setText(
                        authorized ? "訪問授權成功！" : "未授權的設備，拒絕訪問！"
                ));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> authResultText.setText("驗證過程中出現錯誤：" + e.getMessage()));
            }
        });
    }
}
