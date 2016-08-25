package io.cmmc.netpieclient;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

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

    private void brokerconnect(String appid, String key, String secret) {
        File fi = new File(tempFile.toString());
        BufferedReader br;
        StringBuilder sb = new StringBuilder();
        String line;
        String mqttuser, secrettoken, mqttclientid, secretid, mqttpassword, hkey, ckappkey;
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
                Log.d(TAG, "clientId: " + mqttclientid);
                Log.d(TAG, "mqttUser: " + mqttuser);
                Log.d(TAG, "mqttPassword: " + mqttpassword);
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


}
