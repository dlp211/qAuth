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
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import qauth.djd.qauthclient.R;
import qauth.djd.qauthclient.main.common.logger.Log;

/**
 * Provide views to RecyclerView with data from mDataSet.
 */
public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {
    private static final String TAG = "ProviderAdapter";

    private List<Provider> mDataSet;

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

    public ProviderAdapter(List<Provider> dataSet) {
        mDataSet = dataSet;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.provider_row_item, viewGroup, false);

        ProgressBar mProgress = (ProgressBar) v.findViewById(R.id.progress_bar);

        mProgress.setProgress(33);

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        viewHolder.getTextView().setText( "Application: " + mDataSet.get(position).appName );
        viewHolder.getTextView2().setText( "Package: " + mDataSet.get(position).packageName );

    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }
}
