package com.yang.floatvoiceplayer;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.yang.floatvoiceplayer.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class PlayActivity extends AppCompatActivity {

    public static final String URI_RESOURCE = "uri_resource";

    public static void start(Context context, Uri uri) {
        Intent intent = new Intent(context, PlayActivity.class);
        intent.putExtra(URI_RESOURCE, uri.toString());
        context.startActivity(intent);
    }

    public static final String BROADCAST_INTENT_FILTER = "I am learning english";
    public static final String ACTION = "action";
    public static final String ACTION_PLAY = "play";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_BACK = "back";
    public static final String ACTION_FORWARD = "forward";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_SPEED = "speed";
    public static final String ACTION_INIT = "init";

    private static final float SPEED_ZERO_FIVE = 0.5f;
    private static final float SPEED_ZERO_SEVEN_FIVE = 0.75f;
    private static final float SPEED_ONE = 1f;
    private static final float SPEED_ONE_TWO_FIVE = 1.25f;
    private static final float SPEED_ONE_FIVE = 1.5f;

    private TextView submitButton;
    private EditText input;

    private ExoPlayer player;
    private FloatingService floatingService;
    private Receiver receiver;

    private Uri uri;
    private int seekSeconds = 10;
    private ArrayList<Float> speedList;
    private int speedIndex;
    private Timer timer;
    private boolean isBindingService;
    private boolean isRegisterReceiver;


    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        if(getIntent().getExtras() != null) {
            uri = Uri.parse(getIntent().getStringExtra(URI_RESOURCE));
        }
        initPlay();
        initView();
    }

    private void initPlay() {
        speedList = new ArrayList<>();
        speedList.add(SPEED_ZERO_FIVE);
        speedList.add(SPEED_ZERO_SEVEN_FIVE);
        speedList.add(SPEED_ONE);
        speedList.add(SPEED_ONE_TWO_FIVE);
        speedList.add(SPEED_ONE_FIVE);
        player = new ExoPlayer.Builder(this).build();
        if(uri != null) {
            MediaItem mediaItem =  MediaItem.fromUri(uri);
            player.setMediaItem(mediaItem);
            player.addListener(playListener);
            player.prepare();
        }
        registerBroadCaseReceiver();
        startFloat();
    }

    private void initView() {
        submitButton = findViewById(R.id.button_set_parameter);
        input = findViewById(R.id.input);
        submitButton.setOnClickListener(view -> {
            if(TextUtils.isEmpty(input.getText().toString())) return;
            seekSeconds = Integer.parseInt(input.getText().toString());
        });
    }
    
    private Player.Listener playListener = new Player.Listener() {

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if(playbackState == Player.STATE_ENDED) {
                initPlayProgress();
            }
        }
    };

    private void startFloat() {
        if(Utils.checkFloatPermission(this)) {
            Intent intent = new Intent(this, FloatingService.class);
            isBindingService = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 100);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100) {
            if(Utils.checkFloatPermission(this)){
                Intent intent = new Intent(this, FloatingService.class);
                isBindingService = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            } else {
                Toast.makeText(PlayActivity.this, "没有授权", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FloatingService.MyBinder binder = (FloatingService.MyBinder) service;
            floatingService = binder.getService();
            floatingService.initData(Utils.getFileName(uri, PlayActivity.this), ((int)(player.getContentDuration() / 1000)));
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindingService = false;
        }
    };

    private void registerBroadCaseReceiver() {
        receiver = new Receiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BROADCAST_INTENT_FILTER);
        registerReceiver(receiver, intentFilter);
        isRegisterReceiver = true;
    }

    private void unregisterReceiver() {
        if(isRegisterReceiver) {
            unregisterReceiver(receiver);
            isRegisterReceiver = false;
        }
    }

    private class Receiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra(ACTION);
            if(!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_BACK:
                        back();
                        break;
                    case ACTION_PAUSE:
                        pause();
                        break;
                    case ACTION_PLAY:
                        play();
                        break;
                    case ACTION_FORWARD:
                        forward();
                        break;
                    case ACTION_STOP:
                        stop(true);
                        break;
                    case ACTION_SPEED:
                        speed();
                        break;
                    case ACTION_INIT:
                        initPlayProgress();
                        break;
                    default:
                        break;
                }
            }
        }
    }


    public void play() {
        if(player != null) {
            player.play();
            startTimer();
        }
    }

    public void pause() {
        if(player != null) {
            player.pause();
            stopTimer();
        }
    }

    public void back() {
        if(player != null) {
            player.seekTo(player.getCurrentPosition() - seekSeconds * 1000);
        }
    }

    public void forward() {
        if(player != null) {
            player.seekTo(player.getCurrentPosition() + seekSeconds * 1000);
        }
    }

    public void speed() {
        if(player != null) {
            player.setPlaybackParameters(new PlaybackParameters(speedList.get(speedIndex)));
            if(floatingService != null) {
                floatingService.updateSpeed(String.valueOf(speedList.get(speedIndex)));
            }
            speedIndex += 1;
            if(speedIndex > 4) {
                speedIndex = 0;
            }
        }
    }

    private void stop(boolean action) {
        if(isBindingService) {
            unbindService(serviceConnection);
            isBindingService = false;
        }
        if(player != null) {
            player.stop();
            player.release();
            player = null;
        }
        unregisterReceiver();
        if(action) {
            finish();
        }
    }

    private void initPlayProgress() {
        if(player != null) {
            pause();
            floatingService.initPlay();
            player.seekTo(0);

        }
    }

    private void startTimer() {
        if(timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(floatingService != null && player != null) {
                                floatingService.updateProgress((int)(player.getContentPosition() / 1000));
                            }
                        }
                    });
                }
            }, 1000, 1000);
        }
    }

    private void stopTimer(){
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       stop(false);
    }
}
