package com.braintreepayments.api.localpayment

import com.braintreepayments.api.core.PostalAddress
import org.json.JSONException
import org.json.JSONObject

/**
 * Builder used to construct an local payment request.
 */
class LocalPaymentRequest
/**
 * @param hasUserLocationConsent is an optional parameter that informs the SDK
 * if your application has obtained consent from the user to collect location data in compliance with
 * [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/10144311#personal-sensitive)
 * This flag enables PayPal to collect necessary information required for Fraud Detection and Risk Management.
 *
 * @see [User Data policies for the Google Play Developer Program ](https://support.google.com/googleplay/android-developer/answer/10144311.personal-sensitive)
 *
 * @see [Examples of prominent in-app disclosures](https://support.google.com/googleplay/android-developer/answer/9799150?hl=en.Prominent%20in-app%20disclosure)
 */(private val hasUserLocationConsent: Boolean) {
    /**
     * @param address Optional - The address of the customer. An error will occur if this address is not valid.
     */
    @JvmField
    var address: PostalAddress? = null

    /**
     * @param amount Optional - The amount for the transaction.
     */
    @JvmField
    var amount: String? = null

    /**
     * @param bankIdentificationCode Optional - the Bank Identification Code of the customer (specific to iDEAL transactions).
     */
    var bic: String? = null

    /**
     * @param currencyCode Optional - A valid ISO currency code to use for the transaction. Defaults to merchant
     * currency code if not set.
     */
    @JvmField
    var currencyCode: String? = null

    /**
     * @param displayName Optional - The merchant name displayed inside of the Local Payment flow
     * when starting the payment.
     */
    @JvmField
    var displayName: String? = null

    /**
     * @param email Optional - Payer email of the customer.
     */
    @JvmField
    var email: String? = null

    /**
     * @param givenName Optional - Given (first) name of the customer.
     */
    @JvmField
    var givenName: String? = null

    /**
     * @param merchantAccountId Optional - A non-default merchant account to use for tokenization.
     */
    @JvmField
    var merchantAccountId: String? = null

    /**
     * @param paymentType - The type of payment
     */
    @JvmField
    var paymentType: String? = null

    /**
     * @param paymentTypeCountryCode The country code of the local payment. This value must be one of
     * the supported country codes for a given local payment type listed.
     * For local payments supported in multiple countries, this value
     * may determine which banks are presented to the customer.
     * @see [Supported Country Codes](https://developer.paypal.com/braintree/docs/guides/local-payment-methods/client-side-custom/android/v4.invoke-payment-flow)
     */
    @JvmField
    var paymentTypeCountryCode: String? = null

    /**
     * @param phone Optional - Phone number of the customer.
     */
    @JvmField
    var phone: String? = null

    /**
     * @param shippingAddressRequired - Indicates whether or not the payment needs to be shipped. For digital goods,
     * this should be false. Defaults to false.
     */
    var isShippingAddressRequired: Boolean = false

    /**
     * @param surname Optional - Surname (last name) of the customer.
     */
    @JvmField
    var surname: String? = null

    fun hasUserLocationConsent(): Boolean {
        return hasUserLocationConsent
    }

    fun build(returnUrl: String?, cancelUrl: String?): String {
        try {
            val payload = JSONObject()
                .put(INTENT_KEY, "sale")
                .put(RETURN_URL_KEY, returnUrl)
                .put(CANCEL_URL_KEY, cancelUrl)
                .put(FUNDING_SOURCE_KEY, paymentType)
                .put(AMOUNT_KEY, amount)
                .put(CURRENCY_CODE_KEY, currencyCode)
                .put(GIVEN_NAME_KEY, givenName)
                .put(SURNAME_KEY, surname)
                .put(EMAIL_KEY, email)
                .put(PHONE_KEY, phone)
                .put(MERCHANT_ACCOUNT_ID_KEY, merchantAccountId)
                .putOpt(PAYMENT_TYPE_COUNTRY_CODE_KEY, paymentTypeCountryCode)
                .putOpt(BIC_KEY, bic)

            if (address != null) {
                payload.put(STREET_ADDRESS_KEY, address!!.streetAddress)
                    .put(EXTENDED_ADDRESS_KEY, address!!.extendedAddress)
                    .put(LOCALITY_KEY, address!!.locality)
                    .put(REGION_KEY, address!!.region)
                    .put(POSTAL_CODE_KEY, address!!.postalCode)
                    .put(COUNTRY_CODE_KEY, address!!.countryCodeAlpha2)
            }

            val experienceProfile = JSONObject()
            experienceProfile.put(NO_SHIPPING_KEY, !isShippingAddressRequired)
            experienceProfile.put(DISPLAY_NAME_KEY, displayName)
            payload.put(EXPERIENCE_PROFILE_KEY, experienceProfile)

            return payload.toString()
        } catch (ignored: JSONException) {
        }

        return JSONObject().toString()
    }

    companion object {
        private const val INTENT_KEY = "intent"
        private const val RETURN_URL_KEY = "returnUrl"
        private const val CANCEL_URL_KEY = "cancelUrl"
        private const val EXPERIENCE_PROFILE_KEY = "experienceProfile"
        private const val NO_SHIPPING_KEY = "noShipping"
        private const val DISPLAY_NAME_KEY = "brandName"
        private const val FUNDING_SOURCE_KEY = "fundingSource"
        private const val AMOUNT_KEY = "amount"
        private const val CURRENCY_CODE_KEY = "currencyIsoCode"
        private const val GIVEN_NAME_KEY = "firstName"
        private const val SURNAME_KEY = "lastName"
        private const val EMAIL_KEY = "payerEmail"
        private const val PHONE_KEY = "phone"
        private const val STREET_ADDRESS_KEY = "line1"
        private const val EXTENDED_ADDRESS_KEY = "line2"
        private const val LOCALITY_KEY = "city"
        private const val REGION_KEY = "state"
        private const val POSTAL_CODE_KEY = "postalCode"
        private const val COUNTRY_CODE_KEY = "countryCode"
        private const val MERCHANT_ACCOUNT_ID_KEY = "merchantAccountId"
        private const val PAYMENT_TYPE_COUNTRY_CODE_KEY = "paymentTypeCountryCode"
        private const val BIC_KEY = "bic"
    }
}
