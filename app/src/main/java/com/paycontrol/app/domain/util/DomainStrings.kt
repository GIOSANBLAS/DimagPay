package com.paycontrol.app.domain.util

import android.content.Context
import android.content.res.Resources
import com.paycontrol.app.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mensajes de dominio localizados (repositorios).
 * Evita literales hardcodeados en reglas de negocio visibles al usuario.
 */
@Singleton
class DomainStrings @Inject constructor(
    private val res: Resources
) {
    constructor(context: Context) : this(context.resources)

    fun accountNotFound() = res.getString(R.string.error_account_not_found)
    fun accountNameRequired() = res.getString(R.string.error_account_name_required)
    fun accountNameTooLong() = res.getString(R.string.error_account_name_too_long)
    fun accountTypeInvalid() = res.getString(R.string.error_account_type_invalid)
    fun initialBalanceNegative() = res.getString(R.string.error_initial_balance_negative)
    fun cannotDeleteAccount(name: String, count: Int) =
        res.getQuantityString(R.plurals.error_cannot_delete_account, count, name, count)

    fun clientNotFound() = res.getString(R.string.error_client_not_found)
    fun clientNameRequired() = res.getString(R.string.error_client_name_required)
    fun clientNameTooLong() = res.getString(R.string.error_client_name_too_long)
    fun initialDebtNegative() = res.getString(R.string.error_initial_debt_negative)
    fun paymentMustBePositive() = res.getString(R.string.error_payment_must_be_positive)
    fun depositMustBePositive() = res.getString(R.string.error_deposit_must_be_positive)
    fun amountMustBePositive() = res.getString(R.string.error_amount_must_be_positive)
    fun depositExceedsDebt(formattedDebt: String) =
        res.getString(R.string.error_deposit_exceeds_debt, formattedDebt)

    fun supplierNotFound() = res.getString(R.string.error_supplier_not_found)
    fun supplierNameRequired() = res.getString(R.string.error_supplier_name_required)
    fun supplierNameTooLong() = res.getString(R.string.error_supplier_name_too_long)

    fun transactionNotFound() = res.getString(R.string.error_transaction_not_found)
    fun categoryRequired() = res.getString(R.string.error_category_required)
    fun invalidTransactionType() = res.getString(R.string.error_invalid_transaction_type)
    fun invalidType() = res.getString(R.string.error_invalid_type)
    fun invalidTransferType() = res.getString(R.string.error_invalid_transfer_type)
    fun transferSameAccount() = res.getString(R.string.error_transfer_same_account)
    fun sourceAccountNotFound() = res.getString(R.string.error_source_account_not_found)
    fun destAccountNotFound() = res.getString(R.string.error_dest_account_not_found)
    fun insufficientBalance(accountName: String, balanceCents: Long) =
        res.getString(
            R.string.error_insufficient_balance,
            accountName,
            Money.format(balanceCents)
        )
    fun insufficientBalanceRevert(accountName: String, balanceCents: Long) =
        res.getString(
            R.string.error_insufficient_balance_revert,
            accountName,
            Money.format(balanceCents)
        )
    fun cannotRevertSupplierPayment() =
        res.getString(R.string.error_cannot_revert_supplier_payment)
}
