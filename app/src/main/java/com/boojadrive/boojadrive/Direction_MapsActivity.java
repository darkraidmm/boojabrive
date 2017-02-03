package com.boojadrive.boojadrive;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.input.InputManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Direction_MapsActivity extends FragmentActivity implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,LocationListener,EditText.OnEditorActionListener,
         OnMapReadyCallback {

    private static final String TAG = "@@@";
    private GoogleApiClient mGoogleApiClient = null;
    private LocationRequest mLocationRequest;
    private static final int REQUEST_CODE_LOCATION = 2000;//임의의 정수로 정의
    private static final int REQUEST_CODE_GPS = 2001;//임의의 정수로 정의
    private String GetData="@@@";
    private EditText mEditText;
    private  List<Address> addresses = null;
    private InputMethodManager imm;
    private EditText searchTxt;
    private GoogleMap googleMap;
    LocationManager locationManager;
    MapFragment mapFragment;
    boolean setGPS = false;
    LatLng SEOUL = new LatLng(37.56, 126.97);

   // private int check=0;


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient
                .Builder(Direction_MapsActivity.this)
                .addApi(LocationServices.API)
               // .addApi(Places.GEO_DATA_API)
                //.addApi(Places.PLACE_DETECTION_API)
                .addConnectionCallbacks(Direction_MapsActivity.this)
                .addOnConnectionFailedListener(Direction_MapsActivity.this)
                .build();
        mGoogleApiClient.connect();
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage("GPS가 비활성화 되어있습니다. 활성화 할까요?")
                .setCancelable(false)
                .setPositiveButton("설정", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(callGPSSettingIntent, REQUEST_CODE_GPS);
                    }
                });

        alertDialogBuilder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction__maps);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(Direction_MapsActivity.this);
        Intent intent =getIntent();
        GetData=intent.getStringExtra("value");
        mEditText = (EditText)findViewById(R.id.Drirection_Text);
        mEditText.setOnEditorActionListener(this);
        init();

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    public boolean checkLocationPermission() {
        Log.d(TAG, "checkLocationPermission");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                //퍼미션 요청을 위해 UI를 보여줘야 하는지 검사
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                    //Prompt the user once explanation has been shown;
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);

                } else
                    //UI보여줄 필요 없이 요청
                    requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_LOCATION);

                return false;
            } else {

                Log.d(TAG, "checkLocationPermission" + "이미 퍼미션 획득한 경우");

                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                    Log.d(TAG, "checkLocationPermission Version >= M");
                    showGPSDisabledAlertToUser();
                }

                if (mGoogleApiClient == null) {
                    Log.d(TAG, "checkLocationPermission " + "mGoogleApiClient==NULL");
                    buildGoogleApiClient();
                } else Log.d(TAG, "checkLocationPermission " + "mGoogleApiClient!=NULL");

                if (mGoogleApiClient.isConnected())
                    Log.d(TAG, "checkLocationPermission" + "mGoogleApiClient 연결되 있음");
                else Log.d(TAG, "checkLocationPermission" + "mGoogleApiClient 끊어져 있음");


                mGoogleApiClient.reconnect();//이미 연결되 있는 경우이므로 다시 연결

                googleMap.setMyLocationEnabled(true);
            }
        } else {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                Log.d(TAG, "checkLocationPermission Version < M");
                showGPSDisabledAlertToUser();
            }

            if (mGoogleApiClient == null) {
                buildGoogleApiClient();
            }
            googleMap.setMyLocationEnabled(true);
        }

        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_LOCATION: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //퍼미션이 허가된 경우
                    if ((ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)) {

                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                            Log.d(TAG, "onRequestPermissionsResult");
                            showGPSDisabledAlertToUser();
                        }


                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "퍼미션 취소", Toast.LENGTH_LONG).show();
                }
            }
        }
    }



    @Override
    public void onMapReady(GoogleMap map) {
        //////////////
        //////////
        googleMap = map;

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));


        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            /////////////////
            //////////////////////
            @Override
            public void onMapLoaded() {
                Log.d(TAG, "onMapLoaded");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    checkLocationPermission();
                } else {

                    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !setGPS) {
                        Log.d(TAG, "onMapLoaded");
                        showGPSDisabledAlertToUser();
                    }

                    if (mGoogleApiClient == null) {
                        buildGoogleApiClient();
                    }
                    googleMap.setMyLocationEnabled(true);
                }

            }
        });


        //구글 플레이 서비스 초기화
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();

                googleMap.setMyLocationEnabled(true);
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            }
        } else {
            buildGoogleApiClient();
            googleMap.setMyLocationEnabled(true);
        }
    }
/////////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

@Override
public void onConnectionSuspended(int cause) {
    //구글 플레이 서비스 연결이 해제되었을 때, 재연결 시도
    Log.d(TAG, "Connection suspended");
    mGoogleApiClient.connect();
}

    //성공적으로 GoogleApiClient 객체 연결되었을 때 실행
@Override
public void onConnected(@Nullable Bundle bundle) {
    Log.d(TAG, "onConnected");

    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
        setGPS = true;

    mLocationRequest = new LocationRequest();
    //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    mLocationRequest.setInterval(1000);
    mLocationRequest.setFastestInterval(1000);


    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        Log.d(TAG, "onConnected " + "getLocationAvailability mGoogleApiClient.isConnected()=" + mGoogleApiClient.isConnected());
        if (!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();


        // LocationAvailability locationAvailability = LocationServices.FusedLocationApi.getLocationAvailability(mGoogleApiClient);

        if (setGPS && mGoogleApiClient.isConnected())//|| locationAvailability.isLocationAvailable() )
        {
            Log.d(TAG, "onConnected " + "requestLocationUpdates");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) return;

            //현재 위치에 마커 생성
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("현재위치");
            googleMap.addMarker(markerOptions);

           // 지도 상에서 보여주는 영역 이동
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }

    }


}
    protected void onStart() {
        super.onStart();

        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    @Override
    public void onPause() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG, "OnDestroy");

        if (mGoogleApiClient != null) {
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionFailedListener(this);

            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }

            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
        }

        super.onDestroy();
    }
    @Override
    public void onLocationChanged(Location location) {

        /* String errorMessage = "";

        googleMap.clear();
        if(check==0){

           //현재 위치에 마커 생성
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("현재위치");
            googleMap.addMarker(markerOptions);

            //지도 상에서 보여주는 영역 이동
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            googleMap.getUiSettings().setCompassEnabled(true);


            //지오코더... GPS를 주소로 변환
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());

            // Address found using the Geocoder.
            List<Address> addresses = null;

            try {
                // Using getFromLocation() returns an array of Addresses for the area immediately
                // surrounding the given latitude and longitude. The results are a best guess and are
                // not guaranteed to be accurate.
                addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(), 1);
                // In this sample, we get just a single address.1);
            } catch (IOException ioException) {
                // Catch network or other I/O problems.
                errorMessage = "지오코더 서비스 사용불가";
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = "잘못된 GPS 좌표";
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size() == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = "주소 미발견";
                    Log.e(TAG, errorMessage);
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            } else {
            /*Address address = addresses.get(0);
            EditText editText1 = (EditText) findViewById(R.id.editText);
            editText1.setText("출발지: "+address.getAddressLine(0).toString())
            Button button = (Button) findViewById(R.id.Call_button);
            button.setText("출발지: " + address.getAddressLine(0).toString());
            Toast.makeText(this, address.getAddressLine(0).toString(), Toast.LENGTH_LONG).show();
                check=1;

            }

        }*/

    }







    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        // TODO Auto-generated method stub

        //오버라이드한 onEditorAction() 메소드

        if(v.getId()==R.id.Drirection_Text && actionId== EditorInfo.IME_ACTION_SEARCH){ // 뷰의 id를 식별, 키보드의 완료 키 입력 검출

            EditText drirectiontext=(EditText)findViewById(R.id.Drirection_Text);
            String Location=drirectiontext.getText().toString();
            List<Address> list2=null;
            Geocoder coder2 = new Geocoder(this, Locale.getDefault());
            try {
                list2=coder2.getFromLocationName(Location,10);
            }
            catch (IOException ioe){
                Toast.makeText(getApplicationContext(),"지오코더오류",Toast.LENGTH_SHORT).show();
            }

            if(list2!=null) {
                double latitude = list2.get(0).getLatitude();
                double longitude = list2.get(0).getLongitude();
                TextView DrirectionText=(TextView)findViewById(R.id.textView);
                StringBuilder stringBuilder=new StringBuilder("출발지: ");
                stringBuilder.append(GetData);
                stringBuilder.append("\n\n ");
                stringBuilder.append("목적지: ");
                stringBuilder.append(list2.get(0).getAddressLine(0).toString());
                stringBuilder.append("\n\n ");
                stringBuilder.append("전화번호: ");
                TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
                if(telephonyManager.getLine1Number()!=null) {
                    stringBuilder.append(telephonyManager.getLine1Number().toString());
                }

                DrirectionText.setText(stringBuilder.toString());



                LatLng latLng = new LatLng(latitude, longitude);
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("목적지");
                googleMap.addMarker(markerOptions);

                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                googleMap.getUiSettings().setCompassEnabled(true);
                //Button button =(Button)findViewById()
            }
        }


        return false;
    }



    public void OnFindButtonCLlicked(View view){

        EditText drirectiontext=(EditText)findViewById(R.id.Drirection_Text);
        String Location=drirectiontext.getText().toString();
        List<Address> list2=null;
        Geocoder coder2 = new Geocoder(this, Locale.getDefault());
        try {
            list2=coder2.getFromLocationName(Location,10);
        }
        catch (IOException ioe){
            Toast.makeText(getApplicationContext(),"지오코더오류",Toast.LENGTH_SHORT).show();
        }

        if(list2!=null) {
            double latitude = list2.get(0).getLatitude();
            double longitude = list2.get(0).getLongitude();
            TextView DrirectionText=(TextView)findViewById(R.id.textView);
            StringBuilder stringBuilder=new StringBuilder("출발지: ");
            stringBuilder.append(GetData);
            stringBuilder.append("\n\n ");
            stringBuilder.append("목적지: ");
            stringBuilder.append(list2.get(0).getAddressLine(0).toString());
            stringBuilder.append("\n\n ");
            stringBuilder.append("전화번호: ");
            TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(getApplicationContext().TELEPHONY_SERVICE);
            if(telephonyManager.getLine1Number()!=null) {
                stringBuilder.append(telephonyManager.getLine1Number().toString());
            }

            DrirectionText.setText(stringBuilder.toString());



            LatLng latLng = new LatLng(latitude, longitude);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("목적지");
            googleMap.addMarker(markerOptions);

            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
            googleMap.getUiSettings().setCompassEnabled(true);
            hidekeybord();


            //Button button =(Button)findViewById()
        }
    }
    /*public void OnCall_button_CLlicked(View view){
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("tel:16448898"));
        startActivity(intent);

    }
    public void OnDriver_Button_CLlicked(View view){

    }*/

    public void OnReturn_button_CLlicked(View view){
        EditText drirectiontext=(EditText)findViewById(R.id.Drirection_Text);
        String Location=drirectiontext.getText().toString();
        List<Address> list2=null;
        Geocoder coder2 = new Geocoder(this, Locale.getDefault());
        try {
            list2=coder2.getFromLocationName(Location,10);
        }
        catch (IOException ioe){
            Toast.makeText(getApplicationContext(),"지오코더오류",Toast.LENGTH_SHORT).show();
        }
        if(list2!=null) {
            Intent intent2 = new Intent(this, MapsActivity.class);
            intent2.putExtra("value2",list2.get(0).getAddressLine(0).toString());
            setResult(RESULT_OK, intent2);


        }





        finish();
    }

    private void init(){

        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        searchTxt = (EditText) findViewById(R.id.Drirection_Text);

    }

    public void hidekeybord(){
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputMethodManager inputMethodManager= (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(0,0);
                // inputMethodManager.hideSoftInputFromInputMethod(searchTxt.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                }
        },200);


    }

}
