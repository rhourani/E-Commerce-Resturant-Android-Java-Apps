package com.ds.androideatitv2server.ui.order;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.androideatitv2server.Adapter.MyOrderAdapter;
import com.ds.androideatitv2server.Adapter.MyShipperSelectionAdapter;
import com.ds.androideatitv2server.Callback.IShipperLoadCallbackListener;
import com.ds.androideatitv2server.Common.BottomSheetOrderFragment;
import com.ds.androideatitv2server.Common.Common;
import com.ds.androideatitv2server.Common.MySwipeHelper;
import com.ds.androideatitv2server.EventBus.ChangeMenuClick;
import com.ds.androideatitv2server.EventBus.LoadOrderEvent;
import com.ds.androideatitv2server.Model.FCMSendData;
import com.ds.androideatitv2server.Model.OrderModel;
import com.ds.androideatitv2server.Model.ShipperModel;
import com.ds.androideatitv2server.Model.ShippingOrderModel;
import com.ds.androideatitv2server.Model.TokenModel;
import com.ds.androideatitv2server.R;
import com.ds.androideatitv2server.Remote.IFCMService;
import com.ds.androideatitv2server.Remote.RetrofitFCMClient;
import com.ds.androideatitv2server.TrackingOrderActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class OrderFragment extends Fragment implements IShipperLoadCallbackListener {

    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;
    @BindView(R.id.txt_order_filter)
    TextView txt_order_filter;

    RecyclerView recycler_shipper;
    private Context context;
    private Unbinder unbinder;
    LayoutAnimationController layoutAnimationController;
    MyOrderAdapter adapter;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private IFCMService ifcmService;

    private IShipperLoadCallbackListener shipperLoadCallbackListener;

    private OrderViewModel orderViewModel;
    private MyShipperSelectionAdapter myShipperSelectedAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        orderViewModel =
                ViewModelProviders.of(this).get(OrderViewModel.class);
        View root = inflater.inflate(R.layout.fragment_order, container, false);

        unbinder = ButterKnife.bind(this, root);
        initView();
        orderViewModel.getMessageError().observe(this, s -> {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        });
        orderViewModel.getOrderMoListMutableLiveData().observe(this, orderModels -> {
            if(orderModels != null)
            {
                adapter = new MyOrderAdapter(getContext(), orderModels);
                recycler_order.setAdapter(adapter);
                recycler_order.setLayoutAnimation(layoutAnimationController);

                updateTextCounter();
            }
        });
        return root;
    }

    private void initView() {

        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        shipperLoadCallbackListener = this;

        setHasOptionsMenu(true);

        recycler_order.setHasFixedSize(true);
        recycler_order.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);

        // Get Size
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int width = displayMetrics.widthPixels;


        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(),recycler_order, width/6) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {

                try {
                    buf.add(new MyButton(getContext(), "Directions", 30, 0, Color.parseColor("#9b0000"),
                            pos -> {

                                try {
                                    OrderModel orderModel = ((MyOrderAdapter) recycler_order.getAdapter())
                                            .getItemAtPosition(pos);


                                    if (orderModel.getOrderStatus() == 1)
                                    {
                                        try {
                                            Common.currentOrderSelected = orderModel;
                                            startActivity(new Intent(getContext(),TrackingOrderActivity.class));
                                        }
                                        catch (Exception e)
                                        {
                                            Toast.makeText(getContext(), "Something went wrong! "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }

                                    }
                                    else
                                    {
                                        Toast.makeText(getContext(), new StringBuilder("Order is ")
                                                .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                                                .append(". So you cant track direction") , Toast.LENGTH_SHORT).show();
                                    }
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(context, "Context wrong is : "+e.getMessage(), Toast.LENGTH_SHORT).show();

                                    Toast.makeText(getContext(), "wrong is : "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                }



                            }));
                }
                catch (Exception e)
                {
                    Toast.makeText(context, "Something went wrong! "+e.getMessage(), Toast.LENGTH_SHORT).show();
                }


                buf.add(new MyButton(getContext(), "Call", 30, 0, Color.parseColor("#560027"),
                        pos -> {

                            Dexter.withActivity(getActivity())
                                    .withPermission(Manifest.permission.CALL_PHONE)
                                    .withListener(new PermissionListener() {
                                        @Override
                                        public void onPermissionGranted(PermissionGrantedResponse response) {
                                            OrderModel orderModel = adapter.getItemAtPosition(pos);
                                            Intent intent = new Intent();
                                            intent.setData(Uri.parse(new StringBuilder("tel: ")
                                            .append(orderModel.getUserPhone()).toString()));
                                            startActivity(intent);
                                        }

                                        @Override
                                        public void onPermissionDenied(PermissionDeniedResponse response) {
                                            Toast.makeText(getContext(), "You must accept "+response.getPermissionName(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                                        }
                                    }).check();

                        }));

                buf.add(new MyButton(getContext(), "Remove", 30, 0, Color.parseColor("#12005e"),
                        pos -> {

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle("DELETE")
                                    .setMessage("Do you want to delete this order?")
                                    .setNegativeButton("CANCEL", ((dialogInterface, which) -> dialogInterface.dismiss()))
                                    .setPositiveButton("DELETE", ((dialogInterface, which) -> {
                                        OrderModel orderModel = adapter.getItemAtPosition(pos); // Get item in adapter
                                        FirebaseDatabase.getInstance()
                                                .getReference(Common.ORDER_REF)
                                                .child(orderModel.getKey())
                                                .removeValue()
                                                .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                .addOnSuccessListener(aVoid -> {
                                                       adapter.removeItem(pos);
                                                       adapter.notifyItemRemoved(pos);
                                                       updateTextCounter();
                                                       dialogInterface.dismiss();
                                                    Toast.makeText(getContext(), "Order has been deleted!", Toast.LENGTH_SHORT).show();
                                                });
                                    }));
                           AlertDialog deleteDialog = builder.create();
                           deleteDialog.show();
                            Button negativeButton = deleteDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                            negativeButton.setTextColor(Color.GREEN);

                            Button positiveButton = deleteDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            positiveButton.setTextColor(Color.RED);

                        }));

                buf.add(new MyButton(getContext(), "Edit", 30, 0, Color.parseColor("#336699"),
                        pos -> {
                            showEditDialog(adapter.getItemAtPosition(pos), pos);
                        }));
            }
        };


    }

    private void showEditDialog(OrderModel orderModel, int pos) {
        View layout_dialog;
        AlertDialog.Builder builder;
        if (orderModel.getOrderStatus() == 0)
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipping, null);

            recycler_shipper = layout_dialog.findViewById(R.id.recycler_shippers);

            builder = new AlertDialog.Builder(getContext(),android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
            .setView(layout_dialog);
        }
        else if(orderModel.getOrderStatus() == -1) // Cancelled
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_cancelled, null);
            builder = new AlertDialog.Builder(getContext())
            .setView(layout_dialog);
        }
        else // Shipped
        {
            layout_dialog = LayoutInflater.from(getContext())
                    .inflate(R.layout.layout_dialog_shipped, null);
            builder = new AlertDialog.Builder(getContext())
            .setView(layout_dialog);
        }

        //View
        Button btn_ok = (Button) layout_dialog.findViewById(R.id.btn_ok);
        Button btn_cancel = (Button) layout_dialog.findViewById(R.id.btn_cancel);

        RadioButton rdi_shipping = (RadioButton)layout_dialog.findViewById(R.id.rdi_shipping);
        RadioButton rdi_shipped = (RadioButton)layout_dialog.findViewById(R.id.rdi_shipped);
        RadioButton rdi_cancelled = (RadioButton)layout_dialog.findViewById(R.id.rdi_cancelled);
        RadioButton rdi_delete = (RadioButton)layout_dialog.findViewById(R.id.rdi_delete);
        RadioButton rdi_restore_placed = (RadioButton)layout_dialog.findViewById(R.id.rdi_restore_placed);

        TextView txt_status = (TextView) layout_dialog.findViewById(R.id.txt_status);

        //Set data
        txt_status.setText(new StringBuilder("Order Status (")
        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
        .append(")"));

        //Create dialog
        AlertDialog dialog = builder.create();

        if (orderModel.getOrderStatus() == 0) //shipping
            loadShipperList(pos, orderModel, dialog, btn_ok, btn_cancel,
                    rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
        else
            showDialog(pos, orderModel, dialog, btn_ok, btn_cancel,
                rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);


    }

    private void loadShipperList(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        List<ShipperModel> tempList = new ArrayList<>();
        DatabaseReference shipperRef = FirebaseDatabase.getInstance().getReference(Common.SHIPPER);
        Query shipperActive = shipperRef.orderByChild("active").equalTo(true); // load active shippers
        shipperActive.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot shipperSnapshot:dataSnapshot.getChildren())
                {
                    ShipperModel shipperModel = shipperSnapshot.getValue(ShipperModel.class);
                    shipperModel.setKey(shipperSnapshot.getKey());
                    tempList.add(shipperModel);
                }
                shipperLoadCallbackListener.onShipperLoadSuccess(pos, orderModel, tempList,
                        dialog,
                        btn_ok, btn_cancel,
                        rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete,rdi_restore_placed);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                shipperLoadCallbackListener.onShipperLoadFailed(databaseError.getMessage());
            }
        });
    }

    private void showDialog(int pos, OrderModel orderModel, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        dialog.show();
        //Custom Dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_cancel.setOnClickListener(view -> dialog.dismiss());
        btn_ok.setOnClickListener(view -> {

            if(rdi_cancelled != null && rdi_cancelled.isChecked())
            {
                updateOrder(pos, orderModel, -1);
                dialog.dismiss();
            }
            else if(rdi_shipping != null && rdi_shipping.isChecked()) // Shipping
            {

                ShipperModel shipperModel = null;
                if (myShipperSelectedAdapter != null)
                {
                    shipperModel = myShipperSelectedAdapter.getSelectedShipper();
                    if (shipperModel != null)
                    {
                        createShippingOrder(pos, shipperModel, orderModel, dialog);
                    }
                    else
                        Toast.makeText(getContext(), "Please select a Shipper!", Toast.LENGTH_SHORT).show();
                }
            }

            else if(rdi_shipped != null && rdi_shipped.isChecked())
            {
                updateOrder(pos, orderModel, 2);
                dialog.dismiss();
            }
            else if(rdi_restore_placed != null && rdi_restore_placed.isChecked())
            {
                updateOrder(pos, orderModel, 0);
                dialog.dismiss();
            }
            else if(rdi_delete != null && rdi_delete.isChecked())
            {
                deleteOrder(pos, orderModel);
                dialog.dismiss();
            }
        });

    }

    private void createShippingOrder(int pos, ShipperModel shipperModel, OrderModel orderModel, AlertDialog dialog) {
        ShippingOrderModel shippingOrderModel = new ShippingOrderModel();
        shippingOrderModel.setShipperPhone(shipperModel.getPhone());
        shippingOrderModel.setShipperName(shipperModel.getName());
        shippingOrderModel.setOrderModel(orderModel);
        shippingOrderModel.setStartTrip(false);
        shippingOrderModel.setCurrentLat(-1.0);
        shippingOrderModel.setCurrentLng(-1.0);

        FirebaseDatabase.getInstance()
                .getReference(Common.SHIPPING_ORDER_REF)
                .child(orderModel.getKey())
                .setValue(shippingOrderModel)
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful())
                    {
                        dialog.dismiss();
                        //first of all we take the token of the user to send notificatiın to itr later
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(shipperModel.getKey())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists())
                                        {
                                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "You have new order needs to ship! ");
                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("You have a new order needs ship to ")
                                                    .append(orderModel.getUserPhone()).toString());

                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(),notiData);

                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if (fcmResponse.getSuccess() == 1)
                                                        {
                                                            updateOrder(pos, orderModel, 1);
                                                        }
                                                        else
                                                        {
                                                            Toast.makeText(getContext(), "Failed to send to shipper! Order not updated!", Toast.LENGTH_SHORT).show();
                                                        }

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "Token could not be found!", Toast.LENGTH_SHORT).show();
                                                    }));
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Token Not Found!", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });


    }

    private void deleteOrder(int pos, OrderModel orderModel) {

        if (!TextUtils.isEmpty(orderModel.getKey()))
        {
            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .removeValue()
                    .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            adapter.removeItem(pos);
                            adapter.notifyItemRemoved(pos);
                            updateTextCounter();
                            Toast.makeText(getContext(), "Order has been deleted successfully!", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
        else
        {
            Toast.makeText(getContext(), "Order number must not be null or empty!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateOrder(int pos, OrderModel orderModel, int status)
    {
        if (!TextUtils.isEmpty(orderModel.getKey()))
        {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("orderStatus", status);

            FirebaseDatabase.getInstance()
                    .getReference(Common.ORDER_REF)
                    .child(orderModel.getKey())
                    .updateChildren(updateData)
                    .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                    .addOnSuccessListener(aVoid -> {


                        //Show dialog
                        android.app.AlertDialog dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
                        dialog.show();

                        //first of all we take the token of the user to send notificatiın to itr later
                        FirebaseDatabase.getInstance()
                                .getReference(Common.TOKEN_REF)
                                .child(orderModel.getUserId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.exists())
                                        {
                                            TokenModel tokenModel = dataSnapshot.getValue(TokenModel.class);
                                            Map<String, String> notiData = new HashMap<>();
                                            notiData.put(Common.NOTI_TITLE, "Your order has been updated!");
                                            notiData.put(Common.NOTI_CONTENT, new StringBuilder("Your order ")
                                            .append(orderModel.getKey())
                                            .append(" was updated to ")
                                            .append(Common.convertStatusToText(status)).toString());

                                            FCMSendData sendData = new FCMSendData(tokenModel.getToken(),notiData);

                                            compositeDisposable.add(ifcmService.sendNotification(sendData)
                                                    .subscribeOn(Schedulers.io())
                                                    .observeOn(AndroidSchedulers.mainThread())
                                                    .subscribe(fcmResponse -> {
                                                        dialog.dismiss();
                                                        if (fcmResponse.getSuccess() == 1)
                                                        {
                                                            Toast.makeText(getContext(), "Order Updated successfully!", Toast.LENGTH_SHORT).show();
                                                        }
                                                        else
                                                        {
                                                            Toast.makeText(getContext(), "Order Updated successfully but failed to send notification!", Toast.LENGTH_SHORT).show();
                                                        }

                                                    }, throwable -> {
                                                        dialog.dismiss();
                                                        Toast.makeText(getContext(), "Token could not be found!", Toast.LENGTH_SHORT).show();
                                                    }));
                                        }
                                        else
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(getContext(), "Token Not Found!", Toast.LENGTH_SHORT).show();
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        dialog.dismiss();
                                        Toast.makeText(getContext(), ""+databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });

                        adapter.removeItem(pos);
                        adapter.notifyItemRemoved(pos);
                        updateTextCounter();

                    });
        }
        else
            {
                Toast.makeText(getContext(), "Order number must not be null or empty!", Toast.LENGTH_SHORT).show();
            }
    }

    private void updateTextCounter() {
        txt_order_filter.setText(new StringBuilder("Order (")
                .append(adapter.getItemCount())
                .append(")"));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.order_filter_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter)
        {
            BottomSheetOrderFragment bottomSheetOrderFragment = BottomSheetOrderFragment.getInstance();
            bottomSheetOrderFragment.show(getActivity().getSupportFragmentManager(), "OrderFilter");
            return true;
        }
        else
        {
         return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        if (EventBus.getDefault().hasSubscriberForEvent(LoadOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(LoadOrderEvent.class);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        compositeDisposable.clear();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().postSticky(new ChangeMenuClick(true));
        super.onDestroy();
    }

    @Subscribe(sticky = true, threadMode =  ThreadMode.MAIN)
    public void onLoadOrderEvent(LoadOrderEvent event)
    {
        orderViewModel.loadOrderByStatus(event.getStatus());
    }

    @Override
    public void onShipperLoadSuccess(List<ShipperModel> shipperModelList) {
            //Do nothing
    }

    @Override
    public void onShipperLoadSuccess(int pos, OrderModel orderModel, List<ShipperModel> shipperModels, AlertDialog dialog, Button btn_ok, Button btn_cancel, RadioButton rdi_shipping, RadioButton rdi_shipped, RadioButton rdi_cancelled, RadioButton rdi_delete, RadioButton rdi_restore_placed) {
        if (recycler_shipper != null)
        {
            recycler_shipper.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
            recycler_shipper.setLayoutManager(layoutManager);
            recycler_shipper.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

            myShipperSelectedAdapter = new MyShipperSelectionAdapter(getContext(), shipperModels);
            recycler_shipper.setAdapter(myShipperSelectedAdapter);
        }
        showDialog(pos, orderModel, dialog, btn_ok, btn_cancel,
                rdi_shipping, rdi_shipped, rdi_cancelled, rdi_delete, rdi_restore_placed);
    }

    @Override
    public void onShipperLoadFailed(String message) {

        Toast.makeText(getContext(), ""+message, Toast.LENGTH_SHORT).show();
    }
}










