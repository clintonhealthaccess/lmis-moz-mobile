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

package org.openlmis.core.view.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.openlmis.core.R;
import org.openlmis.core.model.Service;
import org.openlmis.core.view.viewmodel.PTVReportViewModel;

public class PTVTestLeftHeader extends FrameLayout {

  private LayoutInflater layoutInflater;
  private PTVReportViewModel viewModel;

  public PTVTestLeftHeader(Context context) {
    super(context);
    init();
  }

  public PTVTestLeftHeader(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public PTVTestLeftHeader(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public PTVTestLeftHeader(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init();
  }

  private void init() {
    layoutInflater = LayoutInflater.from(getContext());
  }

  private ViewGroup inflateView() {
    return (ViewGroup) layoutInflater.inflate(R.layout.fragment_ptv_left_header, this, false);
  }

  public void initView(PTVReportViewModel viewModel) {
    this.viewModel = viewModel;
    ViewGroup inflate = inflateView();
    TextView tvName = inflate.findViewById(R.id.tv_name);
    EditText etStock = inflate.findViewById(R.id.et_initial_stock);
    ViewGroup service = inflate.findViewById(R.id.ll_services);
    List<EditText> services = addService(service);
    TextView tvTotal = inflate.findViewById(R.id.tv_total);
    TextView tvReceived = inflate.findViewById(R.id.tv_entry);
    EditText etAdjustment = inflate.findViewById(R.id.et_adjustment);
    EditText etFinalStock = inflate.findViewById(R.id.et_finalStock);
    setHeaderView(tvName, etStock, services, tvTotal, tvReceived, etAdjustment, etFinalStock);
    addView(inflate);
  }

  private List<EditText> addService(ViewGroup service) {
    List<EditText> etServices = new ArrayList<>();
    for (Service serviceItem : viewModel.getServices()) {
      ViewGroup inflate = (ViewGroup) layoutInflater.inflate(R.layout.item_service, this, false);
      EditText etService = inflate.findViewById(R.id.et_service);
      etService.setId(viewModel.getServices().indexOf(serviceItem));
      service.addView(inflate);
      etServices.add(etService);
    }
    return etServices;
  }

  private void setHeaderView(TextView tvName,
      EditText etStock,
      List<EditText> services,
      TextView tvTotal,
      TextView tvReceived,
      EditText etAdjustment,
      EditText etFinalStock) {
    tvName.setText(R.string.PatientAndService);
    etStock.setText(R.string.initialStockLevel);
    tvTotal.setText(R.string.totalPtv);
    tvReceived.setText(R.string.entries);
    etAdjustment.setText(R.string.loss_and_adjustment);
    etFinalStock.setText(R.string.final_stock);
    etStock.setEnabled(false);
    etAdjustment.setEnabled(false);
    etFinalStock.setEnabled(false);
    for (EditText etService : services) {
      int serviceIndex = etService.getId();
      Service serviceCurrent = viewModel.getServices().get(serviceIndex);
      etService.setText(serviceCurrent.getName());
      etService.setEnabled(false);
    }
    setHeaderViewTextStyle(etStock, services, tvTotal, tvReceived, etAdjustment, etFinalStock);
  }

  private void setHeaderViewTextStyle(EditText etStock,
      List<EditText> services,
      TextView tvTotal,
      TextView tvReceived,
      EditText etAdjustment,
      EditText etFinalStock) {
    float fontSize = getResources().getDimension(R.dimen.font_size_small);
    etStock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
    etStock.setTypeface(Typeface.DEFAULT_BOLD);
    tvTotal.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
    tvTotal.setTypeface(Typeface.DEFAULT_BOLD);
    tvReceived.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
    tvReceived.setTypeface(Typeface.DEFAULT_BOLD);
    etAdjustment.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
    etAdjustment.setTypeface(Typeface.DEFAULT_BOLD);
    etFinalStock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
    etFinalStock.setTypeface(Typeface.DEFAULT_BOLD);
    for (EditText etService : services) {
      etService.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, fontSize);
      etService.setTypeface(Typeface.DEFAULT_BOLD);
    }
  }
}
