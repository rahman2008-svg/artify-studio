package com.example.data.database

import kotlinx.coroutines.flow.Flow

class DesignRepository(private val designDraftDao: DesignDraftDao) {
    val allDrafts: Flow<List<DesignDraft>> = designDraftDao.getAllDrafts()

    suspend fun getDraftById(id: Int): DesignDraft? {
        return designDraftDao.getDraftById(id)
    }

    suspend fun insertDraft(draft: DesignDraft): Long {
        return designDraftDao.insertDraft(draft)
    }

    suspend fun deleteDraftById(id: Int) {
        designDraftDao.deleteDraftById(id)
    }
}
