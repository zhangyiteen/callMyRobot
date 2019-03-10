package com.example.callrobot;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.media.MediaPlayer;
import android.widget.ImageButton;
import android.widget.VideoView;
import android.net.Uri;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // Create a VideoView variable, a MediaPlayer variable, and an int to hold the current
    // video position.
    private VideoView videoBG;
    private MediaPlayer mMediaPlayer;
    private int mCurrentVideoPosition;
    private static final String TAG = "RobotCalled";

    public Boolean isRobotCalled;


    //create a hashMap to storage the virtualMap info
//    HashMap<String, PointF> virtualMap = new HashMap<>();

    private ImageButton myCurrentLocationButton;
    private ImageButton callRobotButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // create a listener for open location myself activity
        myCurrentLocationButton = (ImageButton) findViewById(R.id.fab);
        myCurrentLocationButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                locateMyself();
            }
        });

        // Hook up the VideoView to our UI.
        videoBG = (VideoView) findViewById(R.id.videoView);
        // create a listener for open call robot activity
//        callRobotButton =(ImageButton) findViewById(R.id.fab1);
//        callRobotButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                callRobot();
//            }
//        });

        // Build your video Uri
        Uri uri = Uri.parse("android.resource://" // First start with this,
                + getPackageName() // then retrieve your package name,
                + "/" // add a slash,
                + R.raw.my_robot); // and then finally add your video resource. Make sure it is stored
        // in the raw folder.

        // Set the new Uri to our VideoView
        videoBG.setVideoURI(uri);
        // Start the VideoView
        videoBG.start();

        // Set an OnPreparedListener for our VideoView. For more information about VideoViews,
        // check out the Android Docs: https://developer.android.com/reference/android/widget/VideoView.html
        videoBG.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mMediaPlayer = mediaPlayer;
                // We want our video to play over and over so we set looping to true.
                mMediaPlayer.setLooping(true);
                // We then seek to the current position if it has been set and play the video.
                if (mCurrentVideoPosition != 0) {
                    mMediaPlayer.seekTo(mCurrentVideoPosition);
                    mMediaPlayer.start();
                }
            }
        });


        //initial the beacon station location information on the virtual map
//        this.virtualMap = initBeaconStationLocation();
//        Intent intentToLocatingMyself = new Intent(MainActivity.this, LocatingMyself.class);
//        intentToLocatingMyself.putExtra("virtualMap", this.virtualMap);
//        startActivity(intentToLocatingMyself);
//
//        Intent intentToCallRobot = new Intent(MainActivity.this, CallRobot.class);
//        intentToCallRobot.putExtra("virtualMap", this.virtualMap);
//        startActivity(intentToCallRobot);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Capture the current video position and pause the video.
        mCurrentVideoPosition = mMediaPlayer.getCurrentPosition();
        videoBG.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart the video when resuming the Activity
        videoBG.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // When the Activity is destroyed, release our MediaPlayer and set it to null.
        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    // open the people location activity
    public void locateMyself(){
        isRobotCalled = false;
        //Intent i = new Intent(MainActivity.this, LocatingMyself.class);

        //startActivity(i);
        Log.d(TAG, "the isRobotCalled is set to:" + isRobotCalled.toString());
        Intent intent = new Intent(this, LocatingMyself.class);
        intent.putExtra("isRobotCalled", isRobotCalled);
        startActivity(intent);
    }

    // open the call robot activity
    public void callRobot(){
        isRobotCalled = true;
        Log.d(TAG, "the isRobotCalled is set to:" + isRobotCalled.toString());
        Intent intent = new Intent(this, LocatingMyself.class);
        intent.putExtra("isRobotCalled", isRobotCalled);
        startActivity(intent);
    }


}
