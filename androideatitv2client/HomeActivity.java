package com.ds.androideatitv2client;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Toast;
import android.app.AlertDialog;
import android.view.LayoutInflater;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import java.util.HashMap;
import java.util.Map;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import com.andremion.counterfab.CounterFab;
import com.ds.androideatitv2client.Common.Common;
import com.ds.androideatitv2client.Database.CartDataSource;
import com.ds.androideatitv2client.Database.CartDatabase;
import com.ds.androideatitv2client.Database.LocalCartDataSource;
import com.ds.androideatitv2client.EventBus.CategoryClick;
import com.ds.androideatitv2client.EventBus.CounterCartEvent;
import com.ds.androideatitv2client.EventBus.FoodItemClick;
import com.ds.androideatitv2client.EventBus.HideFABCart;
import com.ds.androideatitv2client.EventBus.PopularCategoryClick;
import com.ds.androideatitv2client.EventBus.BestDealItemClick;
import com.ds.androideatitv2client.EventBus.MenuItemBack;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;
import com.ds.androideatitv2client.Model.FoodModel;



import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import android.widget.EditText;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;

import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.ds.androideatitv2client.Common.Common;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import android.text.TextUtils;


import com.google.android.gms.common.api.Status;

import com.ds.androideatitv2client.Model.CategoryModel;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CartDataSource cartDataSource;
    private AppBarConfiguration mAppBarConfiguration;
    private DrawerLayout drawer;
    private NavController navController;

    android.app.AlertDialog dialog;

    int menuClickId = -1;

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    @BindView(R.id.fab)
    CounterFab fab;
    @Override
    protected void onResume()
    {
        super.onResume();
        countCartItem();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initPlaceClient();

        dialog = new SpotsDialog.Builder().setContext(this).setCancelable(false).build();

        ButterKnife.bind(this);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(this).cartDAO());



        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navController.navigate(R.id.nav_cart);

            }
        });
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_menu, R.id.nav_food_list, R.id.nav_view_orders, R.id.nav_food_detail, R.id.nav_cart)
                .setDrawerLayout(drawer)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront(); // Fixed


        View headerView = navigationView.getHeaderView(0);
        TextView txt_user = (TextView) headerView.findViewById(R.id.txt_user);
        Common.setSpanString("Hello, ", Common.currentUser.getName(), txt_user);

        countCartItem();


    }

    private void initPlaceClient() {
        Places.initialize(this, getString(R.string.google_maps_key));
        placesClient = Places.createClient(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void showUpdateInfodialog() {
        androidx.appcompat.app.AlertDialog.Builder builder= new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Update Info");
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
                Toast.makeText(HomeActivity.this, ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });


        //set data
        edt_name.setText(Common.currentUser.getName());
        txt_address_detail.setText(Common.currentUser.getAddress());
        edt_phone.setText(Common.currentUser.getPhone());


        builder.setNegativeButton("CANCEL", (dialogInterface, which) -> {
            dialogInterface.dismiss();
        });

        builder.setPositiveButton("UPDATE", (dialogInterface, which) -> {
            if(placeSelected != null)
            {
                if(TextUtils.isEmpty(edt_name.getText().toString()))
                {
                    Toast.makeText(HomeActivity.this, "Please enter your Name", Toast.LENGTH_SHORT).show();
                    return;
                }

                Map<String, Object> update_data = new HashMap<>();
                update_data.put("name", edt_name.getText().toString());
                update_data.put("address", txt_address_detail.getText().toString());
                update_data.put("lat", placeSelected.getLatLng().latitude);
                update_data.put("lng", placeSelected.getLatLng().longitude);


            FirebaseDatabase.getInstance()
                    .getReference(Common.USER_REFERENCES)
                    .child(Common.currentUser.getUid())
                    .updateChildren(update_data)
                    .addOnFailureListener(e -> {

                        dialogInterface.dismiss();
                        Toast.makeText(HomeActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    })
                    .addOnSuccessListener( aVoid -> {
                            dialogInterface.dismiss();
                            Toast.makeText(HomeActivity.this, "Update Info succesed", Toast.LENGTH_SHORT).show();
                            Common.currentUser.setName(update_data.get("name").toString());
                            Common.currentUser.setAddress(update_data.get("address").toString());
                            Common.currentUser.setLat(Double.parseDouble(update_data.get("lat").toString()));
                            Common.currentUser.setLng(Double.parseDouble(update_data.get("lng").toString()));

                    });


            }
            else
            {
                Toast.makeText(HomeActivity.this, "Please select address", Toast.LENGTH_SHORT).show();

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

    private void signOut() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Are you sure you want to sing out?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        Common.selectedFood = null;
                        Common.categorySelected = null;
                        Common.currentUser = null;
                        FirebaseAuth.getInstance().signOut();

                        Intent intent = new Intent( HomeActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    }
        });
        AlertDialog dialog = builder.create();
        dialog.show();


    }

    //event bus

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);

    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCategorySelected(CategoryClick event)
    {
        if (event.isSuccess())
        {
            navController.navigate(R.id.nav_food_list);
            //Toast.makeText(this, "You Clicked to:"+event.getCategoryModel().getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        menuItem.setCheckable(true);
        drawer.closeDrawers();
        switch (menuItem.getItemId()){
            case R.id.nav_home:
                if(menuItem.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_home);
                break;
            case R.id.nav_menu:
                if(menuItem.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_menu);
                break;
            case R.id.nav_cart:
                if(menuItem.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_cart);
                break;
            case R.id.nav_view_orders:
                if(menuItem.getItemId() != menuClickId)
                    navController.navigate(R.id.nav_view_orders);
                break;
            case R.id.nav_update_info:
                showUpdateInfodialog();
                break;
            case R.id.nav_sign_out:
                signOut();
                break;
        }
        menuClickId = menuItem.getItemId();
        return true;
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onBestDealItemClick(BestDealItemClick event)
    {
        if (event.getBestDealModel() != null)
        {
            dialog.show();

            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.getBestDealModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot){

                            if(dataSnapshot.exists())
                                {
                                    Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                    Common.categorySelected.setMenu_id(dataSnapshot.getKey());
                                    //Load Food
                                    com.google.firebase.database.FirebaseDatabase.getInstance()
                                    .getReference("Category")
                                    .child(event.getBestDealModel().getMenu_id())
                                    .child("foods")
                                    .orderByChild("id")
                                    .equalTo(event.getBestDealModel().getFood_id())
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                                {
                                                    for(DataSnapshot itemSnapShot : dataSnapshot.getChildren())
                                                        {
                                                            Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                            Common.selectedFood.setKey(itemSnapShot.getKey());
                                                        }

                                                    navController.navigate(R.id.nav_food_detail );
                                                }
                                            else
                                                {
                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                                                }
                                            dialog.dismiss();

                                        }
                                        @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError){
                                            dialog.dismiss();
                                            Toast.makeText(HomeActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                                            }

                                    });
                                }
                            else
                                {
                                    dialog.dismiss();
                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();

                                }


                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError){

                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onPopularItemClick(PopularCategoryClick event)
    {
        if (event.getPopularCategoryModel() != null)
        {
            dialog.show();

            com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("Category")
                    .child(event.getPopularCategoryModel().getMenu_id())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot){

                            if(dataSnapshot.exists())
                                {
                                    Common.categorySelected = dataSnapshot.getValue(CategoryModel.class);
                                    Common.categorySelected.setMenu_id(dataSnapshot.getKey());

                                    //Load Food
                                    com.google.firebase.database.FirebaseDatabase.getInstance()
                                    .getReference("Category")
                                    .child(event.getPopularCategoryModel().getMenu_id())
                                    .child("foods")
                                    .orderByChild("id")
                                    .equalTo(event.getPopularCategoryModel().getFood_id())
                                    .limitToLast(1)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                                {
                                                    for(DataSnapshot itemSnapShot : dataSnapshot.getChildren())
                                                        {
                                                            Common.selectedFood = itemSnapShot.getValue(FoodModel.class);
                                                            Common.selectedFood.setKey(itemSnapShot.getKey());
                                                        }

                                                    navController.navigate(R.id.nav_food_detail );
                                                }
                                            else
                                                {
                                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();
                                                }
                                            dialog.dismiss();

                                        }
                                        @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError){
                                            dialog.dismiss();
                                            Toast.makeText(HomeActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();

                                            }

                                    });
                                }
                            else
                                {
                                    dialog.dismiss();
                                    Toast.makeText(HomeActivity.this, "Item doesn't exists!", Toast.LENGTH_SHORT).show();

                                }


                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError){

                            dialog.dismiss();
                            Toast.makeText(HomeActivity.this, ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onFoodItemClick(FoodItemClick event)
    {
        if (event.isSuccess())
        {
            navController.navigate(R.id.nav_food_detail);
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onHideFABCart(HideFABCart event)
    {
        if (event.isHidden())
        {
            fab.hide();
        }
        else
            fab.show();

    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onCartCounter(CounterCartEvent event)
    {
        if (event.isSuccess())
        {
            countCartItem();
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void countCartAgain(CounterCartEvent event)
    {
        if(event.isSuccess())
        {
            countCartItem();
        }
    }
    private void countCartItem() {
        cartDataSource.countItemInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer integer) {
                        fab.setCount(integer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty"))
                        {
                            Toast.makeText(HomeActivity.this, "[COUNT CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        else
                            fab.setCount(0);
                    }
                });
    }


    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMenuItemBack(MenuItemBack event)
    {
        menuClickId = -1;
        if(getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
    }

}