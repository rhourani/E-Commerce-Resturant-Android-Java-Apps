package com.ds.androideatitv2server.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ds.androideatitv2server.EventBus.UpdateShipperEvent;
import com.ds.androideatitv2server.Model.ShipperModel;
import com.ds.androideatitv2server.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class MyShipperAdapter extends RecyclerView.Adapter<MyShipperAdapter.MyViewHolder> {

    Context context;
    List<ShipperModel> shipperModelList;

    public MyShipperAdapter(Context context, List<ShipperModel> shipperModelList) {
        this.context = context;
        this.shipperModelList = shipperModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView= LayoutInflater.from(context).inflate(R.layout.layout_shipper, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.txt_name.setText(new StringBuilder(shipperModelList.get(position).getName()));
        holder.txt_phone.setText(new StringBuilder(shipperModelList.get(position).getPhone()));
        holder.btn_enable.setChecked(shipperModelList.get(position).isActive());

        holder.btn_enable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            EventBus.getDefault().postSticky(new UpdateShipperEvent(shipperModelList.get(position),isChecked));
        });
    }

    @Override
    public int getItemCount() {
        return shipperModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private Unbinder unbinder;

        @BindView(R.id.txt_name)
        TextView txt_name;
        @BindView(R.id.txt_phone)
        TextView txt_phone;
        @BindView(R.id.btn_enable)
        SwitchCompat btn_enable;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
        }
    }
}
