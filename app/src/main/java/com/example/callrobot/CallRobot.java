package com.example.callrobot;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.RangeNotifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class CallRobot extends AppCompatActivity implements BeaconConsumer, RangeNotifier{

    ImageView imageHuman;
    ImageView imageRobot;
    ObjectAnimator objectAnimator;

    HashMap<String, PointF> virtualMap = new HashMap<>();

    // debug log tag
    private static final String TAG = "BeaconFind";
    private static final String DEBUG_TAG = "Debug";
    private static final String EXCEPTION_TAG = "Exception";
    private static final String ERROR_TAG = "Error";

    // permission request code
    private final static int MY_LOCATION_PERMISSION_REQUEST_CODE = 101;

    // beacon scanner
    private BeaconManager beaconManager;

    // restful client
    private static String uniqueID = UUID.randomUUID().toString();
    private String sessionTicket;
    private String robotID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_robot);

        initBeaconStationLocation();

        //toolbar customization
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_bar_call_robot);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Call Robot");

        imageHuman=(ImageView)findViewById(R.id.callRobotHuman);

        imageRobot=(ImageView)findViewById(R.id.callRobotRobot);


        float initialHumanHorizontalTarget = 640;
        float initialHumanVerticalTarget = 500;

        float initialRobotHorizontalTarget = 600;
        float initialRobotVerticalTarget = 1230;

        callRobot();
        // initial user current location, triggered every time this activity load.
        initialLocation(imageHuman, initialHumanHorizontalTarget, initialHumanVerticalTarget);

        // initial robot current location, triggered every time this activity load.
        initialLocation(imageRobot, initialRobotHorizontalTarget, initialRobotVerticalTarget);

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
    }



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


    private void processBeacons(LinkedList<Beacon> beacons) throws JSONException {
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

        //send the user currently location to the server
        updateLocation(locations);

        //update user current location on the map
        updateUserLocation(locations);
    }

    @Override
    public void onPause() {
        super.onPause();
        beaconManager.unbind(this);
    }

    /** once the call robot button clicked,
     * send a message to the server to call a robot
     * send_param: id uuid of the mobile device
     * response: robot name, current location?
     * */
    public void callRobot() {
        RequestParams params = new RequestParams();
        params.put("uid", uniqueID);
        Log.d(TAG, "callRobot request is sent" );
        RestClient.get("assist/", params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // response {"ticket":session_ticket, "robot":robot_id}
                // if no robot available response {}
                try {
                    if(response.has("robot") && response.has("ticket")) {
                        sessionTicket = response.getString("ticket");
                        robotID = response.getString("robot");
                        Log.d(DEBUG_TAG, "ticket:" + sessionTicket + " robot:" + robotID);
                        if(response.has("loc")){
                            updateRobotLocation(robotID, response.getJSONArray("loc"));
                        }
                    } else {
                        // TODO: emit message: no robot, please try again
                    }
                } catch (JSONException e){
                    System.out.println(e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable){
                System.out.println(responseString);
            }
        });
    }

    public void updateLocation(JSONArray locations){
//       "{'uid':'1246', 'loc':[{'a':5, 'b':3, 'c':-30}]}"
//       "{'uid':'1246', 'loc':[{'a':5, 'b':3, 'c':-30}], 'ticket':1234, 'robot':xxxx}"
        JSONObject payload = new JSONObject();

        StringEntity entity = null;
        try {
            payload.put("uid", uniqueID);
            payload.put("loc", locations);
            if(sessionTicket != null && robotID != null){
                payload.put("ticket", sessionTicket);
                payload.put("robot", robotID);
            }
            entity = new StringEntity(payload.toString());
        } catch (UnsupportedEncodingException e){
            Log.i(EXCEPTION_TAG, e.toString());
        } catch (JSONException e){
            Log.i(EXCEPTION_TAG, e.toString());
        }

        RestClient.post(this, "proximity/", entity, "application/json", new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response){
                try {
                    if(response.has("ticket") && response.has("robot")){
                        // check if it is my session
                        if(response.getString("ticket").equals(sessionTicket)){
                            // updating robot location
                            if(response.has("loc")){
                                JSONArray robotLoc = response.getJSONArray("loc");
                                updateRobotLocation(response.getString("robot"), robotLoc);
                            }
                        }
                    }
                } catch (JSONException e) {
                    System.out.println(e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable){
                System.out.println(responseString);
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject response){
                Log.e(ERROR_TAG, "update location failed, no network" + statusCode);
            }

        });
    }

    public void updateRobotLocation(String robot, JSONArray RobotLocs) throws JSONException {
        // updating robot location on map
        Log.d(DEBUG_TAG, "update robot loc:" + robot + " loc:" + RobotLocs.toString());
        if(RobotLocs.length() >= 2){
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
                    Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateA.x + beaconCoordinateA.y);
                }
            }

            // get the 2nd nearest distance
            JSONObject beaconLocB = null;
            try {
                beaconLocB = RobotLocs.getJSONObject(1);
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
                    Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateB.x + beaconCoordinateB.y);
                }
            }

            Log.d(TAG, "The dista" + beaconCoordinateB.x + beaconCoordinateB.y);//distanceAX = (distanceToA * distanceToA - distanceToB * distanceToB + distanceAB * distanceAB) / (2 * distanceAB);

            double robotCurrentVertical = 0;
            double robotCurrentHorizontal = 0;

            boolean isHorizontal = false;
            boolean isVertical = true;

            for (int i = 0; i < RobotLocs.length(); i++) {
                JSONObject beaconObject = RobotLocs.getJSONObject(i);
                if(beaconObject.optString("bid").equals("0x000000000008")){
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
                if(distanceToA < 1.5){
                    robotCurrentHorizontal = beaconCoordinateA.x;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    robotCurrentHorizontal = (beaconCoordinateA.x + beaconCoordinateB.x)/2;
                }
                showHumanCurrentLocation(isHorizontal, (float) robotCurrentHorizontal, isVertical, 0);

            }

            //calculate the vertical movement distance
            if(isVertical == true) {
                if(distanceToA < 1.5){
                    robotCurrentVertical = beaconCoordinateA.y;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    robotCurrentVertical = (beaconCoordinateA.y + beaconCoordinateB.y)/2;
                }
                showHumanCurrentLocation(isHorizontal, 0, isVertical, (float)robotCurrentVertical);
            }


        }
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
                    Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateA.x + beaconCoordinateA.y);
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
                    Log.d(TAG, "nearest beacon station coordinate:" + beaconCoordinateB.x + beaconCoordinateB.y);
                }
            }

            //Calculate the distance between nearest beacon station and 2nd nearest beacon station
            //on the virtualMap
            //double distanceAB = distanceCalculation(beaconCoordinateA, beaconCoordinateB);
            Log.d(TAG, "The dista" + beaconCoordinateB.x + beaconCoordinateB.y);
            //distanceAX = (distanceToA * distanceToA - distanceToB * distanceToB + distanceAB * distanceAB) / (2 * distanceAB);


            double userCurrentVertical = 0;
            double userCurrentHorizontal = 0;

            boolean isHorizontal = false;
            boolean isVertical = true;

            for (int i = 0; i < locs.length(); i++) {
                JSONObject beaconObject = locs.getJSONObject(i);
                if(beaconObject.optString("bid").equals("0x000000000008")){
                    isHorizontal = false;
                    isVertical = true;
                }
                if(beaconObject.optString("bid").equals("x000000000001")){
                    isHorizontal = true;
                    isVertical = false;
                }
            }

            //calculate the horizontal movement distance
            if( isHorizontal == true){
                if(distanceToA < 1.5){
                    userCurrentHorizontal = beaconCoordinateA.x;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    userCurrentHorizontal = (beaconCoordinateA.x + beaconCoordinateB.x)/2;
                }
                showHumanCurrentLocation(isHorizontal, (float) userCurrentHorizontal, isVertical, 0);

            }

            //calculate the vertical movement distance
            if(isVertical == true) {
                if(distanceToA < 1.5){
                    userCurrentVertical = beaconCoordinateA.y;
                    Log.d(TAG, "The distance to the" + beaconIdA + "smaller than 1 meter");
                } else {
                    userCurrentVertical = (beaconCoordinateA.y + beaconCoordinateB.y)/2;
                }
                showHumanCurrentLocation(isHorizontal, 0, isVertical, (float)userCurrentVertical);
            }
        }
    }



    // initial user current location, should triggered every time this activity load.
    public void initialLocation(ImageView imageTarget, float horizontalTarget, float verticalTarget){
        objectAnimator= ObjectAnimator.ofFloat(imageTarget,"x",horizontalTarget);
        objectAnimator.setDuration(0);
        objectAnimator.start();

        objectAnimator= ObjectAnimator.ofFloat(imageTarget,"y", verticalTarget);
        objectAnimator.setDuration(0);
        objectAnimator.start();
    }


    // update the human location in real-time
    public void showHumanCurrentLocation(boolean isHorizontal, float horizontalTarget,
                                         boolean isVertical, float verticalTarget){
        if(isHorizontal == true) {
            objectAnimator = ObjectAnimator.ofFloat(imageHuman, "x", horizontalTarget);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        }
        if(isVertical == true) {
            objectAnimator= ObjectAnimator.ofFloat(imageHuman,"y", verticalTarget);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        }
    }

    // update the robot location in real-time
    public void showRobotCurrentLocation(boolean isHorizontal, float horizontalTarget,
                                         boolean isVertical, float verticalTarget){
        if(isHorizontal == true) {
            objectAnimator = ObjectAnimator.ofFloat(imageRobot, "x", horizontalTarget);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        }
        if(isVertical == true) {
            objectAnimator= ObjectAnimator.ofFloat(imageRobot,"y", verticalTarget);
            objectAnimator.setDuration(500);
            objectAnimator.start();
        }
    }

    public HashMap<String,PointF> initBeaconStationLocation() {

        // HashMap<String, PointF> virtualMap = new HashMap<>();
        this.virtualMap.put("0x000000000060", new PointF(680,500));
        this.virtualMap.put("0x000000000038", new PointF(680, 640));
        this.virtualMap.put("0x000000000022", new PointF(680,780));
        this.virtualMap.put("0x000000000011", new PointF(680, 920));
        this.virtualMap.put("0x000000000008", new PointF(680,1060));
        //this.virtualMap.put("0x000000000007", new PointF(640, 1000));
        this.virtualMap.put("0x000000000006", new PointF(680,1200));
        this.virtualMap.put("0x000000000005", new PointF(600, 1200));
        this.virtualMap.put("0x000000000001", new PointF(450,1200));
        return (this.virtualMap);
    }

}
