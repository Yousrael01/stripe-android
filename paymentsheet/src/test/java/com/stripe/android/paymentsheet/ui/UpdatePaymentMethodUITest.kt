package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.ViewActionRecorder
import com.stripe.android.ui.core.elements.TEST_TAG_DIALOG_CONFIRM_BUTTON
import com.stripe.android.ui.core.elements.TEST_TAG_SIMPLE_DIALOG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class UpdatePaymentMethodUITest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingExpiryDate_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = null,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryMonth_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = -1,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun invalidExpiryYear_displaysDots() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryYear = 202,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "••/••"
            )
        }
    }

    @Test
    fun singleDigitExpiryMonth_hasLeadingZero() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = 8,
                expiryYear = 2029,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "08/29"
            )
        }
    }

    @Test
    fun doubleDigitExpiryMonth_doesNotHaveLeadingZero() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                expiryMonth = 11,
                expiryYear = 2029,
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertExpiryDateEquals(
                "11/29"
            )
        }
    }

    @Test
    fun threeDigitCvcCardBrand_displaysThreeDotsForCvc() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                brand = CardBrand.Visa
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertCvcEquals(
                "•••"
            )
        }
    }

    @Test
    fun fourDigitCvcCardBrand_displaysFourDotsForCvc() {
        val card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card!!.copy(
                brand = CardBrand.AmericanExpress
            )
        )

        runScenario(displayableSavedPaymentMethod = card.toDisplayableSavedPaymentMethod()) {
            assertCvcEquals(
                "••••"
            )
        }
    }

    @Test
    fun canRemoveIsFalse_removeButtonHidden() = runScenario(
        canRemove = false,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun canRemoveIsTrue_removeButtonHidden() = runScenario(
        canRemove = true,
    ) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertExists()
    }

    @Test
    fun clickingRemoveButton_displaysDialog_deletesOnConfirm() = runScenario(canRemove = true) {
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).assertExists()
        composeRule.onNodeWithTag(UPDATE_PM_REMOVE_BUTTON_TEST_TAG).performClick()

        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).assertExists()
        composeRule.onNodeWithTag(TEST_TAG_DIALOG_CONFIRM_BUTTON).performClick()

        viewActionRecorder.consume(
            UpdatePaymentMethodInteractor.ViewAction.RemovePaymentMethod
        )
        assertThat(viewActionRecorder.viewActions).isEmpty()
        composeRule.onNodeWithTag(TEST_TAG_SIMPLE_DIALOG).assertDoesNotExist()
    }

    private fun assertExpiryDateEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_EXPIRY_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private fun assertCvcEquals(text: String) {
        composeRule.onNodeWithTag(UPDATE_PM_CVC_FIELD_TEST_TAG).assertTextContains(
            text
        )
    }

    private class FakeUpdatePaymentMethodInteractor(
        override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
        override val canRemove: Boolean,
        val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>,
    ) : UpdatePaymentMethodInteractor {
        override val isLiveMode: Boolean = false
        override val card: PaymentMethod.Card = displayableSavedPaymentMethod.paymentMethod.card!!

        override fun handleViewAction(viewAction: UpdatePaymentMethodInteractor.ViewAction) {
            viewActionRecorder.record(viewAction)
        }
    }

    private fun runScenario(
        displayableSavedPaymentMethod: DisplayableSavedPaymentMethod = PaymentMethodFixtures.displayableCard(),
        canRemove: Boolean = true,
        testBlock: Scenario.() -> Unit,
    ) {
        val viewActionRecorder = ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>()
        val interactor = FakeUpdatePaymentMethodInteractor(
            displayableSavedPaymentMethod = displayableSavedPaymentMethod,
            canRemove = canRemove,
            viewActionRecorder = viewActionRecorder,
        )

        composeRule.setContent {
            UpdatePaymentMethodUI(interactor = interactor, modifier = Modifier)
        }

        Scenario(viewActionRecorder).apply(testBlock)
    }

    private data class Scenario(
        val viewActionRecorder: ViewActionRecorder<UpdatePaymentMethodInteractor.ViewAction>,
    )
}
