package com.ds.androideatitv2server.ui.category;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ds.androideatitv2server.Adapter.MyCategoriesAdapter;
import com.ds.androideatitv2server.Common.Common;
import com.ds.androideatitv2server.Common.MySwipeHelper;
import com.ds.androideatitv2server.Common.SpacesItemDecoration;
import com.ds.androideatitv2server.EventBus.ToastEvent;
import com.ds.androideatitv2server.Model.CategoryModel;
import com.ds.androideatitv2server.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import dmax.dialog.SpotsDialog;

public class CategoryFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1234;
    private CategoryViewModel categoryViewModel;

    Unbinder unbinder;
    @BindView(R.id.recycler_menu)
    RecyclerView recycler_menu;
    AlertDialog dialog;
    LayoutAnimationController layoutAnimationController;
    MyCategoriesAdapter adapter;

    List<CategoryModel> categoryModels;
    ImageView img_category;
    private Uri imageUri = null;

    FirebaseStorage storage;
    StorageReference storageReference;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        categoryViewModel =
                ViewModelProviders.of(this).get(CategoryViewModel.class);
        View root = inflater.inflate(R.layout.fragment_category, container, false);
        unbinder = ButterKnife.bind(this, root);
        initViews();
        categoryViewModel.getMessageError().observe(this, s -> {
            Toast.makeText(getContext(), ""+s, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        categoryViewModel.getCategoryListMultable().observe(this, categoryModelList -> {
            dialog.dismiss();
            categoryModels = categoryModelList;
            adapter = new MyCategoriesAdapter(getContext(), categoryModels);
            recycler_menu.setAdapter(adapter);
            recycler_menu.setLayoutAnimation(layoutAnimationController);
        });
        return root;
    }

    private void initViews() {

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        dialog = new SpotsDialog.Builder().setContext(getContext()).setCancelable(false).build();
        //dialog.show(); Removing this to fix loading show when resume fragment
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(getContext(), R.anim.layout_item_from_left);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());


        recycler_menu.setLayoutManager(layoutManager);
        recycler_menu.addItemDecoration(new DividerItemDecoration(getContext(), layoutManager.getOrientation()));

        MySwipeHelper mySwipeHelper = new MySwipeHelper(getContext(),recycler_menu, 200) {
            @Override
            public void instantiateMyButton(RecyclerView.ViewHolder viewHolder, List<MyButton> buf) {
                buf.add(new MyButton(getContext(), "Update", 30, 0, Color.parseColor("#560027"), pos -> {

                    Common.categorySelected = categoryModels.get(pos);
                    
                    showUpdateDialog();

                }));
            }
        };


    }

    private void showUpdateDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle("Update");
        builder.setMessage("Please fill information");

        View itemView = LayoutInflater.from(getContext()).inflate(R.layout.layout_update_category, null);
        EditText edt_category_name = (EditText) itemView.findViewById(R.id.edt_category_name);
        img_category = (ImageView) itemView.findViewById(R.id.img_category);

        //Set data
        edt_category_name.setText(new StringBuilder("").append(Common.categorySelected.getName()));
        Glide.with(getContext()).load(Common.categorySelected.getImage()).into(img_category);

        //Set Event
        img_category.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
        });

        builder.setNegativeButton("CANCEL", (dialogInterface, which) -> dialogInterface.dismiss());
        builder.setPositiveButton("UPDATE", (dialogInterface, which) -> {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", edt_category_name.getText().toString());

            if(imageUri != null)
            {
                //In this we will use fire base storage to upload image
                dialog.setMessage("Uploading...");
                dialog.show();

                String unique_name = UUID.randomUUID().toString(); //unique_name == image name
                StorageReference imageFolder = storageReference.child("images/"+unique_name);

                imageFolder.putFile(imageUri)
                        .addOnFailureListener(e -> {
                            dialog.dismiss();
                            Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }).addOnCompleteListener(task -> {
                            dialog.dismiss();
                            imageFolder.getDownloadUrl().addOnSuccessListener(uri -> {
                                updateData.put("image", uri.toString());
                                updateCategory(updateData);
                            });
                        }).addOnProgressListener(taskSnapshot -> {
                            double progress = (100.0* taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            dialog.setMessage(new StringBuilder("Uploading: ").append(progress).append("%"));
                });
            }
            else
            {
                updateCategory(updateData);
            }

        });

        builder.setView(itemView);
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void updateCategory(Map<String, Object> updateData) {
        FirebaseDatabase.getInstance()
                .getReference(Common.CATEGORY_REF)
                .child(Common.categorySelected.getMenu_id())
                .updateChildren(updateData)
                .addOnFailureListener(e -> Toast.makeText(getContext(), ""+e.getMessage(), Toast.LENGTH_SHORT).show())
                .addOnCompleteListener(task -> {
                    categoryViewModel.loadCategories();
                    EventBus.getDefault().postSticky(new ToastEvent(true, false));

                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK)
        {
            if(data != null  &&  data.getData() != null )
            {
                imageUri = data.getData();
                img_category.setImageURI(imageUri);
            }
        }
    }
}