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

package org.openlmis.core.view.holder;

import android.content.Context;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputLayout;
import org.apache.commons.lang3.StringUtils;
import org.openlmis.core.LMISApp;
import org.openlmis.core.R;
import org.openlmis.core.manager.MovementReasonManager;
import org.openlmis.core.utils.SingleTextWatcher;
import org.openlmis.core.view.activity.BaseActivity;
import org.openlmis.core.view.activity.BulkInitialInventoryActivity;
import org.openlmis.core.view.activity.InitialInventoryActivity;
import org.openlmis.core.view.activity.InventoryActivity;
import org.openlmis.core.view.activity.NewStockMovementActivity;
import org.openlmis.core.view.activity.PhysicalInventoryActivity;
import org.openlmis.core.view.activity.UnpackKitActivity;
import org.openlmis.core.view.adapter.LotMovementAdapter;
import org.openlmis.core.view.fragment.SimpleDialogFragment;
import org.openlmis.core.view.viewmodel.LotMovementViewModel;
import roboguice.inject.InjectView;

@SuppressWarnings("PMD")
public class LotMovementViewHolder extends BaseViewHolder {

  @InjectView(R.id.et_lot_amount)
  private EditText etLotAmount;

  @InjectView(R.id.ly_lot_amount)
  private TextInputLayout lyLotAmount;

  @InjectView(R.id.et_lot_info)
  private EditText etLotInfo;

  @InjectView(R.id.vg_lot_soh)
  private LinearLayout vgLotSOH;

  @InjectView(R.id.tv_lot_soh)
  private TextView tvLotSOH;

  @InjectView(R.id.tv_lot_soh_tip)
  private TextView tvLotSOHTip;

  @InjectView(R.id.iv_del)
  private ImageView iconDel;
  private LotMovementAdapter.MovementChangedListener movementChangeListener;
  private LotMovementAdapter.MovementChangedListenerWithStatus movementChangedListenerWithStatus;

  public LotMovementViewHolder(View itemView) {
    super(itemView);
  }

  public void populate(final LotMovementViewModel viewModel,
      final LotMovementAdapter lotMovementAdapter) {
    populateLotInfo(viewModel);
    populateLotSOHBanner(viewModel);
    updateDeleteIcon(viewModel.isNewAdded(),
        getOnClickListenerForDeleteIcon(viewModel, lotMovementAdapter));
    populateAmountField(viewModel);
  }

  private void populateLotSOHBanner(LotMovementViewModel viewModel) {
    if (context instanceof InitialInventoryActivity
        || context instanceof UnpackKitActivity
        || context instanceof BulkInitialInventoryActivity) {
      vgLotSOH.setVisibility(View.GONE);
    } else {
      if (viewModel.isNewAdded()) {
        if (viewModel.quantityGreaterThanZero()) {
          vgLotSOH.setVisibility(View.GONE);
        } else {
          tvLotSOHTip.setText(getString(R.string.label_new_added_lot));
        }
      } else {
        tvLotSOH.setText(viewModel.getLotSoh());
      }
    }
  }

  private void populateLotInfo(LotMovementViewModel viewModel) {
    etLotInfo.setText(viewModel.getLotNumber() + " - " + viewModel.getExpiryDate());
    etLotInfo.setKeyListener(null);
    etLotInfo.setOnKeyListener(null);
    etLotInfo.setBackground(null);
  }

  private void populateAmountField(LotMovementViewModel viewModel) {
    final EditTextWatcher textWatcher = new EditTextWatcher(viewModel);
    if (viewModel.isExpiredLot()
        && viewModel.getMovementType() == MovementReasonManager.MovementType.ISSUE) {
      etLotAmount.setText(null);
      lyLotAmount.setErrorEnabled(true);
      etLotAmount.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
      etLotAmount.setEnabled(false);
      etLotAmount.setHint(getString(R.string.lots_has_expire));
      etLotAmount.setTextSize(14);
      etLotAmount.setHintTextColor(context.getResources().getColor(R.color.color_red));
      etLotAmount.setGravity(Gravity.TOP);
      etLotAmount.setSingleLine(false);
      etLotAmount.setHorizontallyScrolling(false);
    } else {
      etLotAmount.setHintTextColor(context.getResources().getColor(R.color.color_select_title));
      etLotAmount.setEnabled(true);
      etLotAmount.setMaxLines(9);
      etLotAmount.removeTextChangedListener(textWatcher);
      etLotAmount.setText(viewModel.getQuantity());
      etLotAmount.addTextChangedListener(textWatcher);
      etLotAmount.setHint(LMISApp.getInstance().getString(R.string.hint_lot_amount));
      lyLotAmount.setErrorEnabled(false);
    }

    if (!viewModel.isValid()) {
      setQuantityError(getString(R.string.msg_empty_quantity));
    }
    if (!viewModel.isQuantityLessThanSoh()) {
      setQuantityError(getString(R.string.msg_invalid_quantity));
    } else {
      if (PhysicalInventoryActivity.KEY_FROM_PHYSICAL_COMPLETED.equals(viewModel.getFrom())
          && StringUtils.isBlank(etLotAmount.getText())) {
        lyLotAmount.setError(getString(R.string.msg_empty_quantity) + etLotAmount.getText());
      } else {
        lyLotAmount.setError(null);
      }
    }
  }

  private String getString(int id) {
    return context.getResources().getString(id);
  }

  private void setQuantityError(String string) {
    etLotAmount.requestFocus();
    lyLotAmount.setError(string);
  }

  private void updateDeleteIcon(boolean isNewAdded,
      View.OnClickListener onClickListenerForDeleteIcon) {
    if (isNewAdded) {
      iconDel.setVisibility(View.VISIBLE);
      iconDel.setOnClickListener(onClickListenerForDeleteIcon);
    }
  }

  @SuppressWarnings("squid:S1874")
  @NonNull
  private View.OnClickListener getOnClickListenerForDeleteIcon(final LotMovementViewModel viewModel,
      final LotMovementAdapter lotMovementAdapter) {
    return v -> {
      final SimpleDialogFragment dialogFragment = SimpleDialogFragment.newInstance(
          Html.fromHtml(getString(R.string.msg_remove_new_lot_title)),
          Html.fromHtml(context.getResources()
              .getString(R.string.msg_remove_new_lot, viewModel.getLotNumber(),
                  viewModel.getExpiryDate(), lotMovementAdapter.getProductName())),
          getString(R.string.btn_remove_lot),
          getString(R.string.btn_cancel), "confirm_dialog");
      dialogFragment.show(((BaseActivity) context).getSupportFragmentManager(), "confirm_dialog");
      dialogFragment.setCallBackListener(new SimpleDialogFragment.MsgDialogCallBack() {
        @Override
        public void positiveClick(String tag) {
          lotMovementAdapter.remove(viewModel);
          if (context instanceof InventoryActivity) {
            ((InventoryActivity) context).productListRecycleView.getAdapter()
                .notifyDataSetChanged();
          }
        }

        @Override
        public void negativeClick(String tag) {
          dialogFragment.dismiss();
        }
      });
    };
  }

  public void setMovementChangeListener(
      LotMovementAdapter.MovementChangedListener movementChangedListener) {
    this.movementChangeListener = movementChangedListener;
  }

  public void setMovementChangedLisenerWithStatus(
      LotMovementAdapter.MovementChangedListenerWithStatus movementChangedLisenerWithStatus) {
    this.movementChangedListenerWithStatus = movementChangedLisenerWithStatus;
  }

  class EditTextWatcher extends SingleTextWatcher {

    private final LotMovementViewModel viewModel;

    public EditTextWatcher(LotMovementViewModel viewModel) {
      this.viewModel = viewModel;
    }

    @Override
    public void afterTextChanged(Editable editable) {
      this.viewModel.setDataChanged(true);
      viewModel.setQuantity(editable.toString());
      lyLotAmount.setErrorEnabled(false);
      updateVgLotSOHAndError(context);
      if (movementChangeListener != null) {
        movementChangeListener.movementChange();
      }
      if (context instanceof BulkInitialInventoryActivity
          && movementChangedListenerWithStatus != null) {
        movementChangedListenerWithStatus.movementChange(editable.toString());
      }
    }

    @SuppressWarnings("squid:S3776")
    private void updateVgLotSOHAndError(Context context) {
      if (context instanceof NewStockMovementActivity) {
        if (viewModel.isNewAdded()) {
          if (viewModel.validateLotWithPositiveQuantity()) {
            vgLotSOH.setVisibility(View.GONE);
          } else {
            vgLotSOH.setVisibility(View.VISIBLE);
            setQuantityError(getString(R.string.msg_empty_quantity));
          }
        }
        if (!viewModel.validateQuantityNotGreaterThanSOH()) {
          setQuantityError(getString(R.string.msg_invalid_quantity));
        }
      }

      if (context instanceof PhysicalInventoryActivity) {
        if (viewModel.isNewAdded()) {
          if (viewModel.validateLotWithPositiveQuantity()) {
            vgLotSOH.setVisibility(View.GONE);
          } else {
            vgLotSOH.setVisibility(View.VISIBLE);
            setQuantityError(getString(R.string.msg_empty_quantity));
          }
        } else {
          if (!viewModel.validateLotWithNoEmptyFields()) {
            setQuantityError(getString(R.string.msg_empty_quantity));
          }
        }
      }

      if (context instanceof InitialInventoryActivity && !viewModel.validateLotWithPositiveQuantity()) {
        setQuantityError(getString(R.string.msg_empty_quantity));
      }
      if (context instanceof BulkInitialInventoryActivity && !viewModel.validateLotWithPositiveQuantity()) {
        setQuantityError(getString(R.string.msg_empty_quantity));
      }
    }
  }
}
