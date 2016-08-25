package io.cmmc.netpieclient;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.cmmc.netpieclient.microgear.OauthNetpieLibrary;

public class MainActivity extends AppCompatActivity {
    MqttAndroidClient mqttAndroidClient;
    public OauthNetpieLibrary oauthNetpieLibrary;
    public String name = "microgear.cache";
    //    public static String appidvalue, keyvalue, secretvalue;
    public File tempFile;
    public File cDir;
    public Context context;

    public String appid = "CMMC";
    public String appsecret = "ahKOgQWSE6h87Anc9QP5HJgdQ";
    public String appkey = "60qturoh80sRMXq";
    private String TAG = "MainActivity";
    private String serverUri;
    private String mqttuser, mqttclientid, mqttpassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        oauthNetpieLibrary = new OauthNetpieLibrary();
        context = this;
        cDir = context.getCacheDir();
        tempFile = new File(cDir.getPath() + "/" + name);
        String a = oauthNetpieLibrary.create(appid, appkey, appsecret, tempFile.toString());
        Log.d(TAG, "onCreate: " + a);
        if (a.equals("yes")) {
            brokerconnect(appid, appkey, appsecret);
        } else if (a.equals("id")) {
            Log.d(TAG, "onCreate: App id Invalid");
        } else if (a.equals("secretandid")) {
            Log.d(TAG, "onCreate: App id,Key or Secret Invalid");
        } else {
//            brokerconnect(appid, key, secret);
//            context.bindService(new Intent(context, MicrogearService.class), serviceConnection, 0);
        }
    }

    private void setupMqttClient() {
        Log.d(TAG, "> clientId: " + mqttclientid);
        Log.d(TAG, "> mqttUser: " + mqttuser);
        Log.d(TAG, "> mqttPassword: " + mqttpassword);
        serverUri = "tcp://gb.netpie.io:1883";

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, mqttclientid);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    addToHistory("[CON] Reconnected to : " + serverURI);
                } else {
                    addToHistory("[CON] Connected to: " + serverURI);
                }
//                subscribeToTopic();
            }

            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + new String(message.getPayload()));
//                textView.setText(topic + " => " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true);
        mqttConnectOptions.setUserName(mqttuser);
        mqttConnectOptions.setPassword(mqttpassword.toCharArray());
//        mqttConnectOptions.setAutomaticReconnect(true);

        try {
            Log.d(TAG, "setupMqttClient: BEING CONNECTED..");
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, ">>>> onSuccess: ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "++ >>>> onFailure: "+ exception.getCause());
                    asyncActionToken.getException();
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void brokerconnect(String appid, String key, String secret) {
        File fi = new File(tempFile.toString());
        BufferedReader br;
        StringBuilder sb = new StringBuilder();
        String line;
        String secrettoken, secretid, hkey, ckappkey;
        FileInputStream fis;
        try {
            fis = new FileInputStream(tempFile.toString());
            br = new BufferedReader(new InputStreamReader(fis));
            while ((line = br.readLine()) != null) {
                System.out.print(line);
                sb.append(line);
            }
            Log.d(TAG, "NAT: " + sb.toString());
            JSONObject json = new JSONObject(sb.toString());
            mqttuser = json.getJSONObject("_").getString("key");
            secrettoken = json.getJSONObject("_").getJSONObject("accesstoken").getString("secret");
            mqttclientid = json.getJSONObject("_").getJSONObject("accesstoken").getString("token");
            secretid = secret;
            hkey = secrettoken + "&" + secretid; //okay
            long date = new Date().getTime();
            date = date / 1000;
            mqttuser = mqttuser + "%" + date;
            SecretKeySpec keySpec = new SecretKeySpec(hkey.getBytes(), "HmacSHA1");
            try {
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(keySpec);
                mqttpassword = mqttclientid + "%" + mqttuser;
                byte[] result = mac.doFinal(mqttpassword.getBytes());
                mqttpassword = io.cmmc.netpieclient.microgear.Base64.encode(result);
                setupMqttClient();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void addToHistory(String mainText) {
        Log.d(TAG, "[LOG:] addToHistory: " + mainText);

    }
}
