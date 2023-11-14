package com.microsoft.identity.common.java.opentelemetry.perf;

import lombok.NonNull;

public enum PerfOperation {
    load_all_accounts(PerfOperationType.account_manager),
    load_wpj_entry(PerfOperationType.wpj_data),
    foci_request(PerfOperationType.network),
    drs_discovery(PerfOperationType.network),
    acquire_at(PerfOperationType.network),
    acquire_nonce(PerfOperationType.network),
    acquire_prt(PerfOperationType.network),
    cache_get_all_client_ids(PerfOperationType.shared_preferences_cache),
    cache_clear_all(PerfOperationType.shared_preferences_cache),
    cache_remove_account(PerfOperationType.shared_preferences_cache),
    cache_get_account_by_home_account_id(PerfOperationType.shared_preferences_cache),
    cache_get_id_tokens_for_account_record(PerfOperationType.shared_preferences_cache),
    cache_get_all_tenant_accounts_for_account_by_client_id(PerfOperationType.shared_preferences_cache),
    cache_get_accounts(PerfOperationType.shared_preferences_cache),
    cache_get_account_with_aggregated_account_data_by_local_account_id(PerfOperationType.shared_preferences_cache),
    cache_get_account_by_local_account_id(PerfOperationType.shared_preferences_cache),
    cache_get_accounts_with_aggregated_account_data(PerfOperationType.shared_preferences_cache),
    cache_get_account(PerfOperationType.shared_preferences_cache),
    cache_remove_credential(PerfOperationType.shared_preferences_cache),
    cache_save_and_load_aggregated_account_data(PerfOperationType.shared_preferences_cache),
    cache_load_with_aggregated_account_data(PerfOperationType.shared_preferences_cache),
    cache_load_aggregated_account_data(PerfOperationType.shared_preferences_cache),
    cache_load(PerfOperationType.shared_preferences_cache),
    cache_save(PerfOperationType.shared_preferences_cache),
    sign_prt_jwt(PerfOperationType.key_operation),
    get_mam_enrollment_id(PerfOperationType.ipc),
    perform_cloud_discovery(PerfOperationType.network),
    is_authorized_to_share_tokens(PerfOperationType.tsl),
    encrypt(PerfOperationType.key_operation),
    decrypt(PerfOperationType.key_operation),
    command_execution(PerfOperationType.other),
    auth_sdk_operation(PerfOperationType.other);

    @NonNull
    private final PerfOperationType mPerfOperationType;

    PerfOperation(@NonNull final PerfOperationType perfOperationType) {
        mPerfOperationType = perfOperationType;
    }

    public PerfOperationType getPerfOperationType() {
        return mPerfOperationType;
    }

    public enum PerfOperationType {
        shared_preferences_cache,
        account_manager,
        wpj_data,
        network,
        key_operation,
        ipc,
        tsl,
        other
    }
}