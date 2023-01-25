package com.microsoft.identity.common

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.microsoft.identity.common.internal.broker.BrokerData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

//TODO: write tests.
class BrokerMetadataCache (val context: Context) : IBrokerMetadataCache {
    companion object {
        val sGson = Gson()
        const val BROKER_METADATA_CACHE_STORE = "BROKER_METADATA_CACHE_STORE"
        const val ACTIVE_BROKER_CACHE_KEY = "ACTIVE_BROKER_CACHE_KEY"
        private val Context.sBrokerMetadataCacheStore: DataStore<Preferences> by preferencesDataStore(name = BROKER_METADATA_CACHE_STORE)
    }

    override fun getCachedActiveBroker(): BrokerData? {
        val key = stringPreferencesKey(ACTIVE_BROKER_CACHE_KEY)
        return runBlocking {
            val data = context.sBrokerMetadataCacheStore.data.first()[key]
            if (data != null){
                return@runBlocking sGson.fromJson(data, BrokerData::class.java)
            } else{
                return@runBlocking null
            }
        }
    }

    override fun setCachedActiveBroker(brokerData: BrokerData) {
        val key = stringPreferencesKey(ACTIVE_BROKER_CACHE_KEY)
        runBlocking {
            context.sBrokerMetadataCacheStore.edit {
                it[key] = sGson.toJson(brokerData, BrokerData::class.java)
            }
        }
    }

    override fun clearCachedActiveBroker() {
        val key = stringPreferencesKey(ACTIVE_BROKER_CACHE_KEY)
        runBlocking {
            context.sBrokerMetadataCacheStore.edit {
                it.remove(key)
            }
        }
    }
}