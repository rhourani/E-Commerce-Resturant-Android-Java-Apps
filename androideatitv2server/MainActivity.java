package com.ds.androideatitv2server;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.ds.androideatitv2server.Common.Common;
import com.ds.androideatitv2server.Model.ServerUserModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    private static int APP_REQUEST_CODE = 7171;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;
    private AlertDialog dialog;
    private DatabaseReference serverRef;
    private List<AuthUI.IdpConfig> providers;

    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(listener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(listener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {



        providers = Arrays.asList(new AuthUI.IdpConfig.PhoneBuilder().build());

        serverRef = FirebaseDatabase.getInstance().getReference(Common.SERVER_REF);
        firebaseAuth = FirebaseAuth.getInstance();
        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();
        listener = firebaseAuthLocal -> {

            FirebaseUser user = firebaseAuthLocal.getCurrentUser();
            if(user != null)
            {
                //Check user from firebase
                checkServerUserFromFirebase(user);
            }
            else
            {
                phoneLogin();
            }
        };
    }

    private void checkServerUserFromFirebase(FirebaseUser user) {
        dialog.show();
        serverRef.child(user.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists())
                        {
                            ServerUserModel userModel = dataSnapshot.getValue(ServerUserModel.class);
                            if(userModel.isActive())
                            {
                                goToHomeActivity(userModel);
                            }
                            else
                            {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "You be allowed from Admin before you can be able to sign into the app", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            //User does not exists in database
                            dialog.dismiss();
                            showRegisterDialog(user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        dialog.dismiss();
                        Toast.makeText(MainActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegisterDialog(FirebaseUser user) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Register");
        builder.setMessage("Please fill information \n Admin will accept your account later");

        View itemview = LayoutInflater.from(this).inflate(R.layout.layout_register,null);
        EditText edt_name = (EditText)itemview.findViewById(R.id.edt_name);
        EditText edt_phone = (EditText) itemview.findViewById(R.id.edt_phone);

        //Set data
        edt_phone.setText(user.getPhoneNumber());
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }})
                .setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (TextUtils.isEmpty(edt_name.getText().toString()))
                        {
                            Toast.makeText(MainActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        ServerUserModel serverUserModel = new ServerUserModel();
                        serverUserModel.setUid(user.getUid());
                        serverUserModel.setName(edt_name.getText().toString());
                        serverUserModel.setPhone(edt_phone.getText().toString());
                        serverUserModel.setActive(true); // Default failed, we must active user by manual in firebase

                        dialog.show();;

                        serverRef.child(serverUserModel.getUid())
                                .setValue(serverUserModel)
                                .addOnFailureListener(e -> {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }).addOnCompleteListener(task -> {
                                    dialog.dismiss();
                                    Toast.makeText(MainActivity.this, "Congratulations ! Register success! Admin will check and activate your account soon!", Toast.LENGTH_SHORT).show();
                                    //goToHomeActivity(serverUserModel);
                                });


                    }
                });

        builder.setView(itemview);

        androidx.appcompat.app.AlertDialog registerDialog = builder.create();
        registerDialog.show();
    }

    private void goToHomeActivity(ServerUserModel serverUserModel) {

        dialog.dismiss();
        Common.currentServerUser = serverUserModel;
        Intent intent = new Intent(this,HomeActivity.class);
        intent.putExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, getIntent().getBooleanExtra(Common.IS_OPEN_ACTIVITY_NEW_ORDER, false));
        startActivity(intent);
        finish();
    }

    private void phoneLogin() {
        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .build(), APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK)
            {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else 
            {
                Toast.makeText(this, "Oo Oo! Failed To Sing In!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}