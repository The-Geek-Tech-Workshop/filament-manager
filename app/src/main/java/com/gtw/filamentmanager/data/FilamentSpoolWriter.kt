package com.gtw.filamentmanager.data

import android.nfc.Tag
import com.gtw.filamentmanager.data.bambu.BambuFilamentSpoolWriter
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool
import com.gtw.filamentmanager.model.domain.FilamentSpool
import javax.inject.Inject

interface FilamentSpoolWriter<F : FilamentSpool> {

    suspend fun write(tag: Tag, filamentSpool: F)

}

class FilamentSpoolWriterFactory @Inject constructor(
    private val bambuFilamentSpoolWriter: BambuFilamentSpoolWriter
) {

    suspend fun <F : FilamentSpool> write(tag: Tag, filamentSpool: F) {
        when (filamentSpool) {
            is BambuFilamentSpool -> bambuFilamentSpoolWriter.write(tag, filamentSpool)
            else -> null
        }
    }
}