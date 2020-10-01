package com.ds.androideatitv2server.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.androideatitv2server.Callback.IRecyclerClickListener;
import com.ds.androideatitv2server.Common.Common;
import com.ds.androideatitv2server.Model.CartItem;
import com.ds.androideatitv2server.Model.OrderModel;
import com.ds.androideatitv2server.R;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyOrderAdapter extends RecyclerView.Adapter<MyOrderAdapter.MyViewHolder> {

    private Context context;
    List<OrderModel> orderModelList;
    //private Calendar calendar;
    private SimpleDateFormat simpleDateFormat;

    public MyOrderAdapter() {
    }

    public MyOrderAdapter(Context context, List<OrderModel> orderModelList) {
        this.context = context;
        this.orderModelList = orderModelList;
        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.layout_order_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context)
                .load(orderModelList.get(position).getCartItemList().get(0).getFoodImage())
                .into(holder.img_food_image); // Load default image exists in cart

        holder.txt_order_number.setText(orderModelList.get(position).getKey());
        Common.setSpanStringColor("Order date ", simpleDateFormat.format(orderModelList.get(position).getCreateDate()),
                holder.txt_time, Color.parseColor("#333639"));
        Common.setSpanStringColor("Order status ", Common.convertStatusToText(orderModelList.get(position).getOrderStatus()),
                holder.txt_order_status, Color.parseColor("#00579A"));
        Common.setSpanStringColor("Name ", orderModelList.get(position).getUserName(),
                holder.txt_name, Color.parseColor("#00574B"));
        Common.setSpanStringColor("Num of items ", orderModelList.get(position).getCartItemList() == null ? "0" :
                String.valueOf(orderModelList.get(position).getCartItemList().size()),
                holder.txt_num_item, Color.parseColor("#4B647D"));

        holder.setRecyclerClickListener((view, pos) -> {
            showDialog(orderModelList.get(pos).getCartItemList());
        });
    }

    private void showDialog(List<CartItem> cartItemList) {
        View layout_dialog = LayoutInflater.from(context).inflate(R.layout.layout_dialog_order_daetil, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(layout_dialog);

        Button btn_ok = (Button) layout_dialog.findViewById(R.id.btn_ok);
        RecyclerView recycler_order_detail = (RecyclerView)layout_dialog.findViewById(R.id.recycler_order_detail);
        recycler_order_detail.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        recycler_order_detail.setLayoutManager(layoutManager);
        recycler_order_detail.addItemDecoration(new DividerItemDecoration(context, layoutManager.getOrientation()));

        MyOrderDetailAdapter myOrderDetailAdapter = new MyOrderDetailAdapter(context, cartItemList);
        recycler_order_detail.setAdapter(myOrderDetailAdapter);

        //Show Dialog
        AlertDialog dialog = builder.create();
        dialog.show();
        //Custom dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.CENTER);

        btn_ok.setOnClickListener(v -> dialog.dismiss());
    }

    @Override
    public int getItemCount() {
        return orderModelList.size();
    }

    public OrderModel getItemAtPosition(int pos) {
        return orderModelList.get(pos);
    }

    public void removeItem(int pos) {
         orderModelList.remove(pos);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.img_food_image)
        ImageView img_food_image;
        @BindView(R.id.txt_name)
        TextView txt_name;
        @BindView(R.id.txt_time)
        TextView txt_time;
        @BindView(R.id.txt_order_status)
        TextView txt_order_status;
        @BindView(R.id.txt_order_number)
        TextView txt_order_number;
        @BindView(R.id.txt_num_item)
        TextView txt_num_item;


        Unbinder unbinder;

        IRecyclerClickListener recyclerClickListener;

        public void setRecyclerClickListener(IRecyclerClickListener recyclerClickListener) {
            this.recyclerClickListener = recyclerClickListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            recyclerClickListener.onItemClickListener(v, getAdapterPosition());
        }
    }
}
