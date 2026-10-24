package com.rms.master;

/**
 * Technical keys of lookup TYPES and VALUES that workflow code must refer to.
 * These are plain strings resolved against ref_lookup_type / ref_lookup_value (never Java enums);
 * the rows themselves are maintained as master data.
 */
public final class LookupCodes {
    private LookupCodes() { }

    public static final String TREATY_STATUS = "TREATY_STATUS";
    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String RUN_OFF = "RUN_OFF";
    public static final String CLOSED = "CLOSED";
    public static final String SUPERSEDED = "SUPERSEDED";

    public static final String MC_STATUS = "MC_STATUS";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String RETURNED = "RETURNED";

    public static final String MC_ENTITY = "MC_ENTITY";
    public static final String ENTITY_TREATY = "TREATY";
    public static final String MC_ACTION = "MC_ACTION";
    public static final String ACTION_ACTIVATE = "ACTIVATE";

    public static final String AMEND_MODE = "AMEND_MODE";
    public static final String PROSPECTIVE = "PROSPECTIVE";

    public static final String RISK_GRADE = "RISK_GRADE";
    /** Policy transaction type of erp_policy_txn (NEW_BUSINESS, RENEWAL, ENDORSEMENT, CANCELLATION). */
    public static final String TXN_TYPE = "TXN_TYPE";
    public static final String REINSTATEMENT_BASIS = "REINSTATEMENT_BASIS";
    public static final String ALLOCATION_METHOD = "ALLOCATION_METHOD";
    public static final String ALLOCATION_BASIS = "ALLOCATION_BASIS";
    public static final String RESIDUAL_ACTION = "RESIDUAL_ACTION";
    public static final String R_CONTINUE = "CONTINUE";
    public static final String LR_BASIS = "LR_BASIS";

    /** Calc-strategy bean names stored in treaty_type.calc_strategy_code. */
    public static final String STRAT_QS = "QuotaShareCalculator";
    public static final String STRAT_SURPLUS = "SurplusCalculator";
    public static final String STRAT_XOL_RISK = "XolPerRiskCalculator";
    public static final String STRAT_XOL_CAT = "XolCatCalculator";
    public static final String STRAT_STOP = "StopLossCalculator";
    public static final String STRAT_FAC_OBL = "FacObligatoryCalculator";
    public static final String STRAT_FAC_RES = "FacResidualCalculator";

    /** Commission type codes that need extra configuration. */
    public static final String COMM_FLAT = "FLAT";
    public static final String COMM_SLIDING = "SLIDING";
}
