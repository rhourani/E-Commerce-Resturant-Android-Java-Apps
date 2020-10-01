package com.ds.androideatitv2client.ui.cart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.ds.androideatitv2client.Adapter.MyCartAdapter;
import com.ds.androideatitv2client.Callback.ILoadTimeFromFirebaseListener;
import com.ds.androideatitv2client.Common.Common;
import com.ds.androideatitv2client.Common.MySwipeHelper;
import com.ds.androideatitv2client.Database.CartDataSource;
import com.ds.androideatitv2client.Database.CartDatabase;
import com.ds.androideatitv2client.Database.CartItem;
import com.ds.androideatitv2client.Database.LocalCartDataSource;
import com.ds.androideatitv2client.EventBus.CounterCartEvent;
import com.ds.androideatitv2client.EventBus.HideFABCart;
import com.ds.androideatitv2client.EventBus.MenuItemBack;
import com.ds.androideatitv2client.EventBus.UpdateItemInCart;
import com.ds.androideatitv2client.HomeActivity;
import com.ds.androideatitv2client.Model.FCMSendData;
import com.ds.androideatitv2client.Model.OrderModel;
import com.ds.androideatitv2client.R;
import com.ds.androideatitv2client.Remote.ICloudFunctions;
import com.ds.androideatitv2client.Remote.IFCMService;
import com.ds.androideatitv2client.Remote.RetrofitCloudClient;
import com.ds.androideatitv2client.Remote.RetrofitFCMClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CartFragment  extends Fragment implements ILoadTimeFromFirebaseListener {

    private Place placeSelected;
    private AutocompleteSupportFragment places_fragment;
    private PlacesClient placesClient;
    private List<Place.Field> placeFields = Arrays.asList(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG);

    private static final int REQUEST_BRAINTREE_CODE = 7777;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private Parcelable recyclerViewState;
    private CartDataSource cartDataSource;

    private String address, comment;

    ICloudFunctions cloudFunctions;
    IFCMService ifcmService;
    ILoadTimeFromFirebaseListener listener;


    LocationRequest locationRequest;
    LocationCallback locationCallback;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location currentLocation;


    @BindView(R.id.recycler_cart)
    RecyclerView recycler_cart;
    @BindView(R.id.txt_total_price)
    TextView txt_total_price;
    @BindView(R.id.txt_empty_cart)
    TextView txt_empty_cart;
    @BindView(R.id.group_place_holder)
    CardView group_place_holder;


    @OnClick(R.id.btn_place_order)
    void onPlaceOrderClick(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("One more step");

        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_place_order, null);



        EditText edt_comment = (EditText) view.findViewById(R.id.edt_comment);
        TextView txt_address = (TextView) view.findViewById(R.id.txt_address_detail);

        RadioButton rdi_home = (RadioButton) view.findViewById(R.id.rdi_home_address);
        RadioButton rdi_other_address = (RadioButton) view.findViewById(R.id.rdi_other_address);
        RadioButton rdi_ship_to_this = (RadioButton) view.findViewById(R.id.rdi_ship_this_address);
        RadioButton rdi_cod = (RadioButton) view.findViewById(R.id.rdi_cod);
        RadioButton rdi_braintree = (RadioButton) view.findViewById(R.id.rdi_braintree);


        places_fragment = (AutocompleteSupportFragment)getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.places_autocomplete_fragment);
        places_fragment.setPlaceFields(placeFields);
        places_fragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {

                placeSelected = place;
                txt_address.setText(place.getAddress());

            }

            @Override
            public void onError(@NonNull Status status) {
                Toast.makeText(getContext(), ""+status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Data
        txt_address.setText(Common.currentUser.getAddress()); // By default this is the use's home address

        //Event
        rdi_home.setOnCheckedChangeListener((compoundButton, b) -> {
          if(b)
          {
              txt_address.setText(Common.currentUser.getAddress());
              txt_address.setVisibility(View.VISIBLE);
              places_fragment.setHint(Common.currentUser.getAddress());

          }
        });
        rdi_other_address.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
            {

                txt_address.setVisibility(View.VISIBLE);

            }
        });
        rdi_ship_to_this.setOnCheckedChangeListener((compoundButton, b) -> {
            if(b)
            {
                fusedLocationProviderClient.getLastLocation()
                        .addOnFailureListener(e ->  {

                                Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                txt_address.setVisibility(View.GONE);

                        })
                        .addOnCompleteListener( task -> {
                            String coordinates = new StringBuilder()
                                    .append(task.getResult().getLatitude())
                                    .append("/")
                                    .append(task.getResult().getLatitude()).toString();

                            Single<String> singleAddress = Single.just(getAddressFromLating(task.getResult().getLatitude(),
                                    task.getResult().getLongitude()));

                            Disposable disposable = singleAddress.subscribeWith(new DisposableSingleObserver<String>() {
                                @Override
                                public void onSuccess(String s) {
                                    txt_address.setText(s);
                                    txt_address.setVisibility(View.VISIBLE);
                                    places_fragment.setHint(s);
                                }

                                @Override
                                public void onError(Throwable e) {
                                    txt_address.setText(e.getMessage());
                                    txt_address.setVisibility(View.VISIBLE);
                                }
                            });




                            });
            }
        });

        builder.setView(view);
        builder.setNegativeButton("NO", (dialogInterface, i) -> {
            dialogInterface.dismiss();
        }).setPositiveButton("YES", (dialogInterface, i) -> {
            //Toast.makeText(getContext(), "Implemente late!", Toast.LENGTH_SHORT).show();
            if(rdi_cod.isChecked())
                paymentCOD(txt_address.getText().toString(),edt_comment.getText().toString());
            else if (rdi_braintree.isChecked())
            {
                address = txt_address.getText().toString();
                comment = edt_comment.getText().toString();
                if(!TextUtils.isEmpty(Common.currentToken))
                {
                    DropInRequest dropInRequest = new DropInRequest().clientToken(Common.currentToken);
                    startActivityForResult(dropInRequest.getIntent(getContext()), REQUEST_BRAINTREE_CODE);
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dialog1 -> {
            //this fixes dublicate order crash
            if (places_fragment != null)
            {
                getActivity().getSupportFragmentManager()
                        .beginTransaction().remove(places_fragment)
                        .commit();
            }
        });
        dialog.show();

    }

    private void paymentCOD(String address, String comment) {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    //When we have all items we will get the total price as well here
                    cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleObserver<Double>() {
                                @Override
                                public void onSubscribe(Disposable d) {

                                }

                                @Override
                                public void onSuccess(Double totalPrice) {
                                    double finalPrice = totalPrice; // we will modify this formula for discount later on
                                    OrderModel orderModel = new OrderModel();
                                    orderModel.setUserId(Common.currentUser.getUid());
                                    orderModel.setUserName(Common.currentUser.getName());
                                    orderModel.setUserPhone(Common.currentUser.getPhone());
                                    orderModel.setShippingAddress(address);
                                    orderModel.setComment(comment);

                                    if(currentLocation != null)
                                    {
                                        orderModel.setLat(currentLocation.getLatitude());
                                        orderModel.setLng(currentLocation.getLongitude());
                                    }
                                    else

                                    {
                                        orderModel.setLat(-0.1f);
                                        orderModel.setLng(-0.1f);
                                    }

                                    orderModel.setCartItemList(cartItems);
                                    orderModel.setTotalPayment(totalPrice);
                                    orderModel.setDiscount(0); // Modify with discount later
                                    orderModel.setFinalPayment(finalPrice);
                                    orderModel.setCod(true);
                                    orderModel.setTransactionId("Cash On Delivery");

                                    //Submit this order to firebase
                                    //writeOrderToFirebase(order);
                                    syncLocalTimeWithGlobaltime(orderModel);

                                }

                                @Override
                                public void onError(Throwable e) {
                                    if (!e.getMessage().contains("Query returned empty result set"))
                                        Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });

                }, throwable -> {
                    Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }));

    }

    //using online payment credit or depit ..
    private void writeOrderToFirebase(OrderModel orderModel) {
        FirebaseDatabase.getInstance()
                .getReference(Common.ORDER_REF)
                .child(Common.creatOrderNumber()) // Create order number with only digit
                .setValue(orderModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                }).addOnCompleteListener(task -> {
                    //write success
            cartDataSource.cleanCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {

                            //This is for the notification
                            Map<String,String> notiData = new HashMap<>();
                            notiData.put(Common.NOTI_TITLE, "New Order");
                            notiData.put(Common.NOTI_CONTENT, "You have new order from "+Common.currentUser.getPhone());

                            FCMSendData sendData = new FCMSendData(Common.creatTopicOrder(), notiData);

                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(fcmResponse -> {

                                //Order placed and notification sent
                                Toast.makeText(getContext(), "Order placed Successfully!", Toast.LENGTH_SHORT).show();
                                EventBus.getDefault().postSticky(new CounterCartEvent(true));

                            }, throwable -> {
                                Toast.makeText(getContext(), "Order was sent but failed to send notification!", Toast.LENGTH_SHORT).show();
                                EventBus.getDefault().postSticky(new CounterCartEvent(true));
                            }));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private String getAddressFromLating(double latitude, double longitude)  {
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        String result ="";
        try{
            List<Address> addressList = geocoder.getFromLocation(latitude,longitude,1);
            if(addressList != null && addressList.size() > 0 )
            {
                Address address = addressList.get(0); // always get first item
                StringBuilder sb = new StringBuilder(address.getAddressLine(0));
                result = sb.toString();
            }
            else {
                result = "Address not found";
            }


        } catch (IOException e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }


    private MyCartAdapter adapter;

    private Unbinder unbinder;

    private CartViewModel cartViewModel;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        cartViewModel =
                ViewModelProviders.of(this).get(CartViewModel.class);
        View root = inflater.inflate(R.layout.fragment_cart, container, false);

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);
        cloudFunctions = RetrofitCloudClient.getInstance().create(ICloudFunctions.class);
        listener = this;

        cartViewModel.initCartDataSource(getContext());
        cartViewModel.getMutableLiveDataCartItem().observe(this, new Observer<List<CartItem>>() {
            @Override
            public void onChanged(List<CartItem> cartItems) {
                if(cartItems == null || cartItems.isEmpty())
                {
                    recycler_cart.setVisibility(View.GONE);
                    group_place_holder.setVisibility(View.GONE);
                    txt_empty_cart.setVisibility(View.VISIBLE);

                }
                else
                {
                    recycler_cart.setVisibility(View.VISIBLE);
                    group_place_holder.setVisibility(View.VISIBLE);
                    txt_empty_cart.setVisibility(View.GONE);

                    adapter = new MyCartAdapter(getContext(),cartItems);
                    recycler_cart.setAdapter(adapter);
                }
            }
        });

        unbinder = ButterKnife.bind(this, root);
        initView();
        initLocation();
        return root;
    }

    private void initLocation() {
        buildLocationRequest();
        buildLocationCallback();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void buildLocationCallback() {
        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);
                
    }

    private void initView() {
        
        initPlaceClient();
        
        setHasOptionsMenu(true);

        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(getContext()).cartDAO());

        EventBus.getDefault().postSticky(new HideFABCart(true));

        recycler_cart.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_cart.setLayoutManager(layoutManager);
        recycler_cart.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_cart, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Delete", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            //Toast.makeText(getContext(), "Delete item Click", Toast.LENGTH_SHORT).show();
                            CartItem cartItem = adapter.getItemAtPosition(pos);
                            cartDataSource.deleteCartItem(cartItem)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(new SingleObserver<Integer>() {
                                        @Override
                                        public void onSubscribe(Disposable d) {

                                        }

                                        @Override
                                        public void onSuccess(Integer integer) {
                                            adapter.notifyItemRemoved(pos);
                                            sumAllItemsInCart(); // update total price
                                            EventBus.getDefault().postSticky(new CounterCartEvent(true));// Update FAB
                                            Toast.makeText(getContext(), "Delete item from Cart successful!", Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onError(Throwable e) {
                                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                }));
            }
        };

        sumAllItemsInCart();
    }

    private void initPlaceClient() {
        Places.initialize(getContext(), getString(R.string.google_maps_key));
        placesClient = Places.createClient(getContext());
    }

    private void sumAllItemsInCart() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double aDouble) {
                        txt_total_price.setText(new StringBuilder("Total: $").append(aDouble));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if(!e.getMessage().contains("Query returned empty"))
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
       // menu.findItem(R.id.action_settings).setVisible(false); // hide setting option when you are in cart
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.cart_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_clear_cart)
        {
            cartDataSource.cleanCart(Common.currentUser.getUid())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            Toast.makeText(getContext(), "Clear Cart Success", Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().postSticky(new CounterCartEvent(true));
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop () {
        EventBus.getDefault().postSticky(new HideFABCart(false));
        EventBus.getDefault().postSticky(new CounterCartEvent(false));
        cartViewModel.onStop();
        if(EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(fusedLocationProviderClient != null)
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateItemInCartEvent(UpdateItemInCart event)
    {
        if(event.getCartItem() != null)
        {
            //first, save state of Recycler view
            recyclerViewState = recycler_cart.getLayoutManager().onSaveInstanceState();
            cartDataSource.updateCartItems(event.getCartItem())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<Integer>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onSuccess(Integer integer) {
                            calculateTotalPrice();
                            recycler_cart.getLayoutManager().onRestoreInstanceState(recyclerViewState);//fix error refresh recycler view after update
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(getContext(), "[UPDATE CART]"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

        }
     }

    private void calculateTotalPrice() {
        cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Double>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Double price) {
                        txt_total_price.setText(new StringBuilder("Total: ")
                        .append(Common.formatPrice(price)));
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (!e.getMessage().contains("Query returned empty result set"))
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_BRAINTREE_CODE)
        {
            if(resultCode == Activity.RESULT_OK)
            {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                PaymentMethodNonce nonce = result.getPaymentMethodNonce();

                //Calculate sum in cart
                cartDataSource.sumPriceInCart(Common.currentUser.getUid())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new SingleObserver<Double>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(Double totalPrice) {
                                //Get all items within the cart to create an order
                                compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(cartItems -> {

                                    //Submit payment
                                    Map<String,String> headers = new HashMap<>();
                                    headers.put("Authorization", Common.buildToken(Common.authorizeKey));
                                    compositeDisposable.add(cloudFunctions.submitPayment(
                                            headers,
                                            totalPrice,
                                            nonce.getNonce())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(braintreeTransaction -> {
                                                if(braintreeTransaction.isSuccess())
                                                {
                                                    double finalPrice = totalPrice; // we will modify this formula for discount later on
                                                    OrderModel orderModel = new OrderModel();
                                                    orderModel.setUserId(Common.currentUser.getUid());
                                                    orderModel.setUserName(Common.currentUser.getName());
                                                    orderModel.setUserPhone(Common.currentUser.getPhone());
                                                    orderModel.setShippingAddress(address);
                                                    orderModel.setComment(comment);

                                                    if(currentLocation != null)
                                                    {
                                                        orderModel.setLat(currentLocation.getLatitude());
                                                        orderModel.setLng(currentLocation.getLongitude());
                                                    }
                                                    else

                                                    {
                                                        orderModel.setLat(-0.1f);
                                                        orderModel.setLng(-0.1f);
                                                    }

                                                    orderModel.setCartItemList(cartItems);
                                                    orderModel.setTotalPayment(totalPrice);
                                                    orderModel.setDiscount(0); // Modify with discount later
                                                    orderModel.setFinalPayment(finalPrice);
                                                    orderModel.setCod(false);
                                                    orderModel.setTransactionId(braintreeTransaction.getTransaction().getId());

                                                    //Submit this order to firebase
                                                    syncLocalTimeWithGlobaltime(orderModel);
                                                }

                                            }, throwable -> {
                                                Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();

                                            })
                                    );

                                }, throwable -> {
                                    Toast.makeText(getContext(), ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();

                                }));
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (!e.getMessage().contains("Query returned empty result set"))
                                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void syncLocalTimeWithGlobaltime(OrderModel orderModel) {
        final DatabaseReference offsetRef = FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset");
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long offset = dataSnapshot.getValue(Long.class);
                long estimatedServerTimeMs = System.currentTimeMillis()+offset; // offset is the missing time betwee the global and local time of us
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
                Date resultDate = new Date(estimatedServerTimeMs);
                Log.d("TEST_DATE", ""+sdf.format(resultDate));

                listener.onLoadTimeSuccess(orderModel, estimatedServerTimeMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                listener.onLoadTimeFailed(databaseError.getMessage());
            }
        });
    }

    @Override
    public void onLoadTimeSuccess(OrderModel orderModel, long estimateTimeInMs) {
        orderModel.setCreateDate(estimateTimeInMs);
        orderModel.setOrderStatus(0);
        writeOrderToFirebase(orderModel);
    }

    @Override
    public void onLoadTimeFailed(String message) {
        Toast.makeText(getContext(), ""+message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new MenuItemBack());
        super.onDestroy();
    }
}
