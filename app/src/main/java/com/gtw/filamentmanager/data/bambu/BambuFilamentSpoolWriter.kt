package com.gtw.filamentmanager.data.bambu

import android.nfc.Tag
import com.gtw.filamentmanager.data.FilamentSpoolWriter
import com.gtw.filamentmanager.model.domain.BambuFilamentSpool

class BambuFilamentSpoolWriter : FilamentSpoolWriter<BambuFilamentSpool> {
    override suspend fun write(
        tag: Tag,
        filamentSpool: BambuFilamentSpool
    ) {
        TODO("Not yet implemented")
    }
}