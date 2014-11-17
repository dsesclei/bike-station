package com.bikerecoverynetwork.BikeStation;

import java.util.*;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.estimote.sdk.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class MainActivity extends Activity {

    private static final String ESTIMOTE_PROXIMITY_UUID = "B9407F30-F5F8-466E-AFF9-25556B57FE6D";
    private static final Region ALL_ESTIMOTE_BEACONS = new Region("regionId", ESTIMOTE_PROXIMITY_UUID, null, null);
    private BeaconManager beaconManager;

    private Set<Beacon> recentlyNearbyBeacons = new HashSet<Beacon>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        beaconManager = new BeaconManager(getApplicationContext());
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, List<Beacon> beacons) {
                Set<Beacon> nearbyBeacons = new HashSet<Beacon>(beacons);
                Set<Beacon> removedBeacons = new HashSet<Beacon>(recentlyNearbyBeacons);
                removedBeacons.removeAll(nearbyBeacons);
                Set<Beacon> newBeacons = new HashSet<Beacon>(nearbyBeacons);
                newBeacons.removeAll(recentlyNearbyBeacons);
                if (newBeacons.size() > 0) {
                    Log.d("Beacon", "New beacons: " + newBeacons);
                    sendJson("http://bikerecoverynetwork.com/station", "in=" + beaconsToJson(newBeacons));
                }
                if (removedBeacons.size() > 0) {
                    Log.d("Beacon", "Removed beacons: " + removedBeacons);
                    sendJson("http://bikerecoverynetwork.com/station", "out=" + beaconsToJson(removedBeacons));
                }
                recentlyNearbyBeacons = new HashSet<Beacon>(nearbyBeacons);
            }
        });
    }

    private String beaconsToJson(Set<Beacon> beacons) {
        return beaconsToJson(new ArrayList<Beacon>(beacons));
    }

    private String beaconsToJson(List<Beacon> beacons) {
        String json = new String("[");
        for (Beacon beacon : beacons) {
            json += "{ 'major': " + beacon.getMajor() + ", 'minor': " + beacon.getMinor() + "},";
        }
        json += "]";
        return json;
    }

    private void sendJson(final String url, final String json) {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    HttpClient client = new DefaultHttpClient();

                    try {
                        HttpPost req = new HttpPost(url);
                        StringEntity params;
                        try {
                            params = new StringEntity(json);
                        } catch (Exception e) {
                            Log.d("Beacon", e.toString());
                            return;
                        }
                        req.addHeader("Content-Type", "application/x-www-form-urlencoded");
                        req.setEntity(params);
                        try {
                            HttpResponse response = client.execute(req);
                        } catch (Exception e) {
                            Log.d("Beacon", e.toString());
                            return;
                        }
                    } catch (Exception e) {
                        Log.d("Beacon", e.toString());
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS);
                } catch (Exception e) {
                    Log.e("Beacon", "Cannot start ranging", e);
                }
            }
        });
    }
}
