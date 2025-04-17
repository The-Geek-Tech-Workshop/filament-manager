package com.gtw.filamentmanager.data

import android.nfc.Tag
import com.gtw.filamentmanager.data.bambu.BambuFilamentSpoolWriter
import com.gtw.filamentmanager.model.domain.FilamentSpool
import com.gtw.filamentmanager.model.domain.TagFormat

interface FilamentSpoolWriter<F : FilamentSpool> {

    val tagFormat: TagFormat

    suspend fun write(tag: Tag, filamentSpool: F)

}

class FilamentSpoolWriterFactory() {
    private val writers = listOf<FilamentSpoolWriter<out FilamentSpool>>(
        BambuFilamentSpoolWriter()
    )

    fun create(tagFormat: TagFormat): FilamentSpoolWriter<out FilamentSpool>? =
        writers.firstOrNull { it.tagFormat == tagFormat }
}