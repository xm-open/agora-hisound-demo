package io.agora.rte.extension.hisound.example;

import static io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
import static io.agora.rtc2.Constants.CLIENT_ROLE_BROADCASTER;
import static io.agora.rtc2.Constants.POSITION_BEFORE_MIXING;
import static io.agora.rtc2.Constants.POSITION_MIXED;
import static io.agora.rtc2.Constants.POSITION_PLAYBACK;
import static io.agora.rtc2.Constants.POSITION_RECORD;
import static io.agora.rtc2.Constants.RAW_AUDIO_FRAME_OP_MODE_READ_ONLY;
import static io.agora.rtc2.Constants.RENDER_MODE_HIDDEN;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.agora.extension.hisound.ExtensionManager;

import io.agora.rtc2.ChannelMediaOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IMediaExtensionObserver;
import io.agora.rtc2.IAudioFrameObserver;
import io.agora.rtc2.audio.AudioParams;

import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

public class MainActivity extends AppCompatActivity implements IMediaExtensionObserver {

    private static final String TAG = "MainActivity";
    public static final String EXTENSION_NAME = ExtensionManager.EXTENSION_NAME; // Name of target link library used in CMakeLists.txt
    public static final String EXTENSION_VENDOR_NAME = ExtensionManager.EXTENSION_VENDOR_NAME; // Provider name used for registering in agora-bytedance.cpp
    public static final String EXTENSION_AUDIO_FILTER = ExtensionManager.EXTENSION_AUDIO_FILTER; // Audio filter name defined in ExtensionProvider.h
    public static final String EFFECT_WORK_DIR = "work_dir";
    private static final String EFFECT_ASSET = "hisound_effect";

    private RtcEngine mRtcEngine;

    private ListView mListViewEffect;
    private int mEffectIndex = 0;
    private int mPrevEffectIndex= 0;
    private List<EffectModel> mListSoundEffect;
    private AdapterEffect mAdapterEffect;
    private boolean joined;
    private EditText et_channel;
    private Button join;
    private int myUid;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static void hideInputBoard(Activity activity, EditText editText)
    {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListViewEffect = findViewById(R.id.listviewEffect);
        join = findViewById(R.id.btn_join);
        et_channel = findViewById(R.id.et_channel);
        join.setOnClickListener(view -> {
            if (view.getId() == R.id.btn_join) {
                if (!joined) {
                    hideInputBoard(this, et_channel);
                    String channelId = et_channel.getText().toString();
                    if (AndPermission.hasPermissions(this, Permission.Group.STORAGE, Permission.Group.MICROPHONE)) {
                        joinChannel(channelId);
                        return;
                    }
                    AndPermission.with(this).runtime().permission(
                            Permission.Group.STORAGE,
                            Permission.Group.MICROPHONE
                    ).onGranted(permissions ->
                            joinChannel(channelId)).start();
                } else {
                    joined = false;
                    mRtcEngine.leaveChannel();
                    join.setText(getString(R.string.join));
                }
            }
        });
        findViewById(R.id.init_extension).setOnClickListener(view -> initExtension());
        initPermission();
        initSoundEffect();
    }

    private void initPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO},
                    0);
        } else {
            initRtcEngine();
        }
    }

    private EffectModel createEffectModel(File file) {
        if (file.isDirectory()) {
            File effectJson = new File(file, "effect.bin");
            if (effectJson.exists()) {
                Log.i(TAG, "workPath: " + file.getAbsolutePath() +
                        " des: " + file.getName());
                return new EffectModel(file.getAbsolutePath(),file.getName());
            }
        }
        return null;
    }

    private boolean hasAgoraSimpleFilterLib(){
        try {
            Class<?> aClass = Class.forName("io.agora.extension.hisound.ExtensionManager");
            return aClass != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(!hasAgoraSimpleFilterLib()){
            new AlertDialog.Builder(getApplicationContext())
                    .setMessage(R.string.hisound_extension_tip)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        this.onBackPressed();
                    })
                    .show();
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (Arrays.equals(grantResults, new int[]{0, 0})) {
                initRtcEngine();
            }
        }
    }

    private void initSoundEffect() {
        mListSoundEffect = new ArrayList<>();
        mListSoundEffect.add(new EffectModel("", "无音效"));
        Context context = getApplicationContext();
        try {
            ResourceUtils.copyFileOrDir(
                    MainActivity.this.getAssets(),
                    EFFECT_ASSET,
                    getExternalFilesDir(null).getAbsolutePath()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        File soundEffectFolder = Objects.requireNonNull(context).getExternalFilesDir(EFFECT_ASSET);
        Log.i(TAG, "soundEffectFolder: " + soundEffectFolder);
        if (soundEffectFolder.exists() && soundEffectFolder.isDirectory()) {
            File[] listFiles = soundEffectFolder.listFiles();
            Log.i(TAG, "listFiles: " + Arrays.toString(listFiles));
            if (listFiles != null && listFiles.length > 0) {
                for (File file : listFiles) {
                    Log.i(TAG, "effect file: " + file);
                    EffectModel effectModel = createEffectModel(file);
                    if (effectModel != null) {
                        mListSoundEffect.add(effectModel);
                    }
                }
            }
        }

        mAdapterEffect = new AdapterEffect();
        mListViewEffect.setAdapter(mAdapterEffect);
        mListViewEffect.setOnItemClickListener((parent, view, position, id) -> {
            if(!joined){
                Log.i(TAG, "先加入频道");
                return ;
            }

            mEffectIndex = position % mListSoundEffect.size();
            if(mEffectIndex == mPrevEffectIndex){
                Log.i(TAG, "the effects before and after are same");
                return ;
            }

            if(mEffectIndex == 0){
                JSONObject o = new JSONObject();
                setExtensionProperty(ExtensionManager.KEY_STOP_EFFECT, o.toString());
                mPrevEffectIndex = mEffectIndex;
                mAdapterEffect.notifyDataSetChanged();
                Log.i(TAG, "close effect");
                return ;
            }

            mPrevEffectIndex = mEffectIndex;
            mAdapterEffect.notifyDataSetChanged();
            EffectModel effectModel = mListSoundEffect.get(mEffectIndex);
            Log.i(TAG, "select effect: " + effectModel.workPath);

            String jsonValue = null;
            JSONObject o = new JSONObject();
            try {
                o.put(EFFECT_WORK_DIR, effectModel.workPath);
                jsonValue = o.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (jsonValue != null) {
                setExtensionProperty(ExtensionManager.KEY_LOAD_EFFECT, jsonValue);
            }
        });

    }

    private void setExtensionProperty(String key, String property) {
        mRtcEngine.setExtensionProperty(EXTENSION_VENDOR_NAME, EXTENSION_AUDIO_FILTER, key, property);
    }

    private void initExtension(){
        try {
            JSONObject jsonObject = new JSONObject();
            // 传入在声网控制台激活插件后获取的 appKey
            jsonObject.put("appkey", getString(R.string.appKey));
            // 传入在声网控制台激活插件后获取的 appSecret
            jsonObject.put("secret", getString(R.string.appSecret));
            jsonObject.put("init_json",
                    Objects.requireNonNull(getApplicationContext()).getExternalFilesDir(EFFECT_ASSET + "/init.json"));
            jsonObject.put("init_dir",
                    Objects.requireNonNull(getApplicationContext()).getExternalFilesDir(EFFECT_ASSET));
            setExtensionProperty(ExtensionManager.KEY_INIT_FILTER, jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    private void initRtcEngine() {
        RtcEngineConfig config = new RtcEngineConfig();
        config.mContext = getApplicationContext();
        config.mAppId = getString(R.string.agora_app_id);
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING;
        config.addExtension(EXTENSION_NAME);
        config.mExtensionObserver = this;
        config.mEventHandler = iRtcEngineEventHandler;
        config.mAreaCode = RtcEngineConfig.AreaCode.AREA_CODE_CN;
        try {
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
        if (mRtcEngine == null) {
            return;
        }
        mRtcEngine.setParameters("{"
                + "\"rtc.report_app_scenario\":"
                + "{"
                + "\"appScenario\":" + 100 + ","
                + "\"serviceType\":" + 11 + ","
                + "\"appVersion\":\"" + RtcEngine.getSdkVersion() + "\""
                + "}"
                + "}");
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

        // 调用 registerAudioFrameObserver 注册音频观测器，并传入 IAudioFrameObserve 实例。
        mRtcEngine.registerAudioFrameObserver(iAudioFrameObserver);

        // 初始化插件
        enableExtension(true);

        mRtcEngine.enableAudio();
    }

    @Override
    public void onEvent(String provider, String extension, String key, String value) {
        Log.i(TAG, "onEvent vendor: " + provider + "  extension: " + extension + "  key: " + key + "  value: " + value);
    }

    @Override
    public void onStarted(String provider, String extension) {

    }

    @Override
    public void onStopped(String provider, String extension) {

    }

    @Override
    public void onError(String provider, String extension, int error, String message) {

    }

    private void enableExtension(boolean enabled) {
        mRtcEngine.enableExtension(EXTENSION_VENDOR_NAME, EXTENSION_AUDIO_FILTER, enabled);
    }

    private final IAudioFrameObserver iAudioFrameObserver = new IAudioFrameObserver() {

        private boolean initMixedOut = false;
        private boolean initPreMixOut = false;
        private boolean initRecordOut = false;
        private boolean initPlayBack = false;

        private OutputStream mixedOutput;
        private OutputStream preMixOutput;
        private OutputStream recordOutput;
        private OutputStream playBackOutput;

        @Override
        public boolean onRecordAudioFrame(String channel, int audioFrameType, int samples, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer byteBuffer, long renderTimeMs, int bufferLength) {
//            Log.i(TAG, "onRecordAudioFrame");
            if(!initRecordOut){
                try {
                    recordOutput = new FileOutputStream(
                            Environment.getExternalStorageDirectory().getPath() +
                                    "/Android/data/io.agora.rtc.extension.hisound/files/recordOutput-hisound.pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                initRecordOut = true;
            }
            try {
                int length = byteBuffer.remaining();
                byte[] buffer = new byte[length];
                byteBuffer.get(buffer);
                byteBuffer.flip();
                recordOutput.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            boolean isWriteBackAudio = false;
            if(isWriteBackAudio){
                int length = byteBuffer.remaining();
//                byteBuffer.flip();
//                byte[] buffer = readBuffer();
                byte[] origin = new byte[length];
                byteBuffer.get(origin);
                byteBuffer.flip();
//                byteBuffer.put(audioAggregate(origin, buffer), 0, byteBuffer.remaining());
                byteBuffer.put(origin, 0, byteBuffer.remaining());
                byteBuffer.flip();
            }
            return true;
        }


        @Override
        public boolean onPlaybackAudioFrame(String channel, int audioFrameType, int samples, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer byteBuffer, long renderTimeMs, int bufferLength) {
//            Log.i(TAG, "onPlaybackAudioFrame");
            if(!initPlayBack){
                try {
                    playBackOutput = new FileOutputStream(
                            Environment.getExternalStorageDirectory().getPath() +
                                    "/Android/data/io.agora.rtc.extension.hisound/files/playBackOutput-hisound.pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                initPlayBack = true;
            }
            try {
                int length = byteBuffer.remaining();
                byte[] buffer = new byte[length];
                byteBuffer.get(buffer);
                byteBuffer.flip();
                playBackOutput.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean onMixedAudioFrame(String channel, int audioFrameType, int samples, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer byteBuffer, long renderTimeMs, int bufferLength) {
//            Log.i(TAG, "onMixedAudioFrame");

            if(!initMixedOut){
                try {
                    mixedOutput = new FileOutputStream(
                            Environment.getExternalStorageDirectory().getPath() +
                                    "/Android/data/io.agora.rtc.extension.hisound/files/mixedOut-hisound.pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                initMixedOut = true;
            }
            try {
                int length = byteBuffer.remaining();
                byte[] buffer = new byte[length];
                byteBuffer.get(buffer);
                byteBuffer.flip();
                mixedOutput.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public boolean onEarMonitoringAudioFrame(int type, int samplesPerChannel, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer buffer, long renderTimeMs, int avsync_type) {
            return false;
        }

        @Override
        public boolean onPlaybackAudioFrameBeforeMixing(String channel, int uid, int audioFrameType, int samples, int bytesPerSample, int channels, int samplesPerSec, ByteBuffer byteBuffer, long renderTimeMs, int bufferLength) {
//            Log.i(TAG, "onPlaybackAudioFrameBeforeMixing");
            if(!initPreMixOut){
                try {
                    preMixOutput = new FileOutputStream(
                            Environment.getExternalStorageDirectory().getPath()
                                    + "/Android/data/io.agora.rtc.extension.hisound/files/preMixOut-hisound.pcm");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                initPreMixOut = true;
            }
            try {
                int length = byteBuffer.remaining();
                byte[] buffer = new byte[length];
                byteBuffer.get(buffer);
                byteBuffer.flip();
                preMixOutput.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        public int getObservedAudioFramePosition() {
            return POSITION_PLAYBACK | POSITION_RECORD |
                    POSITION_MIXED | POSITION_BEFORE_MIXING;
        }

        @Override
        public AudioParams getRecordAudioParams() {
            return new AudioParams(44100, 1, RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, 1024);
        }

        @Override
        public AudioParams getPlaybackAudioParams() {
            return new AudioParams(44100, 1, RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, 1024);
        }

        @Override
        public AudioParams getMixedAudioParams() {
            return new AudioParams(44100, 1, RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, 1024);
        }

        @Override
        public AudioParams getEarMonitoringAudioParams() {
            return new AudioParams(44100, 1, RAW_AUDIO_FRAME_OP_MODE_READ_ONLY, 1024);
        }

    };

    private final IRtcEngineEventHandler iRtcEngineEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onError(int err) {
            Log.w(TAG, String.format("onError code %d message %s", err, RtcEngine.getErrorDescription(err)));
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
            super.onLeaveChannel(stats);
            Log.i(TAG, String.format("local user %d leaveChannel!", myUid));
            if(mRtcEngine == null){
                Log.e(TAG, "engine == null");
            }
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.i(TAG, String.format("onJoinChannelSuccess channel %s uid %d", channel, uid));
            myUid = uid;
            joined = true;
            handler.post(() -> {
                join.setEnabled(true);
                join.setText(getString(R.string.leave));
            });
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.i(TAG, "onUserJoined->" + uid);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.i(TAG, String.format("user %d offline! reason:%d", uid, reason));
        }

        @Override
        public void onActiveSpeaker(int uid) {
            super.onActiveSpeaker(uid);
            Log.i(TAG, String.format("onActiveSpeaker:%d", uid));
        }
    };

    private void joinChannel(String channelId) {
        mRtcEngine.setClientRole(CLIENT_ROLE_BROADCASTER);
        TokenUtils.gen(getApplicationContext(), channelId, 0, accessToken -> {
            ChannelMediaOptions option = new ChannelMediaOptions();
            option.autoSubscribeAudio = true;
            Log.i("TAG", "accessToken: " + accessToken);
            int res = mRtcEngine.joinChannel(accessToken, channelId, 0, option);
            if (res != 0) {
                Toast.makeText(getApplicationContext(), RtcEngine.getErrorDescription(Math.abs(res)),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, RtcEngine.getErrorDescription(Math.abs(res)));
                return;
            }
            join.setEnabled(false);
        });
    }

    public class AdapterEffect extends BaseAdapter {

        @Override
        public int getCount() {
            return mListSoundEffect.size();
        }

        @Override
        public EffectModel getItem(int position) {
            return mListSoundEffect.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = View.inflate(getApplicationContext(),
                        R.layout.item_effect_layout, null);
            }

            ((TextView) convertView.findViewById(R.id.des_effect))
                    .setText(mListSoundEffect.get(position).des);
            ((TextView) convertView.findViewById(R.id.url_effect))
                    .setText(mListSoundEffect.get(position).workPath);

            if (position == mEffectIndex % mListSoundEffect.size()) {
                convertView.setBackgroundColor(Color.GRAY);
            } else {
                convertView.setBackgroundColor(Color.BLUE);
            }
            return convertView;
        }
    }

    static class EffectModel {
        public String workPath;
        public String des;

        public EffectModel(String workPath, String des) {
            this.workPath = workPath;
            this.des = des;
        }
    }
}