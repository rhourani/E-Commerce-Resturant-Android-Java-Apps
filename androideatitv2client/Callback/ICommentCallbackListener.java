package com.ds.androideatitv2client.Callback;

import com.ds.androideatitv2client.Model.CommentModel;

import java.util.List;

public interface ICommentCallbackListener {
    void onCommentLoadSuccess(List<CommentModel> commentModels);
    void onCommentLoadFailed(String message);

}
