package com.ds.androideatitv2client;


import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.androideatitv2client.Common.Common;
import com.ds.androideatitv2client.Model.UserModel;
import com.ds.androideatitv2client.Remote.ICloudFunctions;
import com.ds.androideatitv2client.Remote.RetrofitCloudClient;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;  //any number
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ICloudFunctions cloudFunctions;

    private DatabaseReference userRef;
    private List<AuthUI.IdpConfig>providers;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @Override
    protected void onStart(){
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        if (listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Init();
    }

    private void Init() {

        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);


        providers= Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());


        userRef= FirebaseDatabase.getInstance().getReference(Common.USER_REFERENCES);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(this).build();
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions.class);


        listener = firebaseAuth -> {

            Dexter.withActivity(this)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(new PermissionListener() {
                        @Override
                        public void onPermissionGranted(PermissionGrantedResponse response) {

                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            if (user != null) {
                                //Account is already logged in
                                CheckUserFromFirebase(user);
                                // Toast.makeText(MainActivity.this, "Already Logged In", Toast.LENGTH_SHORT).show();
                            } else {
                                phoneLogIn();

                            }
                        }

                        @Override
                        public void onPermissionDenied(PermissionDeniedResponse response) {
                            Toast.makeText(MainActivity.this, "You must enable location permission to use the app", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                        }
                    }).check();

        };
    }


    private void CheckUserFromFirebase(FirebaseUser user) {
        dialog.show();
        userRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    FirebaseAuth.getInstance().getCurrentUser()
                            .getIdToken(true)
                            .addOnFailureListener(e -> Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                            .addOnCompleteListener(tokenResultTask -> {
                                Common.authorizeKey = tokenResultTask.getResult().getToken();

                                Map<String,String> headers = new HashMap<>();
                                headers.put("Authorization", Common.buildToken(Common.authorizeKey));

                                compositeDisposable.add(cloudFunctions.getToken(headers)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(braintreeToken -> {

                                            dialog.dismiss();
                                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                            goToHomeActivity(userModel, braintreeToken.getToken());

                                        }, throwable -> {
                                            dialog.dismiss();
                                            Toast.makeText(MainActivity.this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                        }));
                            });
                  /* */
                }
                else{
                    showRegisterDialog(user);
                    dialog.dismiss();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, ""+ databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showRegisterDialog(FirebaseUser user)
    {
        androidx.appcompat.app.AlertDialog.Builder builder= new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please Fill Information");

        View itemView= LayoutInflater.from(this).inflate(R.layout.layout_register, null);
        EditText edt_name= (EditText)itemView.findViewById(R.id.edt_name);

        TextView txt_address_detail = (TextView)itemView.findViewById(R.id.txt_address_detail);


        EditText edt_phone= (EditText)itemView.findViewById(R.id.edt_phone);



        places_fragment = (AutocompleteSupportFragment) getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                placeSelected = place;
                txt_address_detail.setText(place.getAddress());

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(MainActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        //set data
        edt_phone.setText(user.getPhoneNumber());


        builder.setNegativeButton("CANCEL", (dialogInterface, which) -> {
            dialogInterface.dismiss();
        });

        builder.setPositiveButton("REGISTER", (dialogInterface, which) -> {
            if(placeSelected != null) {
                if (TextUtils.isEmpty(edt_name.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Please enter Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserModel userModel = new UserModel();
                userModel.setUid(user.getUid());
                userModel.setName(edt_name.getText().toString());
                userModel.setAddress(txt_address_detail.getText().toString());
                userModel.setPhone(edt_phone.getText().toString());
                userModel.setLat(placeSelected.getLatLng().latitude);
                userModel.setLng(placeSelected.getLatLng().longitude);

                userRef.child(user.getUid())
                        .setValue(userModel)
                        .addOnCompleteListener(task -> {

                            if (task.isSuccessful()) {

                                FirebaseAuth.getInstance().getCurrentUser()
                                        .getIdToken(true)
                                        .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show())
                                        .addOnCompleteListener(tokenResultTask -> {
                                            Common.authorizeKey = tokenResultTask.getResult().getToken();

                                            Map<String, String> headers = new HashMap<>();
                                            headers.put("Authorization", Common.buildToken(Common.authorizeKey));

                                            compositeDisposable.add(cloudFunctions.getToken(headers)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(braintreeToken -> {

                                                        dialogInterface.dismiss();
                                                        dialogInterface.dismiss();
                                                        Toast.makeText(MainActivity.this, "Successfully Registered", Toast.LENGTH_SHORT).show();
                                                        goToHomeActivity(userModel, braintreeToken.getToken());

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(MainActivity.this, "" + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }));
                                        });

                            }

                        });
            }
            else
            {
                Toast.makeText(this, "Please select address", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(itemView);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialog1 -> {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(places_fragment);
            fragmentTransaction.commit();
        });
        dialog.show();
    }

    private void phoneLogIn() {

        startActivityForResult(AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),APP_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) {
            IdpResponse response= IdpResponse.fromResultIntent(data);
            if(resultCode== RESULT_OK)
            {
                FirebaseUser user= FirebaseAuth.getInstance().getCurrentUser();
            }
            else
            {
                Toast.makeText(this, "Failed to Sign in!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void goToHomeActivity(UserModel userModel, String token) {

        FirebaseInstanceId.getInstance()
                .getInstanceId()
                .addOnFailureListener(e -> {
                    Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();

                    Common.currentUser = userModel;
                    Common.currentToken = token;
                    startActivity(new Intent (MainActivity.this, HomeActivity.class));
                    finish();

                }).addOnCompleteListener(task -> {
                    Common.currentUser = userModel;
                    Common.currentToken = token;
                    Common.updateToken(MainActivity.this, task.getResult().getToken());
                    startActivity(new Intent (MainActivity.this, HomeActivity.class));
                    finish();
                });
        


    }
}