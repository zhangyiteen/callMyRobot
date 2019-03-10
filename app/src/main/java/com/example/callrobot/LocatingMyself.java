package com.example.callrobot;

import android.Manifest;
import android.content.pm.PackageManager;
import android.animation.ObjectAnimator;
import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.view.View;
import android.content.Context;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;


// for gimbal beacon with eddystone configuration
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;
import java.util.HashMap;
//import java.util.Set;
//import java.util.Iterator;
//import java.util.Map;

// for restful client
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;

import java.util.HashMap;
import java.util.LinkedList;

import java.util.UUID;


public class LocatingMyself extends AppCompatActivity implements BeaconConsumer, RangeNotifier {

    public static final String BASE_URL = "http://192.168.1.101:8000/v1/";

    ImageView imageHuman;
    ImageView imageRobot;
    ObjectAnimator objectAnimator;

    HashMap<String, PointF> virtualMap = new HashMap<>();

    private ImageButton callRobotButton;

    // debug log tag
    private static final String TAG = "BeaconFind";
    private static final String DEBUG_TAG = "Debug";
    private static final String INRENT_TAG = "Intent";
    private static final String EXCEPTION_TAG = "Exception";
    private static final String ERROR_TAG = "Error";
    private static final String VIBRATION_TAG = "Vibration";

    // permission request code
    private final static int MY_LOCATION_PERMISSION_REQUEST_CODE = 101;

    // beacon scanner
    private BeaconManager beaconManager;

    // restful client
    private static String uniqueID = UUID.randomUUID().toString();
    private String sessionTicket;
    private String robotID;
    private RequestQueue myQueue;

    public Boolean isRobotCalled;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locating_myself);
        this.isRobotCalled = getIntent().getExtras().getBoolean("isRobotCalled");

        initBeaconStationLocation();
        //toolbar customization
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Call Robot");

        /**get the intent*/

        Log.d(INRENT_TAG,"isRobotCalled value is: " + isRobotCalled.toString());

//        Intent intent1 = getIntent();
//        Boolean isRobotCalled1 = intent.getBooleanExtra("isRobotCalled", false);
//        Log.d(INRENT_TAG,"isRobotCalled value is: " + isRobotCalled1);

        // create a listener for open call robot activity
        callRobotButton =(ImageButton)findViewById(R.id.call_robot_button);
        callRobotButton.setPadding(600,0, 0, 0);
        callRobotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callRobot();
            }
        });

        // create the human icon
        imageHuman=(ImageView)findViewById(R.id.human);
        imageRobot = (ImageView)findViewById(R.id.callRobotRobot);
        initialLocation(imageRobot,450, 1300);

        if(isRobotCalled == false){
            imageRobot.setVisibility(View.INVISIBLE);
        } else {
            imageRobot.setVisibility(View.VISIBLE);
        }

        // initial user current location, triggered every time this activity load.
        float initialHumanHorizontalTarget = 660;
        float initialHumanVerticalTarget = 600;
        initialLocation(imageHuman,initialHumanHorizontalTarget, initialHumanVerticalTarget);


        // beacon scanning and showing
        Log.d(DEBUG_TAG, "starting beacon manager");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            Log.d(DEBUG_TAG,"request fine location permission");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, },
                    MY_LOCATION_PERMISSION_REQUEST_CODE);
        } else { // has permission
            Log.d(DEBUG_TAG,"has course location permission");
        }
        beaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager.bind(this);

        myQueue = Volley.newRequestQueue(this);
    }

    // ask for the location access permission from android system
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantRestuls){
//        super.onRequestPermissionsResult(requestCode, permissions, grantRestuls);
        switch (requestCode) {
            case MY_LOCATION_PERMISSION_REQUEST_CODE:
                if(grantRestuls.length > 0
                        && grantRestuls[0] == PackageManager.PERMISSION_GRANTED){

                    Log.d(DEBUG_TAG,"coarse location permission granted");

                } else {
                    Log.d(DEBUG_TAG,"request permission fine location denied");
                }
                return;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantRestuls);
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        Identifier gimbalNameSpaceId = Identifier.parse("0x36e11849835b06ec69b4");
        Region region = new Region("tud-region", gimbalNameSpaceId, null, null);
        beaconManager.addRangeNotifier(this);
        try {
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // beacon station signal listener
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.d(TAG, "number:" + beacons.size());
//        for(Beacon b: beacons){
//            Log.d(TAG, "" + b.getId2() + " rssi:" + b.getRssi() + " d:" + b.getDistance());
//        }

        if(beacons.size() <= 0){ // no beacon found
            return;
        }
        LinkedList<Beacon> sortedbeacons = new LinkedList<>(beacons);
        Collections.sort(sortedbeacons, new Comparator<Beacon>() {
            public int compare(Beacon a, Beacon b){
                return Double.compare(a.getDistance(), b.getDistance());
            }
        });

        LinkedList<Beacon> closest = new LinkedList<>();
        for(int i = 0; i < 3; i++){
            if(i < sortedbeacons.size()){
                closest.add(sortedbeacons.get(i));
            } else {
                break;
            }
        }

        for(Beacon b: closest){
            Log.d(TAG, "" + b.getId2() + " rssi:" + b.getRssi() + " d:" + b.getDistance());

        }

        // choose the closest 3 beacons.
        try {
            processBeacons(closest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // collect the beacon info from the nearest 3 beacon station together
    private void processBeacons(LinkedList<Beacon> beacons) throws JSONException{
        JSONArray locations = new JSONArray();
        for(Beacon b: beacons) {
            JSONObject beacon = new JSONObject();
            try {
                beacon.put("bid", b.getId2().toHexString());
                beacon.put("rssi", b.getRssi());
                beacon.put("dis", b.getDistance());
            } catch (JSONException e) {
                System.out.println(e);
            }
            locations.put(beacon);
        }
        Log.d(TAG, locations.toString());

        //send the user currently location to the server,
        // here do not need to send it to server
        updateLocation(locations);

        //update user current location on the map
        updateUserLocation(locations);
    }


    public void updateLocation(JSONArray locations){
//       "{'uid':'1246', 'loc':[{'a':5, 'b':3, 'c':-30}]}"
//       "{'uid':'1246', 'loc':[{'a':5, 'b':3, 'c':-30}], 'ticket':1234, 'robot':xxxx}"
        JSONObject payload = new JSONObject();

        try {
            payload.put("uid", uniqueID);
            payload.put("loc", locations);
            if(sessionTicket != null && robotID != null){
                payload.put("ticket", sessionTicket);
                payload.put("robot", robotID);
            }
        }  catch (JSONException e){
            Log.i(EXCEPTION_TAG, e.toString());
        }

        String url = BASE_URL + "proximity/";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, payload, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(DEBUG_TAG, "response?"+response);

                        // when the robot arrive the person
                        if(response.has("service")){
                            service_done();
                            return;
                        }

                        if(response.has("ticket") && response.has("robot")){
                            // check if it is my session
                            try {
                                if(response.getString("ticket").equals(sessionTicket)){
                                    // updating robot location
                                    if(response.has("loc")){
                                        JSONArray robotLoc = response.getJSONArray("loc");
                                        updateRobotLocation(response.getString("robot"), robotLoc);
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        Log.d(TAG,"network post error");
                    }
                });
        myQueue.add(jsonObjectRequest);
    }

    public void updateUserLocation(JSONArray locs) throws JSONException {

        Log.d(DEBUG_TAG, "update user loc:"  + locs.toString());
        if(locs.length() >= 2){
            //get the nearest distance
            JSONObject beaconLocA = null;
            try {
                beaconLocA = locs.getJSONObject(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            double distanceToA = Double.valueOf(beaconLocA.optString("dis"));
            String beaconIdA = beaconLocA.optString("bid");
            PointF beaconCoordinateA = null;
            PointF beaconCoordinateB = null;

            //load the nearest beacon station coordinate from virtual map(beaconCoordinateA)
            for(String i : virtualMap.keySet()){
                if(i.equals(beaconIdA)){
                    Log.d(TAG, "nearest beacon station:" + i);
                    beaconCoordinateA = virtualMap.get(i);
                    //Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateA.x + beaconCoordinateA.y);
                }
            }

            // get the 2nd nearest distance
            JSONObject beaconLocB = null;
            try {
                beaconLocB = locs.getJSONObject(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //double distanceToB =  Double.valueOf(beaconLocB.optString("dis"));
            String beaconIdB = beaconLocB.optString("bid").toString();

            //load the second beacon station coordinate(beaconCoordinateB)
            for(String i : virtualMap.keySet()){
                if(i.equals(beaconIdB)){
                    Log.d(TAG, "2nd beacon station:" + i);
                    beaconCoordinateB = virtualMap.get(i);
                    //Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateB.x + beaconCoordinateB.y);
                }
            }

            //Calculate the distance between nearest beacon station and 2nd nearest beacon station
            //on the virtualMap
            //double distanceAB = distanceCalculation(beaconCoordinateA, beaconCoordinateB);
            //Log.d(TAG, "The dista" + beaconCoordinateB.x + beaconCoordinateB.y);
            //distanceAX = (distanceToA * distanceToA - distanceToB * distanceToB + distanceAB * distanceAB) / (2 * distanceAB);


            double userCurrentVertical = 0;
            double userCurrentHorizontal = 0;

            boolean isHorizontal = false;
            boolean isVertical = true;

            for (int i = 0; i < 2; i++) {
                JSONObject beaconObject = locs.getJSONObject(i);
                if(beaconObject.optString("bid").equals("x000000000008")){
                    isHorizontal = false;
                    isVertical = true;
                }
                if(beaconObject.optString("bid").equals("0x000000000001")){
                    isHorizontal = true;
                    isVertical = false;
                }
            }

            //calculate the horizontal movement distance
            if( isHorizontal == true){
                if(distanceToA < 1.2){
                    userCurrentHorizontal = beaconCoordinateA.x;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    userCurrentHorizontal = (beaconCoordinateA.x + beaconCoordinateB.x)/2;
                }
                showCurrentLocation(isHorizontal, (float) userCurrentHorizontal, isVertical, 0);

            }
            if(isVertical == true) {
                if(distanceToA < 1.2){
                    userCurrentVertical = beaconCoordinateA.y;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    userCurrentVertical = (beaconCoordinateA.y + beaconCoordinateB.y)/2;
                }
                showCurrentLocation(isHorizontal, 0, isVertical, (float)userCurrentVertical);
            }

        }
    }

    /** once the call robot button clicked,
     * send a message to the server to call a robot
     * send_param: id uuid of the mobile device
     * response: robot name, current location?
     * */
    // open the call robot activity
    public void callRobot(){

         //show and initialise the robot location
        this.isRobotCalled = true;
        imageRobot.setVisibility(View.VISIBLE);
        initialLocation(imageRobot,450, 1300);

        String url = BASE_URL + "assist/?uid=" + uniqueID;
        Log.d(DEBUG_TAG, "call clicked: " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(DEBUG_TAG, "" + response);
                        // response {"ticket":session_ticket, "robot":robot_id}
                        // if no robot available response {}
                        try {
                            if (response.has("robot") && response.has("ticket")) {
                                sessionTicket = response.getString("ticket");
                                robotID = response.getString("robot");
                                Log.d(DEBUG_TAG, "ticket:" + sessionTicket + " robot:" + robotID);
                                if (response.has("loc") && response.getJSONArray("loc").length() >= 2) {
                                    updateRobotLocation(robotID, response.getJSONArray("loc"));
                                }
                                //the first time receive the ticket
                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                Log.i(VIBRATION_TAG, "No vibrator service ticket is received. ");
                                if (v.hasVibrator()) {
                                    Log.i(VIBRATION_TAG, "has vibrator service ticket is received. ");
                                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(500,200));
                                }
                            } else {
                                // TODO: emit message: no robot, please try again
                            }
                        } catch (JSONException e) {
                            System.out.println(e);
                        }}
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO: Handle error
                            Log.d(TAG,"network get error");
                        }
                    });

        myQueue.add(jsonObjectRequest);
    }

    public void updateRobotLocation(String robot, JSONArray RobotLocs) throws JSONException {
        // updating robot location on map
        Log.d(DEBUG_TAG, "update robot loc:" + robot + " loc:" + RobotLocs.toString());
        //if(RobotLocs.length() >= 2){
            //get the nearest distance
            JSONObject beaconLocA = null;
            try {
                beaconLocA = RobotLocs.getJSONObject(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // The distance to the nearest beacon station
            double distanceToA = Double.valueOf(beaconLocA.optString("dis"));

            String beaconIdA = beaconLocA.optString("bid");
            PointF beaconCoordinateA = null;
            PointF beaconCoordinateB = null;

            //load the nearest beacon station coordinate from virtual map(beaconCoordinateA)
            for(String i : virtualMap.keySet()){
                if(i.equals(beaconIdA)){
                    Log.d(TAG, "nearest beacon station:" + i);
                    beaconCoordinateA = virtualMap.get(i);
                    //Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateA.x + beaconCoordinateA.y);
                }
            }
            if(beaconCoordinateA == null){
                return;
            }

            // get the 2nd nearest distance
            JSONObject beaconLocB = null;
            try {
                beaconLocB = RobotLocs.getJSONObject(1);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            String beaconIdB = beaconLocB.optString("bid").toString();

            //load the second beacon station coordinate(beaconCoordinateB)
            for(String i : virtualMap.keySet()){
                if(i.equals(beaconIdB)){
                    Log.d(TAG, "2nd beacon station:" + i);
                    beaconCoordinateB = virtualMap.get(i);
                    //Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateB.x + beaconCoordinateB.y);
                }
            }
            if(beaconCoordinateB == null){
                return;
            }
            //Log.d(TAG, "The dista" + beaconCoordinateB.x + beaconCoordinateB.y);

            double robotCurrentVertical = 0;
            double robotCurrentHorizontal = 0;

            boolean isRobotHorizontal = true;
            boolean isRobotVertical = false;

            for (int i = 0; i < 2; i++) {
                JSONObject beaconObject = RobotLocs.getJSONObject(i);
                if(beaconObject.optString("bid").equals("0x000000000008")){
                    isRobotHorizontal = false;
                    isRobotVertical = true;
                }
                if(beaconObject.optString("bid").equals("0x000000000001")){
                    isRobotHorizontal = true;
                    isRobotVertical = false;
                }
            }

            //calculate the horizontal movement distance
            if( isRobotHorizontal == true){
                if(distanceToA < 1.2){
                    robotCurrentHorizontal = beaconCoordinateA.x;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    robotCurrentHorizontal = (beaconCoordinateA.x + beaconCoordinateB.x)/2;
                }
                showRobotCurrentLocation(isRobotHorizontal, (float) robotCurrentHorizontal, isRobotVertical, 0);

            }

            //calculate the vertical movement distance
            if(isRobotVertical == true) {
                if(distanceToA < 1.2){
                    robotCurrentVertical = beaconCoordinateA.y;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    robotCurrentVertical = (beaconCoordinateA.y + beaconCoordinateB.y)/2;
                }
                showRobotCurrentLocation(isRobotHorizontal, 0, isRobotVertical, (float)robotCurrentVertical);
            }
        //}
    }


    // initial ImageView Icon current location, should triggered every time this activity load.
    public void initialLocation(ImageView imageTarget, float horizontalTarget, float verticalTarget){
        objectAnimator= ObjectAnimator.ofFloat(imageTarget,"x",horizontalTarget);
        objectAnimator.setDuration(0);
        objectAnimator.start();

        objectAnimator= ObjectAnimator.ofFloat(imageTarget,"y", verticalTarget);
        objectAnimator.setDuration(0);
        objectAnimator.start();
    }


    // update the user location in real-time
    public void showCurrentLocation(boolean isHorizontal, float horizontalTarget,
                                    boolean isVertical, float verticalTarget){
        if(isHorizontal == true){
            objectAnimator= ObjectAnimator.ofFloat(imageHuman,"x",horizontalTarget);
            objectAnimator.setDuration(0);
            objectAnimator.start();
        }

        if(isVertical == true){
            objectAnimator= ObjectAnimator.ofFloat(imageHuman,"y", verticalTarget);
            objectAnimator.setDuration(0);
            objectAnimator.start();
        }
    }


    // update the robot location in real-time
    public void showRobotCurrentLocation(boolean isHorizontal, float horizontalTarget,
                                         boolean isVertical, float verticalTarget){
        if(isHorizontal == true) {
            objectAnimator = ObjectAnimator.ofFloat(imageRobot, "x", horizontalTarget);
            objectAnimator.setDuration(1000);
            objectAnimator.start();
        }
        if(isVertical == true) {
            objectAnimator= ObjectAnimator.ofFloat(imageRobot,"y", verticalTarget);
            objectAnimator.setDuration(1000);
            objectAnimator.start();
        }
    }

//beacon station location initialization
    public HashMap<String,PointF> initBeaconStationLocation() {

        // HashMap<String, PointF> virtualMap = new HashMap<>();
        this.virtualMap.put("0x000000000060", new PointF(660,640));
        this.virtualMap.put("0x000000000022", new PointF(660, 780));
        this.virtualMap.put("0x000000000011", new PointF(660,920));
        this.virtualMap.put("0x000000000008", new PointF(660, 1060));
        this.virtualMap.put("0x000000000006", new PointF(660,1200));
        //this.virtualMap.put("0x000000000007", new PointF(640, 1000));
        //this.virtualMap.put("0x000000000006", new PointF(660,1300));
        this.virtualMap.put("0x000000000005", new PointF(660, 1300));
        this.virtualMap.put("0x000000000001", new PointF(450,1300));
        return (this.virtualMap);
    }

    // the robot arrive my place
    public void service_done(){
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Log.i(VIBRATION_TAG, "No vibrator provided by the mobile device.");
        if (v.hasVibrator()) {
            Log.i(VIBRATION_TAG, "The mobile device has a vibrator ");
            long[] mVibratePattern = new long[]{0, 500, 200, 500};

            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createWaveform(mVibratePattern, -1));
        }
    }


}
