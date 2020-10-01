package com.ds.androideatitv2client.ui.view_orders;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.androidwidgets.formatedittext.widgets.FormatEditText;
import com.ds.androideatitv2client.Adapter.MyOrdersAdapter;
import com.ds.androideatitv2client.Callback.ILoadOrderCallbackListener;
import com.ds.androideatitv2client.Common.Common;
import com.ds.androideatitv2client.Common.MySwipeHelper;
import com.ds.androideatitv2client.Database.CartItem;
import com.ds.androideatitv2client.EventBus.CounterCartEvent;
import com.ds.androideatitv2client.Model.OrderModel;
import com.ds.androideatitv2client.Model.RefundRequestModel;
import com.ds.androideatitv2client.Model.ShippingOrderModel;
import com.ds.androideatitv2client.R;
import com.ds.androideatitv2client.TrackingOrderActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class ViewOrdersFragment extends Fragment implements ILoadOrderCallbackListener {

    @BindView(R.id.recycler_orders)
    RecyclerView recycler_orders;

    private ViewOrdersViewModel viewOrdersViewModel;

    private Unbinder unbinder;
    
    AlertDialog dialog;

    private ILoadOrderCallbackListener listener;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        viewOrdersViewModel =
                ViewModelProviders.of(this).get(ViewOrdersViewModel.class);
        View root = inflater.inflate(R.layout.fragment_view_order, container, false);
        unbinder = ButterKnife.bind(this, root);
        
        initViews(root);
        loadOrdersFromFirebase();

        viewOrdersViewModel.getMutableLiveDataOrderList().observe(this, orderList -> {
            Collections.reverse(orderList);
            MyOrdersAdapter adapter = new MyOrdersAdapter(getContext(), orderList);
            recycler_orders.setAdapter(adapter);
        });
        
        return root;
    }

    private void loadOrdersFromFirebase() {
        List<OrderModel> orderModelList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference(Common.ORDER_REF)
                .orderByChild("userId")
                .equalTo(Common.currentUser.getUid())
                .limitToLast(100)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot orderSnapShot:dataSnapshot.getChildren())
                        {
                            OrderModel orderModel = orderSnapShot.getValue(OrderModel.class);
                            orderModel.setOrderNumber(orderSnapShot.getKey()); // Remember set it
                            orderModelList.add(orderModel);
                        }
                        listener.onLoadOrderSuccess(orderModelList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        listener.onLoadOrderFailed(databaseError.getMessage());
                    }
                });
    }

    private void initViews(View root) {
        listener = this;

        dialog = new SpotsDialog.Builder().setCancelable(false).setContext(getContext()).build();

        recycler_orders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_orders.setLayoutManager(layoutManager);
        recycler_orders.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(), recycler_orders, 250) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Cancel Order", 30, 0, Color.parseColor("#FF3C30"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPostion(pos);
                            if (orderModel.getOrderStatus() == 0)
                            {
                               if (orderModel.isCod())
                               {
                                   androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                   builder.setTitle("Cancel Order")
                                           .setMessage("Do you really want to cancel this order?")
                                           .setNegativeButton("NO", (dialogInterface, which) -> dialogInterface.dismiss())
                                           .setPositiveButton("YES", (dialogInterface, which) -> {

                                               Map<String,Object> update_data = new HashMap<>();
                                               update_data.put("orderStatus",-1); // Canceling order
                                               FirebaseDatabase.getInstance()
                                                       .getReference(Common.ORDER_REF)
                                                       .child(orderModel.getOrderNumber())
                                                       .updateChildren(update_data)
                                                       .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                       .addOnSuccessListener(aVoid -> {
                                                           orderModel.setOrderStatus(-1);
                                                           ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                           recycler_orders.getAdapter().notifyItemChanged(pos);
                                                           Toast.makeText(getContext(), "Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                       });
                                           });
                                   androidx.appcompat.app.AlertDialog dialog = builder.create();
                                   dialog.show();
                               }
                               else // Not COD
                               {
                                   View layout_refund_request = LayoutInflater.from(getContext())
                                           .inflate(R.layout.layout_refund_request, null);

                                   EditText edt_name = (EditText) layout_refund_request.findViewById(R.id.edt_card_name);
                                   FormatEditText edt_card_number = (FormatEditText) layout_refund_request.findViewById(R.id.edt_card_number);
                                   FormatEditText edt_card_exp = (FormatEditText) layout_refund_request.findViewById(R.id.edt_exp);

                                   //Format credit card
                                   edt_card_number.setFormat("---- ---- ---- ----");
                                   edt_card_exp.setFormat("--/--");

                                   androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
                                   builder.setTitle("Cancel Order")
                                           .setMessage("Do you really want to cancel this order?")
                                           .setView(layout_refund_request)
                                           .setNegativeButton("NO", (dialogInterface, which) -> dialogInterface.dismiss())
                                           .setPositiveButton("YES", (dialogInterface, which) -> {

                                               RefundRequestModel refundRequestModel = new RefundRequestModel();
                                               refundRequestModel.setName(Common.currentUser.getName());
                                               refundRequestModel.setPhone(Common.currentUser.getPhone());
                                               refundRequestModel.setCardName(edt_name.getText().toString());
                                               refundRequestModel.setCardNumber(edt_card_number.getText().toString());
                                               refundRequestModel.setCardExp(edt_card_exp.getText().toString());
                                               refundRequestModel.setAmount(orderModel.getFinalPayment());



                                               FirebaseDatabase.getInstance()
                                                       .getReference(Common.REQUEST_REFUND_MODEL)
                                                       .child(orderModel.getOrderNumber())
                                                       .setValue(refundRequestModel)
                                                       .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                       .addOnSuccessListener(aVoid -> {
                                                           Map<String,Object> update_data = new HashMap<>();

                                                           //updating status in firebase
                                                           update_data.put("orderStatus",-1); // Canceling order
                                                           FirebaseDatabase.getInstance()
                                                                   .getReference(Common.ORDER_REF)
                                                                   .child(orderModel.getOrderNumber())
                                                                   .updateChildren(update_data)
                                                                   .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                                                                   .addOnSuccessListener(a -> {
                                                                       orderModel.setOrderStatus(-1);
                                                                       ((MyOrdersAdapter) recycler_orders.getAdapter()).setItemAtPosition(pos, orderModel);
                                                                       recycler_orders.getAdapter().notifyItemChanged(pos);
                                                                       Toast.makeText(getContext(), "Cancel order successfully!", Toast.LENGTH_SHORT).show();
                                                                   });
                                                       });
                                           });
                                   androidx.appcompat.app.AlertDialog dialog = builder.create();
                                   dialog.show();
                               }
                            }
                            else
                            {
                                Toast.makeText(getContext(), new StringBuilder("Your order was changed to ")
                                        .append(Common.convertStatusToText(orderModel.getOrderStatus()))
                                        .append(", so you can't cancel it!"), Toast.LENGTH_SHORT).show();
                            }

                        }));

                buf.add(new MyButton(getContext(), "Tracking Order", 30, 0, Color.parseColor("#001970"),
                        pos -> {
                            OrderModel orderModel = ((MyOrdersAdapter) recycler_orders.getAdapter()).getItemAtPostion(pos);
                           FirebaseDatabase.getInstance()
                                   .getReference(Common.SHIPPING_ORDER_REF)
                                   .child(orderModel.getOrderNumber())
                                   .addListenerForSingleValueEvent(new ValueEventListener() {
                                       @Override
                                       public void onDataChange(@NonNull DataSnapshot snapshot) {
                                           if (snapshot.exists())
                                           {
                                               Common.currentShippingOrder = snapshot.getValue(ShippingOrderModel.class);
                                               Common.currentShippingOrder.setKey(snapshot.getKey());
                                               if (Common.currentShippingOrder.getCurrentLat() != -1 &&
                                               Common.currentShippingOrder.getCurrentLng() != -1)
                                               {
                                                   startActivity(new Intent(getContext(), TrackingOrderActivity.class));
                                               }
                                               else
                                               {
                                                   Toast.makeText(getContext(), "Your order is ready but shipper doesnt start his trip to you! just wait please!", Toast.LENGTH_SHORT).show();
                                               }
                                           }
                                           else
                                           {
                                               Toast.makeText(getContext(), "Your order has been delivered already!", Toast.LENGTH_SHORT).show();
                                           }
                                       }

                                       @Override
                                       public void onCancelled(@NonNull DatabaseError error) {
                                           Toast.makeText(getContext(), ""+error.getMessage(), Toast.LENGTH_SHORT).show();
                                       }
                                   });

                        }));
            }
        };

    }

    @Override
    public void onLoadOrderSuccess(List<OrderModel> orderModelList) {
        dialog.dismiss();
        viewOrdersViewModel.setMutableLiveDataOrderList(orderModelList);
    }

    @Override
    public void onLoadOrderFailed(String message) {
        dialog.dismiss();
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}
