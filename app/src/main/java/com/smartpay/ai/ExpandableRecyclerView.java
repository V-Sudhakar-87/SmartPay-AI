package com.smartpay.ai;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class ExpandableRecyclerView extends RecyclerView {

    public ExpandableRecyclerView(@NonNull Context context) {
        super(context);
    }

    public ExpandableRecyclerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs
    ) {
        super(context, attrs);
    }

    public ExpandableRecyclerView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(
            int widthMeasureSpec,
            int heightMeasureSpec
    ) {

        int expandedHeightSpec = View.MeasureSpec.makeMeasureSpec(
                Integer.MAX_VALUE >> 2,
                View.MeasureSpec.AT_MOST
        );

        super.onMeasure(widthMeasureSpec, expandedHeightSpec);
    }
}