package com.ds.androideatitv2shipper.ui.shipOrders;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.androideatitv2shipper.Adapters.MyShippingOrderAdapter;
import com.ds.androideatitv2shipper.Common.Common;
import com.ds.androideatitv2shipper.Model.EventBus.UpdateShippingOrderEvent;
import com.ds.androideatitv2shipper.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;

public class OrdersToShipFragment extends Fragment {

    @BindView(R.id.recycler_order)
    RecyclerView recycler_order;


    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    Unbinder unbinder;
    LayoutAnimationController layoutAnimationController;
    MyShippingOrderAdapter adapter;

    private OrdersToShipViewModel ordersToShipViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ordersToShipViewModel =
                ViewModelProviders.of(this).get(OrdersToShipViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home_orders_needs_shipping_list, container, false);
        initViews(root);
        ordersToShipViewModel.getMessageError().observe(this, s -> {
            Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
        });
        ordersToShipViewModel.getShippingOrderMutableData(Common.currentShipperUser.getPhone()).observe(this, shippingOrderModels -> {
            adapter = new MyShippingOrderAdapter(getContext(),shippingOrderModels);
            recycler_order.setAdapter(adapter);
            recycler_order.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews(View root) {
        unbinder = ButterKnife.bind(this,root);
        setHasOptionsMenu(false);

        recycler_order.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_order.setLayoutManager(layoutManager);
        recycler_order.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_slide_from_left);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (EventBus.getDefault().hasSubscriberForEvent(UpdateShippingOrderEvent.class))
            EventBus.getDefault().removeStickyEvent(UpdateShippingOrderEvent.class);
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onUpdateShippingOrder(UpdateShippingOrderEvent event)
    {
        ordersToShipViewModel.getShippingOrderMutableData(Common.currentShipperUser.getPhone()); //Update data
    }

}