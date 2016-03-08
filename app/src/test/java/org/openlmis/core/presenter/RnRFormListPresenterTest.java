package org.openlmis.core.presenter;

import android.support.annotation.NonNull;

import com.google.inject.AbstractModule;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openlmis.core.LMISTestApp;
import org.openlmis.core.LMISTestRunner;
import org.openlmis.core.R;
import org.openlmis.core.exceptions.LMISException;
import org.openlmis.core.manager.SharedPreferenceMgr;
import org.openlmis.core.model.Inventory;
import org.openlmis.core.model.Period;
import org.openlmis.core.model.Program;
import org.openlmis.core.model.RnRForm;
import org.openlmis.core.model.SyncError;
import org.openlmis.core.model.SyncType;
import org.openlmis.core.model.builder.ProgramBuilder;
import org.openlmis.core.model.repository.InventoryRepository;
import org.openlmis.core.model.repository.RnrFormRepository;
import org.openlmis.core.model.repository.StockRepository;
import org.openlmis.core.model.repository.SyncErrorsRepository;
import org.openlmis.core.model.repository.VIARepository;
import org.openlmis.core.model.service.PeriodService;
import org.openlmis.core.utils.DateUtil;
import org.openlmis.core.view.viewmodel.RnRFormViewModel;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import roboguice.RoboGuice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(LMISTestRunner.class)
public class RnRFormListPresenterTest {
    RnRFormListPresenter presenter;
    private List<RnRForm> rnRForms;
    private ArrayList<RnRFormViewModel> viewModels;
    SyncErrorsRepository syncErrorsRepository;
    private RnrFormRepository rnrFormRepository;
    private StockRepository stockRepository;
    private SharedPreferenceMgr sharedPreferenceMgr;
    private Period period;

    private PeriodService periodService;
    private long dateJanTwentySix = 1453766400000l;
    private long dateFebTwentyOne = 1456012800000l;
    private InventoryRepository inventoryRepository;

    @Before
    public void setUp() {
        rnrFormRepository = mock(RnrFormRepository.class);
        stockRepository = mock(StockRepository.class);
        syncErrorsRepository = mock(SyncErrorsRepository.class);
        sharedPreferenceMgr = mock(SharedPreferenceMgr.class);
        periodService = mock(PeriodService.class);
        inventoryRepository = mock(InventoryRepository.class);

        RoboGuice.overrideApplicationInjector(RuntimeEnvironment.application, new AbstractModule() {
            @Override
            protected void configure() {
                bind(SyncErrorsRepository.class).toInstance(syncErrorsRepository);
                bind(RnrFormRepository.class).toInstance(rnrFormRepository);
                bind(StockRepository.class).toInstance(stockRepository);
                bind(SharedPreferenceMgr.class).toInstance(sharedPreferenceMgr);
                bind(PeriodService.class).toInstance(periodService);
                bind(InventoryRepository.class).toInstance(inventoryRepository);
            }
        });

        presenter = RoboGuice.getInjector(RuntimeEnvironment.application).getInstance(RnRFormListPresenter.class);
        period = DateUtil.generateRnRFormPeriodBy(new Date());
        rnRForms = createRnRForms();
        viewModels = new ArrayList<>();
    }

    @Test
    public void shouldBuildFormListViewModels() throws LMISException {
        presenter.setProgramCode("MMIA");
        Collections.reverse(rnRForms);
        when(rnrFormRepository.list("MMIA")).thenReturn(rnRForms);
        when(syncErrorsRepository.getBySyncTypeAndObjectId(any(SyncType.class), anyLong()))
                .thenReturn(Arrays.asList(new SyncError("Error1", SyncType.RnRForm, 1), new SyncError("Error2", SyncType.RnRForm, 1)));

        List<RnRFormViewModel> resultViewModels = presenter.buildFormListViewModels();
        assertThat(resultViewModels.size()).isEqualTo(3);
        assertThat(resultViewModels.get(0).getSyncServerErrorMessage()).isEqualTo("Error2");
    }

    @Test
    public void shouldAddPreviousPeriodForm() {
        rnRForms.remove(0);
        presenter.addPreviousPeriodViewModels(viewModels, rnRForms);

        assertThat(viewModels.size()).isEqualTo(2);
    }

    @Test
    public void shouldReturnOneRnrFormViewModleWhenThereIsNoRnrFormAndToggleOn() throws Exception {

        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(period.getBegin().toDate(), DateUtil.DATE_TIME_FORMAT));
        when(rnrFormRepository.list("VIA")).thenReturn(new ArrayList<RnRForm>());
        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);


        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        String periodString = LMISTestApp.getContext().getString(R.string.label_period_date, DateUtil.formatDate(period.getBegin().toDate()), DateUtil.formatDate(period.getEnd().toDate()));

        assertThat(rnRFormViewModels.size()).isEqualTo(1);
        assertThat(rnRFormViewModels.get(0).getPeriod()).isEqualTo(periodString);
        assertThat(rnRFormViewModels.get(0).getName()).isEqualTo(LMISTestApp.getContext().getString(R.string.label_via_name));
    }

    @Test
    public void shouldReturnUnCompleteInventoryTypeRnrFormViewModel() throws Exception {
        when(rnrFormRepository.list("VIA")).thenReturn(new ArrayList<RnRForm>());
        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(DateUtil.generateRnRFormPeriodBy(new Date()).previous().getBegin().toDate(), DateUtil.DATE_TIME_FORMAT));
        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();
        assertThat(rnRFormViewModels.size()).isEqualTo(1);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_UNCOMPLETE_INVENTORY_IN_CURRENT_PERIOD);
    }

    @Test
    public void shouldReturnCanNotCreateRnrTypeRnrFormViewModel() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_requisition_period_logic_change, true);


        long dateFebFourteen = 1455408000000l;
        ((LMISTestApp) RuntimeEnvironment.application).setCurrentTimeMillis(dateFebFourteen);
        when(rnrFormRepository.list("VIA")).thenReturn(new ArrayList<RnRForm>());
        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(DateUtil.generateRnRFormPeriodBy(new Date()).previous().getBegin().toDate(), DateUtil.DATE_TIME_FORMAT));
        when(periodService.generateNextPeriod("VIA", null)).thenReturn(new Period(new DateTime(dateJanTwentySix), new DateTime(dateFebTwentyOne)));

        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        assertThat(rnRFormViewModels.size()).isEqualTo(1);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_CANNOT_DO_MONTHLY_INVENTORY);
    }

    @Test
    public void shouldReturnUnCompleteInventoryTypeRnrFormViewModelWhenTimeAfterInventoryBegin() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_requisition_period_logic_change, true);

        long dateFebEighteen = 1455753600000l;
        ((LMISTestApp) RuntimeEnvironment.application).setCurrentTimeMillis(dateFebEighteen);
        when(rnrFormRepository.list("VIA")).thenReturn(new ArrayList<RnRForm>());
        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(DateUtil.generateRnRFormPeriodBy(new Date()).previous().getBegin().toDate(), DateUtil.DATE_TIME_FORMAT));
        when(periodService.generateNextPeriod("VIA", null)).thenReturn(new Period(new DateTime(dateJanTwentySix), new DateTime(dateFebTwentyOne)));

        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        assertThat(rnRFormViewModels.size()).isEqualTo(1);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_UNCOMPLETE_INVENTORY_IN_CURRENT_PERIOD);
    }

    @Test
    public void shouldReturnCreateFormTypeRnrFormViewModel() throws Exception {
        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(new Date(), DateUtil.DATE_TIME_FORMAT));
        when(rnrFormRepository.list("VIA")).thenReturn(new ArrayList<RnRForm>());
        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();
        assertThat(rnRFormViewModels.size()).isEqualTo(1);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_CLOSE_OF_PERIOD_SELECTED);
    }


    @Test
    public void shouldReturnCreateFormTypeRnrFormViewModelWithoutRnrformInCurrentPeriod() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_requisition_period_logic_change, false);
        ArrayList<RnRForm> rnRForms = new ArrayList<>();

        RnRForm rnRForm = new RnRForm();
        Program program = new Program();
        program.setProgramCode(VIARepository.VIA_PROGRAM_CODE);
        rnRForm.setProgram(program);
        rnRForm.setStatus(RnRForm.STATUS.AUTHORIZED);
        rnRForm.setPeriodBegin(period.previous().getBegin().toDate());
        rnRForm.setPeriodEnd(period.previous().getEnd().toDate());

        rnRForms.add(rnRForm);

        when(rnrFormRepository.list(VIARepository.VIA_PROGRAM_CODE)).thenReturn(rnRForms);
        when(sharedPreferenceMgr.getLatestPhysicInventoryTime()).thenReturn(DateUtil.formatDate(period.previous().getBegin().toDate(), DateUtil.DATE_TIME_FORMAT));
        presenter.setProgramCode(VIARepository.VIA_PROGRAM_CODE);

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        assertThat(rnRFormViewModels.size()).isEqualTo(2);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_UNCOMPLETE_INVENTORY_IN_CURRENT_PERIOD);
        assertThat(rnRFormViewModels.get(1).getType()).isEqualTo(RnRFormViewModel.TYPE_UNSYNCED_HISTORICAL);
    }

    @Test
    public void shouldReturnMissedPeriodViewModelWhenCurrentDateAfterInventoryCloseDateAndHasNotRnR() throws Exception {
        when(periodService.hasMissedPeriod(anyString())).thenReturn(true);
        when(periodService.getMissedPeriodOffsetMonth(anyString())).thenReturn(4);
        when(periodService.getCurrentMonthInventoryBeginDate()).thenReturn(new DateTime(DateUtil.parseString("2016-02-18", DateUtil.DB_DATE_FORMAT)));
        when(inventoryRepository.queryPeriodInventory(any(Period.class))).thenReturn(new ArrayList<Inventory>());
        rnRForms.clear();

        presenter.addPreviousPeriodMissedViewModels(viewModels, rnRForms);
        assertThat(viewModels.size()).isEqualTo(5);
        assertThat(viewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(1).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(2).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(3).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(4).getType()).isEqualTo(RnRFormViewModel.TYPE_FIRST_MISSED_PERIOD);
    }

    @Test
    public void shouldReturnMissedPeriodAndInventoryDoneWhenHasMissedAndHasInventoryInPeriodAndHasNotRnR() throws Exception {
        when(periodService.hasMissedPeriod(anyString())).thenReturn(true);
        when(periodService.getMissedPeriodOffsetMonth(anyString())).thenReturn(4);
        when(periodService.getCurrentMonthInventoryBeginDate()).thenReturn(new DateTime(DateUtil.parseString("2016-02-18", DateUtil.DB_DATE_FORMAT)));
        ArrayList<Inventory> inventories = new ArrayList<>();
        inventories.add(new Inventory());
        when(inventoryRepository.queryPeriodInventory(any(Period.class))).thenReturn(inventories);
        presenter.setProgramCode("VIA");
        rnRForms.clear();

        presenter.addPreviousPeriodMissedViewModels(viewModels, rnRForms);

        assertThat(viewModels.size()).isEqualTo(5);
        assertThat(viewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(1).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(2).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(3).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(4).getType()).isEqualTo(RnRFormViewModel.TYPE_INVENTORY_DONE);
    }

    @Test
    public void shouldReturnMissPeriodWithOutNextPeriodModelWhenHasMissedAndHasUnCompleteRnr() throws Exception {
        when(periodService.hasMissedPeriod(anyString())).thenReturn(true);
        when(periodService.getMissedPeriodOffsetMonth(anyString())).thenReturn(4);
        when(periodService.getCurrentMonthInventoryBeginDate()).thenReturn(new DateTime(DateUtil.parseString("2016-02-18", DateUtil.DB_DATE_FORMAT)));
        ArrayList<Inventory> inventories = new ArrayList<>();
        inventories.add(new Inventory());
        when(inventoryRepository.queryPeriodInventory(any(Period.class))).thenReturn(inventories);
        presenter.setProgramCode("VIA");

        presenter.addPreviousPeriodMissedViewModels(viewModels, rnRForms);

        assertThat(viewModels.size()).isEqualTo(4);
        assertThat(viewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(1).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(2).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
        assertThat(viewModels.get(3).getType()).isEqualTo(RnRFormViewModel.TYPE_MISSED_PERIOD);
    }

    @Test
    public void shouldAddCurrentFormModelIfHasNoRnrInCurrentPeriod() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_requisition_period_logic_change, true);

        Program program = new ProgramBuilder().setProgramCode("VIA").build();
        String programCode = program.getProgramCode();
        presenter.programCode = programCode;
        RnRForm rnRForm1 = createRnrFormByPeriod(RnRForm.STATUS.AUTHORIZED, DateUtil.parseString("2016-02-18", DateUtil.DB_DATE_FORMAT), program);
        RnRForm rnRForm2 = createRnrFormByPeriod(RnRForm.STATUS.AUTHORIZED, DateUtil.parseString("2016-03-18", DateUtil.DB_DATE_FORMAT), program);

        when(rnrFormRepository.list(programCode)).thenReturn(newArrayList(rnRForm1, rnRForm2));
        when(periodService.hasMissedPeriod(programCode)).thenReturn(false);
        when(periodService.generateNextPeriod(programCode, null)).thenReturn(new Period(new DateTime(DateUtil.parseString("2016-04-18", DateUtil.DB_DATE_FORMAT)), new DateTime(DateUtil.parseString("2016-05-20", DateUtil.DB_DATE_FORMAT))));

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        assertThat(rnRFormViewModels.size()).isEqualTo(3);
        assertThat(rnRFormViewModels.get(0).getType()).isEqualTo(RnRFormViewModel.TYPE_UNCOMPLETE_INVENTORY_IN_CURRENT_PERIOD);
    }

    @Test
    public void shouldNotAddCurrentFormModelIfHasRnrInCurrentPeriod() throws Exception {
        LMISTestApp.getInstance().setFeatureToggle(R.bool.feature_requisition_period_logic_change, true);

        Program program = new ProgramBuilder().setProgramCode("VIA").build();
        String programCode = program.getProgramCode();
        presenter.programCode = programCode;
        RnRForm rnRForm1 = createRnrFormByPeriod(RnRForm.STATUS.AUTHORIZED, DateUtil.parseString("2016-02-18", DateUtil.DB_DATE_FORMAT), program);
        RnRForm rnRForm2 = createRnrFormByPeriod(RnRForm.STATUS.AUTHORIZED, DateUtil.parseString("2016-03-18", DateUtil.DB_DATE_FORMAT), program);

        when(rnrFormRepository.list(programCode)).thenReturn(newArrayList(rnRForm1, rnRForm2));
        when(periodService.hasMissedPeriod(programCode)).thenReturn(false);
        when(periodService.generateNextPeriod(programCode, null)).thenReturn(new Period(new DateTime(DateUtil.parseString("2016-03-18", DateUtil.DB_DATE_FORMAT)), new DateTime(DateUtil.parseString("2016-05-20", DateUtil.DB_DATE_FORMAT))));

        List<RnRFormViewModel> rnRFormViewModels = presenter.buildFormListViewModels();

        assertThat(rnRFormViewModels.size()).isEqualTo(2);
    }

    @NonNull
    private RnRForm createRnrFormByPeriod(RnRForm.STATUS status, Date periodBegin, Program program) {
        RnRForm rnRForm1 = new RnRForm();
        rnRForm1.setPeriodBegin(periodBegin);
        rnRForm1.setPeriodEnd(new DateTime(periodBegin).plusDays(30).toDate());
        rnRForm1.setStatus(status);
        rnRForm1.setProgram(program);
        return rnRForm1;
    }

    private List<RnRForm> createRnRForms() {
        return newArrayList(createRnRForm(RnRForm.STATUS.DRAFT), createRnRForm(RnRForm.STATUS.AUTHORIZED), createRnRForm(RnRForm.STATUS.AUTHORIZED));
    }

    private RnRForm createRnRForm(RnRForm.STATUS status) {
        Program program = new Program();
        program.setProgramCode("MMIA");
        program.setProgramName("MMIA");

        RnRForm rnRForm = RnRForm.init(program, DateUtil.today());
        rnRForm.setId(1L);
        rnRForm.setStatus(status);
        rnRForm.setSynced(true);
        return rnRForm;
    }

}