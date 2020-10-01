package com.ds.androideatitv2shipper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ds.androideatitv2shipper.Adapters.MyShippingOrderAdapter;
import com.ds.androideatitv2shipper.Common.Common;
import com.ds.androideatitv2shipper.Model.EventBus.UpdateShippingOrderEvent;
import com.ds.androideatitv2shipper.Model.FCMSendData;
import com.ds.androideatitv2shipper.Model.ShippingOrderModel;
import com.ds.androideatitv2shipper.Model.TokenModel;
import com.ds.androideatitv2shipper.Remote.IFCMService;
import com.ds.androideatitv2shipper.Remote.IGoogleAPI;
import com.ds.androideatitv2shipper.Remote.RetrofitFCMClient;
import com.ds.androideatitv2shipper.Remote.RetrofitGoogleAPIClient;
import com.ds.androideatitv2shipper.ui.shipOrders.OrdersToShipFragment;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.paperdb.Paper;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ShippingActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private IFCMService ifcmService;

    private Context context;

    private OrdersToShipFragment ordersToShipFragment;

    private Marker shipperMarker;
    private ShippingOrderModel shippingOrderModel;
    private MyShippingOrderAdapter myShippingOrderAdapter;

    //Animation
    private Handler handler;
    private int index, next;
    private LatLng start, end;
    private float v;
    private double lat, lng;
    private Polyline blackPolyline, grayPolyline, redPolyline, yellowPolyline;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private List<LatLng> polyLineList;
    private IGoogleAPI iGoogleAPI;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @BindView(R.id.txt_order_number)
    TextView txt_order_number;
    @BindView(R.id.txt_name)
    TextView txt_name;
    @BindView(R.id.txt_address)
    TextView txt_address;
    @BindView(R.id.txt_date)
    TextView txt_date;

    @BindView(R.id.btn_start_trip)
    MaterialButton btn_start_trip;
    @BindView(R.id.btn_call)
    MaterialButton btn_call;
    @BindView(R.id.btn_done)
    MaterialButton btn_done;
    @BindView(R.id.btn_show)
    MaterialButton btn_show;
    @BindView(R.id.expandable_layout)
    ExpandableLayout expandable_layout;
    /*@BindView(R.id.btn_ship_now)
    MaterialButton btn_ship_now;*/


    @BindView(R.id.img_food_image)
    ImageView img_food_image;


   @OnClick(R.id.btn_done)
    void onDoneButtonClick()
    {

        if (shippingOrderModel != null)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("SET TO DELIVERED")
                    .setMessage("Are you sure that you want to mark this order as delivered?")
                    .setNegativeButton("CANCEL", ((dialogInterface, which) -> dialogInterface.dismiss()))
                    .setPositiveButton("DELIVERED", ((dialogInterface, which) -> {
                        //Delete data offline
                      /*  Paper.init(this);
                        Paper.book().delete(Common.TRIP_START);
                        Paper.book().delete(Common.SHIPPING_ORDER_DATA);*/

                      AlertDialog dialog = new AlertDialog.Builder(this)
                              .setCancelable(false)
                              .setMessage("Waiting...")
                              .create();
                      //Update order
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("orderStatus", 2);


                        FirebaseDatabase.getInstance()
                                .getReference(Common.ORDER_REF)
                                .child(shippingOrderModel.getOrderModel().getKey())
                                .updateChildren(updateData)
                                .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                .addOnSuccessListener(aVoid -> {

                                    //Delete Shipping Order info
                                    FirebaseDatabase.getInstance()
                                            .getReference(Common.SHIPPING_ORDER_REF)
                                            .child(shippingOrderModel.getOrderModel().getKey())
                                            .removeValue()
                                            .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show())
                                            .addOnSuccessListener(aVoid1 -> {
                                                //Delete done
                                                //We will get token and send notificaiton here
                                                //first of all we take the token of the user to send notificatiın to itr later
                                                FirebaseDatabase.getInstance()
                                                        .getReference(Common.TOKEN_REF)
                                                        .child(shippingOrderModel.getOrderModel().getUserId())
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                if(dataSnapshot.exists())
                                                                {
                                                                    TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                                                    Map<String, String> notiData = new HashMap<>();
                                                                    notiData.put(Common.NOTI_TITLE, "Order Delivery!");
                                                                    notiData.put(Common.NOTI_CONTENT, new StringBuilder("Your order has been shipped by shipper ")
                                                                          .append(Common.currentShipperUser.getPhone()).toString());

                                                                    FCMSendData sendData = new FCMSendData(tokenModel.getToken(),notiData);

                                                                    compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                                            .subscribeOn(Schedulers.io())
                                                                            .observeOn(AndroidSchedulers.mainThread())
                                                                            .subscribe(fcmResponse -> {
                                                                                dialog.dismiss();
                                                                                if (fcmResponse.getSuccess() == 1)
                                                                                {
                                                                                    Toast.makeText(ShippingActivity.this, "Done!", Toast.LENGTH_SHORT).show();
                                                                                }
                                                                                else
                                                                                {
                                                                                    Toast.makeText(ShippingActivity.this, "Order Updated successfully but failed to send notification!", Toast.LENGTH_SHORT).show();
                                                                                }

                                                                                if (!TextUtils.isEmpty(Paper.book().read(Common.TRIP_START)))
                                                                                    Paper.book().delete(Common.TRIP_START);
                                                                                EventBus.getDefault().postSticky(new UpdateShippingOrderEvent());
                                                                                finish();

                                                                            }, throwable -> {
                                                                                dialog.dismiss();
                                                                                Toast.makeText(ShippingActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                                            }));
                                                                }
                                                                else
                                                                {
                                                                    dialog.dismiss();
                                                                    Toast.makeText(ShippingActivity.this, "Token Not Found!", Toast.LENGTH_SHORT).show();
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                                dialog.dismiss();
                                                                Toast.makeText(ShippingActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                            });
                                });

                    }));
            AlertDialog deleteDialog = builder.create();
            deleteDialog.show();

            Button negativeButton = deleteDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.GREEN);

            Button positiveButton = deleteDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setTextColor(Color.RED);

        }

    }



    @OnClick(R.id.btn_show)
    void onShowClick()
    {
        if (expandable_layout.isExpanded())
            btn_show.setText("SHOW");
        else
            btn_show.setText("HIDE");
        expandable_layout.toggle();
    }

    AutocompleteSupportFragment places_fragment;
    PlacesClient placesClient;
    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @OnClick(R.id.btn_start_trip)
    void onStartTripClick() {

        try {
            String data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
            Paper.book().write(Common.TRIP_START, data);

            //drawRoutes(data);

            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        Map<String, Object> update_data = new HashMap<>();
                        update_data.put("currentLat", location.getLatitude());
                        update_data.put("currentLng", location.getLongitude());

                        FirebaseDatabase.getInstance()
                                .getReference(Common.SHIPPING_ORDER_REF)
                                .child(shippingOrderModel.getKey())
                                .updateChildren(update_data)
                                .addOnFailureListener(e -> Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show()
                                ).addOnSuccessListener(aVoid -> drawRoutes(data));


                    }).addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show());

            //disable trip button after clicked once
            btn_start_trip.setEnabled(false);
        }
        catch (Exception e)
        {
            Toast.makeText(this, "Something went wrong! "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private boolean isInit = false;
    private Location previousLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_shipping);
            iGoogleAPI = RetrofitGoogleAPIClient.getInstance().create(IGoogleAPI.class);
            ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

                initPlaces();
                setupAutoCompletePlaces();
                ButterKnife.bind(this);

                buildLocationRequerst();
                buildLocationCallback();


            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {
                            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                    .findFragmentById(R.id.map);
                            mapFragment.getMapAsync(ShippingActivity.this::onMapReady);
                            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ShippingActivity.this);
                            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(ShippingActivity.this, "You must enable the location permission!", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                        }
                    }).check();
        }
        catch (Exception e)
        {
            Toast.makeText(this, "something went wrong! "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupAutoCompletePlaces() {

            places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.places_autocomplete_fragment);
            places_fragment.setPlaceFields(placeFields);
            places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    drawRoutes(place);
                }

                @Override
                public void onError(@NonNull Status status) {
                    Toast.makeText(ShippingActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void drawRoutes(Place place) {

        try {
                mMap.addMarker (new MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                        .title(place.getName())
                        .snippet(place.getAddress())
                        .position(place.getLatLng())); // Add destination


                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                        .addOnSuccessListener(location -> {
                            String to = new StringBuilder()
                                    .append(place.getLatLng().latitude)
                                    .append(",")
                                    .append(place.getLatLng().longitude)
                                    .toString();
                            String from = new StringBuilder()
                                    .append(location.getLatitude())
                                    .append(",")
                                    .append(location.getLongitude())
                                    .toString();

                            compositeDisposable.add(iGoogleAPI.getDirections("driving",
                                    "less_driving",
                                    from,to,
                                    getString(R.string.google_maps_key))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(s -> {

                                        try {
                                            //Parse json
                                            JSONObject jsonObject = new JSONObject(s);
                                            JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                JSONObject route = jsonArray.getJSONObject(i);
                                                JSONObject poly = route.getJSONObject("overview_polyline");
                                                String polyLine = poly.getString("points");
                                                polyLineList = Common.decodePoly(polyLine);
                                            }
                                            polylineOptions = new PolylineOptions();
                                            polylineOptions.color(Color.YELLOW);
                                            polylineOptions.width(12);
                                            polylineOptions.startCap(new SquareCap());
                                            polylineOptions.jointType(JointType.ROUND);
                                            polylineOptions.addAll(polyLineList);
                                            yellowPolyline = mMap.addPolyline(polylineOptions);
                                        }
                                        catch (Exception e)
                                        {
                                            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }

                                    }, throwable -> Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show()));

                        });
            }
        catch (Exception e)
        {
            Toast.makeText(this, "Something went Wrong!" +e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void drawRoutes(String data) {


        try {
            ShippingOrderModel shippingOrderModel = new Gson()
                    .fromJson(data, new TypeToken<ShippingOrderModel>(){}.getType());

            //Adding Box
            mMap.addMarker (new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.box))
                    .title(shippingOrderModel.getOrderModel().getUserName())
                    .snippet(shippingOrderModel.getOrderModel().getShippingAddress())
                    .position(new LatLng(shippingOrderModel.getOrderModel().getLat(),
                            shippingOrderModel.getOrderModel().getLng())));

            fusedLocationProviderClient.getLastLocation()
                    .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(location -> {
                        String to = new StringBuilder()
                                .append(shippingOrderModel.getOrderModel().getLat())
                                .append(",")
                                .append(shippingOrderModel.getOrderModel().getLng())
                                .toString();
                        if(location == null)
                        {
                            Toast.makeText(this, "Be sure your GBS is turned on please!", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            String from = new StringBuilder()
                                    .append(location.getLatitude())
                                    .append(",")
                                    .append(location.getLongitude())
                                    .toString();



                        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                                "less_driving",
                                from,to,
                                getString(R.string.google_maps_key))
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(s -> {

                                    try {
                                        //Parse json
                                        JSONObject jsonObject = new JSONObject(s);
                                        JSONArray jsonArray = jsonObject.getJSONArray("routes");
                                        for (int i = 0; i < jsonArray.length(); i++) {
                                            JSONObject route = jsonArray.getJSONObject(i);
                                            JSONObject poly = route.getJSONObject("overview_polyline");
                                            String polyLine = poly.getString("points");
                                            polyLineList = Common.decodePoly(polyLine);
                                        }
                                        polylineOptions = new PolylineOptions();
                                        polylineOptions.color(Color.RED);
                                        polylineOptions.width(12);
                                        polylineOptions.startCap(new SquareCap());
                                        polylineOptions.jointType(JointType.ROUND);
                                        polylineOptions.addAll(polyLineList);
                                        redPolyline = mMap.addPolyline(polylineOptions);
                                    }
                                    catch (Exception e)
                                    {
                                        Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }

                                }, throwable -> Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show()));
                        }
                    });
        }
        catch (Exception e)
        {
            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }



    }

    private void initPlaces() {

            Places.initialize(this, getString(R.string.google_maps_key));
            placesClient = Places.createClient(this);
    }

    private void setShippingOrder() {
        Paper.init(this);

        String data;

        if (TextUtils.isEmpty(Paper.book().read(Common.TRIP_START)))
        {
            //if empty just continue
            btn_start_trip.setEnabled(true);
            data = Paper.book().read(Common.SHIPPING_ORDER_DATA);
        }

        else
        {
            btn_start_trip.setEnabled(false);
            data = Paper.book().read(Common.TRIP_START);
        }

        if (!TextUtils.isEmpty(data))
        {
            drawRoutes(data);
            shippingOrderModel = new Gson()
                    .fromJson(data, new TypeToken<ShippingOrderModel>() {}.getType());
            if (shippingOrderModel != null)
            {
                try {
                    Common.setSpanStringColor("Name: ",
                            shippingOrderModel.getOrderModel().getUserName(),
                            txt_name,
                            Color.parseColor("#333639"));

                    txt_date.setText(new StringBuilder()
                            .append(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
                                    .format(shippingOrderModel.getOrderModel().getCreateDate())));

                    Common.setSpanStringColor("No: ",
                            shippingOrderModel.getOrderModel().getKey(),
                            txt_order_number,
                            Color.parseColor("#673ab7"));

                    Common.setSpanStringColor("Address: ",
                            shippingOrderModel.getOrderModel().getShippingAddress(),
                            txt_address,
                            Color.parseColor("#795548"));

                    Glide.with(this)
                            .load(shippingOrderModel.getOrderModel().getCartItemList().get(0)
                                    .getFoodImage())
                            .into(img_food_image);
                }
                catch (Exception e)
                {
                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                }

        }
        else
        {
            Toast.makeText(this, "Shipping order is null!", Toast.LENGTH_SHORT).show();
        }

    }




    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // Add a marker in Sydney and move the camera
                LatLng locationShipper = new LatLng(locationResult.getLastLocation().getLatitude(),
                        locationResult.getLastLocation().getLongitude());

                updateLocation(locationResult.getLastLocation());


                if (shipperMarker == null)
                {
                    //inflate drawable
                    int height , width;
                    height = width = 80;
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(ShippingActivity.this, R.drawable.shippernew);
                    Bitmap resize = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(),width, height, false);

                    shipperMarker = mMap.addMarker(new MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(resize))
                            .position(locationShipper).title("You"));

                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 18));

                }


                //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(locationShipper, 15));
                if (isInit && previousLocation != null)
                {
                    String from = new StringBuilder()
                            .append(previousLocation.getLatitude())
                            .append(",")
                            .append(previousLocation.getLongitude())
                            .toString();
                   String to = new StringBuilder()
                           .append(locationShipper.latitude)
                           .append(",")
                           .append(locationShipper.longitude)
                           .toString();

                    moveMarkerAnimation(shipperMarker, from, to);

                    previousLocation = locationResult.getLastLocation();
                }
                if (!isInit)
                {
                    isInit = true;
                    previousLocation = locationResult.getLastLocation();
                }
            }
        };
    }

    private void updateLocation(Location lastLocation) {
        Map<String, Object> update_data = new HashMap<>();
        update_data.put("currentLat", lastLocation.getLatitude());
        update_data.put("currentLng", lastLocation.getLongitude());

        String data = Paper.book().read(Common.TRIP_START);

        if (!TextUtils.isEmpty(data))
        {
            ShippingOrderModel shippingOrderModel = new Gson()
                    .fromJson(data, new TypeToken<ShippingOrderModel>(){}.getType());

            if (shippingOrderModel != null)
            {
                FirebaseDatabase.getInstance()
                        .getReference(Common.SHIPPING_ORDER_REF)
                        .child(shippingOrderModel.getKey())
                        .updateChildren(update_data)
                        .addOnFailureListener(e -> Toast.makeText(ShippingActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
        else
        {
            Toast.makeText(this, "Please press Start trip button!", Toast.LENGTH_SHORT).show();
        }
    }

    private void moveMarkerAnimation(Marker marker, String from, String to) {
        //Request diections API to get data
        compositeDisposable.add(iGoogleAPI.getDirections("driving",
                "less_driving",
                from, to,
                getString(R.string.google_maps_key))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(returnResult -> {

            Log.d("API_RETURN", returnResult);

            try {
                //Parse json
                JSONObject jsonObject = new JSONObject(returnResult);
                JSONArray jsonArray = jsonObject.getJSONArray("routes");
                for (int i=0; i<jsonArray.length();i++)
                {
                    JSONObject route = jsonArray.getJSONObject(i);
                    JSONObject poly = route.getJSONObject("overview_polyline");
                    String polyLine = poly.getString("points");
                    polyLineList = Common.decodePoly(polyLine);
                }

                polylineOptions = new PolylineOptions();
                polylineOptions.color(Color.GRAY);
                polylineOptions.width(5);
                polylineOptions.startCap(new SquareCap());
                polylineOptions.jointType(JointType.ROUND);
                polylineOptions.addAll(polyLineList);
                grayPolyline = mMap.addPolyline(polylineOptions);

                blackPolylineOptions = new PolylineOptions();
                blackPolylineOptions.color(Color.BLACK);
                blackPolylineOptions.width(5);
                blackPolylineOptions.startCap(new SquareCap());
                blackPolylineOptions.jointType(JointType.ROUND);
                blackPolylineOptions.addAll(polyLineList);
                blackPolyline = mMap.addPolyline(blackPolylineOptions);

                //Animator
                ValueAnimator polylineAnimator = ValueAnimator.ofInt(0,100);
                polylineAnimator.setDuration(2000);
                polylineAnimator.setInterpolator(new LinearInterpolator());
                polylineAnimator.addUpdateListener(animation -> {
                    List<LatLng> points = grayPolyline.getPoints();
                    int percentValue = (int) animation.getAnimatedValue();
                    int size = points.size();
                    int newPoints = (int) (size*(percentValue/100.0f));
                    List<LatLng> p = points.subList(0, newPoints);
                    blackPolyline.setPoints(p);

                });
                polylineAnimator.start();

                //Bike moving
                handler = new Handler();
                index = -1;
                next = 1;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (index < polyLineList.size() -1)
                        {
                            index++;
                            next = index+1;
                            start = polyLineList.get(index);
                            end = polyLineList.get(next);
                        }

                        ValueAnimator valueAnimator = ValueAnimator.ofInt(0,1);
                        valueAnimator.setDuration(1500);
                        valueAnimator.setInterpolator(new LinearInterpolator());
                        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                v = valueAnimator.getAnimatedFraction();
                                lng = v*end.longitude + (1-v)
                                    *start.longitude;
                                lat = v*end.latitude + (1-v)
                                        *start.latitude;

                                LatLng newPos = new LatLng(lat, lng);
                                marker.setPosition(newPos);
                                marker.setAnchor(0.5f, 0.5f);
                                marker.setRotation(Common.getBearing(start, newPos));

                                mMap.moveCamera(CameraUpdateFactory.newLatLng(marker.getPosition())); // from animate to moveCamera this will make thing realtime
                            }
                        });

                        valueAnimator.start();
                        if (index < polyLineList.size() - 2 ) // Reach destination
                            handler.postDelayed(this, 1500);

                    }
                }, 1500);

            }
            catch (Exception e)
            {
                Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }, throwable -> {
            if (throwable != null)
            Toast.makeText(ShippingActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }));
    }

    private void buildLocationRequerst() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(15000); // 15 sec
        locationRequest.setFastestInterval(10000); // 10 sec
        locationRequest.setSmallestDisplacement(20f); // 20 meters
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        setShippingOrder();

        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            boolean succes = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this,
                    R.raw.uber_light_with_label));
            if (!succes)
                Log.e("Rido says: ", "Style parsing failed");

        } catch (Resources.NotFoundException ex)
        {
            Log.e("Rido says: ", "Resource could not find");
        }

    }

    @Override
    protected void onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onDestroy();
    }
}