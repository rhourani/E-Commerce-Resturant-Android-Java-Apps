package com.ds.androideatitv2client.ui.cart;


import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.ds.androideatitv2client.Common.Common;
import com.ds.androideatitv2client.Database.CartDataSource;
import com.ds.androideatitv2client.Database.CartDatabase;
import com.ds.androideatitv2client.Database.CartItem;
import com.ds.androideatitv2client.Database.LocalCartDataSource;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class CartViewModel extends ViewModel {

    private CompositeDisposable compositeDisposable;
    private CartDataSource cartDataSource;
    private MutableLiveData<List<CartItem>> mutableLiveDataCartItem;

    public CartViewModel() {
        compositeDisposable  = new CompositeDisposable();
    }

    public void initCartDataSource(Context context)
    {
        cartDataSource = new LocalCartDataSource(CartDatabase.getInstance(context).cartDAO());
    }

    public void onStop()
    {
        compositeDisposable.clear();
    }

    public MutableLiveData<List<CartItem>> getMutableLiveDataCartItem() {
        if(mutableLiveDataCartItem == null)
            mutableLiveDataCartItem = new MutableLiveData<>();
        getAllCartItems();
        return mutableLiveDataCartItem;
    }

    private void getAllCartItems() {
        compositeDisposable.add(cartDataSource.getAllCart(Common.currentUser.getUid())
        .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(cartItems -> {
                    mutableLiveDataCartItem.setValue(cartItems);
                }, throwable -> {
                    mutableLiveDataCartItem.setValue(null);
                }));
    }
}
