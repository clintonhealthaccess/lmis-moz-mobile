/*
 * This program is part of the OpenLMIS logistics management information
 * system platform software.
 *
 * Copyright © 2015 ThoughtWorks, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version. This program is distributed in the
 * hope that it will be useful, but WITHOUT ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details. You should
 * have received a copy of the GNU Affero General Public License along with
 * this program. If not, see http://www.gnu.org/licenses. For additional
 * information contact info@OpenLMIS.org
 */

package org.openlmis.core.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import org.openlmis.core.R;
import org.openlmis.core.view.holder.RapidTestReportGridViewHolder;
import org.openlmis.core.view.viewmodel.RapidTestFormGridViewModel;

public class RapidTestReportGridAdapter extends
    RecyclerView.Adapter<RapidTestReportGridViewHolder> {

  Context context;
  private final boolean editable;
  List<RapidTestFormGridViewModel> viewModels;
  private final RapidTestReportGridViewHolder.QuantityChangeListener quantityChangeListener;
  private final int itemWidth;

  public RapidTestReportGridAdapter(List<RapidTestFormGridViewModel> viewModels, Context context,
      boolean editable,
      RapidTestReportGridViewHolder.QuantityChangeListener quantityChangeListener) {
    itemWidth = (int) (context.getResources().getDimension(R.dimen.rapid_view_width)) / 4;
    this.viewModels = viewModels;
    this.context = context;
    this.editable = editable;
    this.quantityChangeListener = quantityChangeListener;
  }

  @Override
  public RapidTestReportGridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView;
    if (editable) {
      itemView = LayoutInflater.from(context)
          .inflate(R.layout.item_rapid_test_report_grid, parent, false);
    } else {
      itemView = LayoutInflater.from(context)
          .inflate(R.layout.item_rapid_test_report_grid_total, parent, false);
    }
    itemView.getLayoutParams().width = itemWidth;
    return new RapidTestReportGridViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(RapidTestReportGridViewHolder holder, int position) {
    RapidTestFormGridViewModel viewModel = viewModels.get(position);
    holder.populate(viewModel, editable, quantityChangeListener);
  }

  @Override
  public int getItemCount() {
    return viewModels.size();
  }
}
