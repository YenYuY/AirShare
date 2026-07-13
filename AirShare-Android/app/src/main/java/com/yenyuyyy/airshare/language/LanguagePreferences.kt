package com.yenyuyyy.airshare.language

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.airSharePreferences by preferencesDataStore(name = "airshare_preferences")

class LanguagePreferences(private val context: Context) {
    private val languageKey = stringPreferencesKey("language_option")

    val languageOption: Flow<AppLanguageOption> = context.airSharePreferences.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            preferences[languageKey]
                ?.let { stored -> runCatching { AppLanguageOption.valueOf(stored) }.getOrNull() }
                ?: AppLanguageOption.AUTO
        }

    suspend fun setLanguageOption(option: AppLanguageOption) {
        context.airSharePreferences.edit { preferences ->
            preferences[languageKey] = option.name
        }
    }
}
