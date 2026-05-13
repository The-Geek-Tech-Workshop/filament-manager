package com.gtw.filamentmanager.data

import android.nfc.Tag
import com.gtw.filamentmanager.data.bambu.BambuFilamentSpoolParser
import com.gtw.filamentmanager.model.domain.FilamentSpool
import javax.inject.Inject


interface FilamentSpoolParser<out F : FilamentSpool> {

    suspend fun canParseTag(tag: Tag): Boolean

    suspend fun parse(tag: Tag): F
}

class FilamentSpoolParserFactory @Inject constructor(
    bambuFilamentSpoolParser: BambuFilamentSpoolParser
) {
    private val parsers = listOf<FilamentSpoolParser<FilamentSpool>>(
        bambuFilamentSpoolParser
    )

    suspend fun create(tag: Tag): FilamentSpoolParser<FilamentSpool>? =
        parsers.firstOrNull { it.canParseTag(tag) }

}