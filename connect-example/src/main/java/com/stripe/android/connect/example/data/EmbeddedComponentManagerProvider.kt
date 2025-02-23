package com.stripe.android.connect.example.data

import com.github.kittinunf.fuel.core.FuelError
import com.stripe.android.connect.EmbeddedComponentManager
import com.stripe.android.connect.FetchClientSecretCallback.ClientSecretResultCallback
import com.stripe.android.connect.PrivateBetaConnectSDK
import com.stripe.android.core.BuildConfig
import com.stripe.android.core.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(PrivateBetaConnectSDK::class)
@Singleton
class EmbeddedComponentManagerProvider @Inject constructor(
    private val embeddedComponentService: EmbeddedComponentService,
    private val settingsService: SettingsService,
) {

    // this factory manages the EmbeddedComponentManager instance, since it needs to wait for
    // a publishable key to be received from the backend before building it.
    // In the future it may manage multiple instances if needed.
    private var embeddedComponentManager: EmbeddedComponentManager? = null

    private val loggingTag = this::class.java.name
    private val logger: Logger = Logger.getInstance(enableLogging = BuildConfig.DEBUG)
    private val ioScope: CoroutineScope by lazy { CoroutineScope(Dispatchers.IO) }

    /**
     * Provides the EmbeddedComponentManager instance, creating it if it doesn't exist.
     * Throws [IllegalStateException] if an EmbeddedComponentManager cannot be created at this time.
     */
    fun provideEmbeddedComponentManager(): EmbeddedComponentManager {
        if (embeddedComponentManager != null) {
            return embeddedComponentManager!!
        }

        // TODO - handle app restoration where publishableKey may be null when this is called
        val publishableKey = embeddedComponentService.publishableKey.value
            ?: throw IllegalStateException("Publishable key must be set before creating EmbeddedComponentManager")

        return EmbeddedComponentManager(
            configuration = EmbeddedComponentManager.Configuration(
                publishableKey = publishableKey,
            ),
            fetchClientSecretCallback = ::fetchClientSecret,
        ).also {
            embeddedComponentManager = it
        }
    }

    /**
     * Helper wrapper around [fetchClientSecret] that fetches the client secret
     */
    @OptIn(PrivateBetaConnectSDK::class)
    private fun fetchClientSecret(clientSecretResultCallback: ClientSecretResultCallback) {
        val account: String = settingsService.getSelectedMerchant()
            ?: return clientSecretResultCallback.onResult(null)

        ioScope.launch {
            try {
                val clientSecret = embeddedComponentService.fetchClientSecret(account)
                clientSecretResultCallback.onResult(clientSecret)
            } catch (e: FuelError) {
                logger.error("($loggingTag) Failed to fetch client secret", e)
                clientSecretResultCallback.onResult(null)
            } catch (e: IOException) {
                logger.error("($loggingTag) Failed to fetch client secret", e)
                clientSecretResultCallback.onResult(null)
            }
        }
    }
}
