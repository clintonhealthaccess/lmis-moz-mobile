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

package org.openlmis.core.view.fragment;

import static org.openlmis.core.presenter.StockCardPresenter.ArchiveStatus.ACTIVE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.event.CmmCalculateEvent;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.presenter.Presenter;
import org.openlmis.core.presenter.StockCardPresenter;
import org.openlmis.core.service.DirtyDataManager;
import org.openlmis.core.utils.Constants;
import org.openlmis.core.view.activity.BaseActivity;
import org.openlmis.core.view.activity.HomeActivity;
import org.openlmis.core.view.activity.StockMovementsWithLotActivity;
import org.openlmis.core.view.adapter.StockCardListAdapter;
import org.openlmis.core.view.holder.StockCardViewHolder;
import org.openlmis.core.view.viewmodel.InventoryViewModel;
import org.openlmis.core.view.widget.ProductsUpdateBanner;
import roboguice.inject.InjectView;

public class StockCardListFragment extends BaseFragment implements
    StockCardPresenter.StockCardListView, AdapterView.OnItemSelectedListener {

  @InjectView(R.id.sort_spinner)
  Spinner sortSpinner;

  @InjectView(R.id.tv_total)
  TextView tvTotal;

  @InjectView(R.id.products_list)
  RecyclerView stockCardRecycleView;

  @InjectView(R.id.product_update_banner)
  ProductsUpdateBanner productsUpdateBanner;

  @Inject
  StockCardPresenter presenter;

  @Inject
  SharedPreferenceMgr sharedPreferenceMgr;

  @Inject
  DirtyDataManager dirtyDataManager;

  StockCardListAdapter mAdapter;

  private int currentPosition;

  @Override
  public Presenter initPresenter() {
    return presenter;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_stockcard_list, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    initRecycleView();
    initSortSpinner();
    loadStockCards();
    EventBus.getDefault().register(this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    currentPosition = position;
    switch (position) {
      case 0:
        mAdapter.sortByName(true);
        break;
      case 1:
        mAdapter.sortByName(false);
        break;
      case 2:
        mAdapter.sortBySOH(false);
        break;
      case 3:
        mAdapter.sortBySOH(true);
        break;
      default:
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // do nothing
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == Activity.RESULT_OK) {
      if (requestCode == Constants.REQUEST_FROM_STOCK_LIST_PAGE) {
        long[] stockCardIds = data.getLongArrayExtra(Constants.PARAM_STOCK_CARD_ID_ARRAY);
        if (stockCardIds == null) {
          return;
        }
        presenter.refreshStockCardsObservable(stockCardIds);
      } else if (requestCode == Constants.REQUEST_UNPACK_KIT) {
        presenter.loadKits();
      } else if (requestCode == Constants.REQUEST_ARCHIVED_LIST_PAGE) {
        loadStockCards();
      }
    }
  }

  @Override
  public void refreshBannerText() {
    productsUpdateBanner.refreshBannerText();
  }

  @Override
  public void showWarning() {
    ((BaseActivity) requireActivity()).showDeletedWarningDialog(buildWarningDialogFragmentDelegate());
  }

  @Override
  public void refresh(List<InventoryViewModel> data) {
    mAdapter.refreshList(data);
    tvTotal.setText(getString(R.string.label_total, mAdapter.getItemCount()));
    onItemSelected(sortSpinner, null, currentPosition, 0L);
  }

  public void onSearch(String query) {
    mAdapter.filter(query);
    tvTotal.setText(getString(R.string.label_total, mAdapter.getItemCount()));
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onReceiveCmmCalculateEvent(CmmCalculateEvent event) {
    if (event.isStart()) {
      return;
    }
    loadStockCards();
  }

  protected void createAdapter() {
    mAdapter = new StockCardListAdapter(new ArrayList<>(), onItemViewClickListener);
  }

  protected void loadStockCards() {
    presenter.loadStockCards(ACTIVE);
  }

  protected StockCardViewHolder.OnItemViewClickListener onItemViewClickListener = inventoryViewModel -> {
    Intent intent = getStockMovementIntent(inventoryViewModel);
    startActivityForResult(intent, Constants.REQUEST_FROM_STOCK_LIST_PAGE);
  };

  protected Intent getStockMovementIntent(InventoryViewModel inventoryViewModel) {
    return StockMovementsWithLotActivity.getIntentToMe(getActivity(), inventoryViewModel, false);
  }

  @NonNull
  private WarningDialogFragment.DialogDelegate buildWarningDialogFragmentDelegate() {
    return () -> {
      dirtyDataManager.deleteAndReset();
      Intent intent = HomeActivity.getIntentToMe(LMISApp.getContext());
      requireActivity().startActivity(intent);
      requireActivity().finish();
    };
  }

  private void initRecycleView() {
    createAdapter();
    stockCardRecycleView.setHasFixedSize(true);
    stockCardRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
    stockCardRecycleView.setAdapter(mAdapter);
  }

  private void initSortSpinner() {
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
        R.array.sort_items_array, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    sortSpinner.setAdapter(adapter);
    sortSpinner.setOnItemSelectedListener(this);
  }
}
