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

package org.openlmis.core.view.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import com.viethoa.RecyclerViewFastScroller;
import com.viethoa.models.AlphabetItem;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.googleanalytics.ScreenName;
import org.openlmis.core.googleanalytics.TrackerActions;
import org.openlmis.core.googleanalytics.TrackerCategories;
import org.openlmis.core.model.Program;
import org.openlmis.core.presenter.InventoryPresenter;
import org.openlmis.core.utils.ToastUtil;
import org.openlmis.core.view.adapter.InventoryListAdapter;
import org.openlmis.core.view.fragment.SimpleDialogFragment;
import org.openlmis.core.view.viewmodel.InventoryViewModel;
import roboguice.inject.InjectView;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public abstract class InventoryActivity<T extends InventoryPresenter> extends SearchBarActivity implements
    InventoryPresenter.InventoryView,
    SimpleDialogFragment.MsgDialogCallBack {

  @InjectView(R.id.fast_scroller)
  RecyclerViewFastScroller fastScroller;

  @InjectView(R.id.products_list)
  public RecyclerView productListRecycleView;

  @InjectView(R.id.tv_total)
  public TextView tvTotal;

  @InjectView(R.id.btn_complete)
  public Button btnDone;

  @InjectView(R.id.btn_save)
  public View btnSave;

  protected InventoryListAdapter<?> mAdapter;

  protected T presenter;

  protected Action1<Object> onNextMainPageAction = o -> {
    loaded();
    goToNextPage();
  };

  protected Action1<Throwable> errorAction = throwable -> {
    loaded();
    showErrorMessage(throwable.getMessage());
  };

  private static final int INVENTORY_MENU_GROUP = 1;

  private final ArrayList<OptionsItem> optionsItems = new ArrayList<>();

  @Override
  public boolean onSearchStart(String query) {
    mAdapter.filter(query);
    setUpFastScroller(mAdapter.getFilteredList());
    return false;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int order = item.getOrder();
    if (order < 0 || order >= optionsItems.size()) {
      return super.onOptionsItemSelected(item);
    }
    mAdapter.setFilterProgram(optionsItems.get(order).getProgram());
    setUpFastScroller(mAdapter.getFilteredList());
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.removeGroup(INVENTORY_MENU_GROUP);
    for (int i = 0; i < optionsItems.size(); i++) {
      menu.add(INVENTORY_MENU_GROUP, i, i, optionsItems.get(i).getName());
    }
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public void showErrorMessage(String msg) {
    ToastUtil.show(msg);
  }

  @Override
  public boolean validateInventory() {
    int position = mAdapter.validateAll();
    setTotal();
    if (position >= 0) {
      clearSearch();
      productListRecycleView.scrollToPosition(position);
      return false;
    }
    return true;
  }

  @Override
  public void positiveClick(String tag) {
    this.finish();
  }

  @Override
  public void negativeClick(String tag) {
    // do nothing
  }

  protected abstract void initRecyclerView();

  protected abstract void goToNextPage();

  protected abstract void setTotal();

  protected abstract T initPresenter();

  @Override
  protected ScreenName getScreenName() {
    return ScreenName.INVENTORY_SCREEN;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.presenter = initPresenter();
    initUI();
    initDate();
    trackInventoryEvent(TrackerActions.SELECT_INVENTORY);
  }

  protected void initUI() {
    initRecyclerView();
    btnSave.setOnClickListener(v -> onSaveClick());
    btnDone.setOnClickListener(v -> onCompleteClick());
  }

  protected void onSaveClick() {
    // do nothing
  }

  protected void onCompleteClick() {
    mAdapter.setFilterProgram(null);
    mAdapter.refresh();
  }

  protected void initDate() {
    loading();
    final Subscription subscription = presenter.loadPrograms().subscribe(getOnProgramsLoadedSubscriber());
    subscriptions.add(subscription);
  }

  @NonNull
  protected Subscriber<List<Program>> getOnProgramsLoadedSubscriber() {
    return new Subscriber<List<Program>>() {
      @Override
      public void onCompleted() {
        // do nothing
      }

      @Override
      public void onError(Throwable e) {
        ToastUtil.show(e.getMessage());
        loaded();
        finish();
      }

      @Override
      public void onNext(List<Program> programs) {
        buildOptionsItem(programs);
        invalidateOptionsMenu();
        Subscription subscription = presenter.getInflatedInventory()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(getOnViewModelsLoadedSubscriber());
        subscriptions.add(subscription);
      }
    };
  }

  @NonNull
  protected Subscriber<List<InventoryViewModel>> getOnViewModelsLoadedSubscriber() {
    return new Subscriber<List<InventoryViewModel>>() {
      @Override
      public void onCompleted() {
        // do nothing
      }

      @Override
      public void onError(Throwable e) {
        ToastUtil.show(e.getMessage());
        loaded();
      }

      @Override
      public void onNext(List<InventoryViewModel> inventoryViewModels) {
        setUpFastScroller(inventoryViewModels);
        mAdapter.refresh();
        setTotal();
        loaded();
      }
    };
  }

  protected void trackInventoryEvent(TrackerActions action) {
    LMISApp.getInstance().trackEvent(TrackerCategories.INVENTORY, action);
  }

  protected void setUpFastScroller(List<InventoryViewModel> viewModels) {
    fastScroller.setVisibility(View.VISIBLE);
    if (viewModels.isEmpty()) {
      fastScroller.setVisibility(View.GONE);
    }
    List<AlphabetItem> mAlphabetItems = new ArrayList<>();
    List<String> strAlphabets = new ArrayList<>();
    for (int i = 0; i < viewModels.size(); i++) {
      String name = viewModels.get(i).getProductName();
      if (StringUtils.isBlank(name)) {
        continue;
      }

      String word = name.substring(0, 1);
      if (!strAlphabets.contains(word)) {
        strAlphabets.add(word);
        mAlphabetItems.add(new AlphabetItem(i, word, false));
      }
    }

    fastScroller.setRecyclerView(productListRecycleView);
    fastScroller.setUpAlphabet(mAlphabetItems);
  }

  protected String getValidateFailedTips() {
    final StringBuilder tips = new StringBuilder();
    final List<Program> validateFailedProgram = mAdapter.getValidateFailedProgram();
    for (int i = 0; i < validateFailedProgram.size(); i++) {
      final Program program = validateFailedProgram.get(i);
      if (i > 0 && i == validateFailedProgram.size() - 1) {
        tips.append("and ");
      }
      tips.append(program.getProgramName()).append(' ');
    }
    return getString(R.string.msg_validate_failed_tips, tips.toString());
  }

  private void buildOptionsItem(List<Program> programs) {
    optionsItems.clear();
    optionsItems.add(new OptionsItem.OptionsItemBuilder()
        .name(getString(R.string.label_inventory_options_all_product))
        .program(null)
        .build());
    for (Program program : programs) {
      optionsItems.add(new OptionsItem.OptionsItemBuilder()
          .name(program.getProgramName())
          .program(program)
          .build());
    }
  }

  @Data
  @Builder
  private static class OptionsItem {

    private String name;

    @Nullable
    private Program program;
  }
}
