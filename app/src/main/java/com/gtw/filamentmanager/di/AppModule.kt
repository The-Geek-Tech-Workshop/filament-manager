package com.gtw.filamentmanager.di

import android.content.Context
import com.gtw.filamentmanager.data.Hkdf
import com.gtw.filamentmanager.data.PrinterAuthenticationDetailsRepoSharedPreferences
import com.gtw.filamentmanager.data.bambu.BambuPrinterRepoSsdp
import com.gtw.filamentmanager.data.bambu.PrinterConnectorMqtt
import com.gtw.filamentmanager.model.repos.PrinterAuthenticationDetailsRepo
import com.gtw.filamentmanager.model.repos.PrinterConnector
import com.gtw.filamentmanager.model.repos.PrinterRepo
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.inject.Singleton
import javax.net.ssl.TrustManagerFactory
import kotlin.coroutines.CoroutineContext

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModuleBindings {

    @Binds
    abstract fun bindPrinterAuthenticationDetailsRepo(
        printerAuthenticationDetailsRepoSharedPreferences: PrinterAuthenticationDetailsRepoSharedPreferences
    ): PrinterAuthenticationDetailsRepo

    @Binds
    abstract fun bindPrinterRepo(
        printerRepoSsdp: BambuPrinterRepoSsdp
    ): PrinterRepo

    @Binds
    abstract fun bindFilamentTrayRepo(
        filamentTrayRepoMqtt: PrinterConnectorMqtt
    ): PrinterConnector


}

class DataCoroutineScope(override val coroutineContext: CoroutineContext = Dispatchers.IO) :
    CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataCoroutineScope(): DataCoroutineScope = DataCoroutineScope()

    @Provides
    @Singleton
    fun provideHkdf(): Hkdf = Hkdf.getInstance("HmacSHA256")

    @Provides
    @Singleton
    // A trust manager that trusts the Bambu CA certificate
    fun provideTrustManagerFactory(
        @ApplicationContext context: Context
    ): TrustManagerFactory = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setCertificateEntry(
                    "bambu local mqtt",
                    CertificateFactory.getInstance("X.509")
                        .generateCertificate(
                            context.assets.open("ca_cert.pem")
                        )
                )
            })
        }
}