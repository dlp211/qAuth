/*
* Copyright (C) 2014 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package qauth.djd.qauthclient.main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import qauth.djd.qauthclient.R;
import qauth.djd.qauthclient.main.common.logger.Log;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class WatchAdapter extends RecyclerView.Adapter<WatchAdapter.ViewHolder> {
    private static final String TAG = "WatchAdapter";

    private List<Watch> mDataSet;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;
        private final TextView textView2;

        public ViewHolder(View v) {
            super(v);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Element " + getPosition() + " clicked.");
                }
            });
            textView = (TextView) v.findViewById(R.id.textView);
            textView2 = (TextView) v.findViewById(R.id.textView2);
        }

        public TextView getTextView() {
            return textView;
        }
        public TextView getTextView2() {
            return textView2;
        }

    }

    public WatchAdapter(List<Watch> dataSet) {
        mDataSet = dataSet;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.text_row_item, viewGroup, false);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.i("yoooo", "Element " + position + " set.");

        viewHolder.getTextView().setText( "Watch Model: " + mDataSet.get(position).model );
        viewHolder.getTextView2().setText( "Unique ID: " + mDataSet.get(position).deviceId );


        /*
        switch (mDataSet.get(position).type) {
            case 1:
                viewHolder.getImgAlc().setImageResource(R.drawable.);
                break;
            case 2:
                viewHolder.getImgAlc().setImageResource(R.drawable.shotglass);
                /*ViewGroup.LayoutParams lp =
                        new ViewGroup.LayoutParams(65, 65);
                lp.setMargins(30, 0, 0, 0);
                viewHolder.getImgAlc().setLayoutParams(lp);
                viewHolder.getImgAlc().requestLayout();*/
                //break;
            //default:
                //viewHolder.getImgAlc().setImageResource(R.drawable.beermug);
        //}

    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
