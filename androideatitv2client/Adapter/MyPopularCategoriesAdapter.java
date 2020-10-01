package com.ds.androideatitv2client.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.androideatitv2client.Callback.IRecyclerClickListener;
import com.ds.androideatitv2client.EventBus.PopularCategoryClick;
import com.ds.androideatitv2client.Model.PopularCategoryModel;
import com.ds.androideatitv2client.R;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.annotations.NonNull;

public class MyPopularCategoriesAdapter extends RecyclerView.Adapter<MyPopularCategoriesAdapter.MyViewHolder> {

    Context context;
    List<PopularCategoryModel> popularCategoryModelList;

    public MyPopularCategoriesAdapter(Context context, List<PopularCategoryModel> popularCategoryModelList) {
        this.context = context;
        this.popularCategoryModelList = popularCategoryModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context)
        .inflate(R.layout.layout_popular_categories_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Glide.with(context).load(popularCategoryModelList.get(position).getImage())
                .into(holder.category_image);
        holder.txt_category_name.setText(popularCategoryModelList.get(position).getName());

        holder.setListener((view, pos) ->
                //Toast.makeText(context, ""+popularCategoryModelList.get(pos).getName(), Toast.LENGTH_SHORT).show()
                        EventBus.getDefault().postSticky(new PopularCategoryClick(popularCategoryModelList.get(pos)))
                 );
    }

    @Override
    public int getItemCount() {
        return popularCategoryModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        Unbinder unbinder;

        @BindView(R.id.txt_category_name)
        TextView txt_category_name;
        @BindView(R.id.category_image)
        CircleImageView category_image;

        IRecyclerClickListener listener;

        public void setListener(IRecyclerClickListener listener) {
            this.listener = listener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this,itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            listener.onItemClickListener(view, getAdapterPosition());
        }
    }
}
