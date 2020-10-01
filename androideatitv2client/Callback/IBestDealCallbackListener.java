package com.ds.androideatitv2client.Callback;

import com.ds.androideatitv2client.Model.BestDealModel;
import com.ds.androideatitv2client.Model.PopularCategoryModel;

import java.util.List;

public interface IBestDealCallbackListener {
    void onBestDealLoadSuccess(List<BestDealModel> BestDealModels);
    void onBestDealLoadFailed(String message);
}
