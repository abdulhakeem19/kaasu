package com.kaasu.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaasu.app.core.database.dao.SmsSenderDao
import com.kaasu.app.core.database.entity.SmsSenderEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsSourcesViewModel @Inject constructor(
    private val smsSenderDao: SmsSenderDao,
) : ViewModel() {

    val smsSenders = smsSenderDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleSender(entity: SmsSenderEntity, enabled: Boolean) {
        viewModelScope.launch { smsSenderDao.update(entity.copy(isEnabled = enabled)) }
    }
}
