package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;
import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;
import com.samsung.android.sdk.samsungpay.v2.payment.sheet.CustomSheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.braintreepayments.api.SamsungPayMapAcceptedCardBrands.mapToSamsungPayCardBrands;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_READY;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_NOT_SUPPORTED;
import static com.samsung.android.sdk.samsungpay.v2.SpaySdk.SPAY_READY;

public class SamsungPayClient {

    private final BraintreeClient braintreeClient;

    @VisibleForTesting
    SamsungPayInternalClient internalClient;

    public SamsungPayClient(BraintreeClient braintreeClient) {
        this.braintreeClient = braintreeClient;
    }

    /**
     * Forwards the user to the Samsung Pay update page.
     * This should be invoked when Samsung Pay returns a {@link SamsungPayError#SAMSUNG_PAY_APP_NEEDS_UPDATE}
     * error code from {@link SamsungPayClient#isReadyToPay(SamsungPayIsReadyToPayCallback)}
     */
    public void goToUpdatePage() {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.goToSamsungPayUpdatePage();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.goto-update-page");
                } else {
                    // TODO: determine if we should notify an error here
                }
            }
        });
    }

    /**
     * Forwards the user to the Samsung Pay activate page.
     * This should be invoked when Samsung Pay returns a {@link SamsungPayError#SAMSUNG_PAY_SETUP_NOT_COMPLETED}
     * error code from {@link SamsungPayClient#isReadyToPay(SamsungPayIsReadyToPayCallback)}
     */
    public void activateSamsungPay() {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.activateSamsungPay();
                    braintreeClient.sendAnalyticsEvent("samsung-pay.activate-samsung-pay");
                } else {
                    // TODO: determine if we should notify an error here
                }
            }
        });
    }

    /**
     * Call isReadyToPay before starting your Samsung Pay flow. isReadyToPay will call you back with the
     * status of Samsung Pay. If if the status of Samsung Pay is anything but [SamsungPayStatus.SPAY_READY],
     * the listener will be called back with a value of false. If the Samsung Pay callback returns an error,
     * see {@link SamsungPayError} for a list of possible error codes
     * @param callback isReadyToPay callback
     */
    public void isReadyToPay(final SamsungPayIsReadyToPayCallback callback) {
        getSamsungPayStatus(new GetSamsungPayStatusCallback() {
            @Override
            public void onResult(@Nullable Integer status, @Nullable Exception samsungPayError) {
                if (status != null) {
                    if (status == SPAY_READY) {
                        getBraintreeSupportedSamsungPayCards(new GetAcceptedCardBrandsCallback() {
                            @Override
                            public void onResult(@Nullable List<SpaySdk.Brand> acceptedCardBrands, @Nullable Exception error) {
                                if (acceptedCardBrands != null) {
                                    boolean isReadyToPay = !acceptedCardBrands.isEmpty();

                                    if (isReadyToPay) {
                                        braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.ready");
                                        callback.onResult(true, null);
                                    } else {
                                        braintreeClient.sendAnalyticsEvent("samsung-pay.request-card-info.no-supported-cards-in-wallet");
                                        Exception noCardsInWalletError =
                                            new SamsungPayException(SamsungPayError.SAMSUNG_PAY_NO_SUPPORTED_CARDS_IN_WALLET);
                                        callback.onResult(false, noCardsInWalletError);
                                    }
                                } else {
                                    callback.onResult(false, error);
                                }
                            }
                        });
                    } else {
                        switch (status) {
                            case SPAY_NOT_READY:
                                braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.not-ready");
                                break;
                            case SPAY_NOT_SUPPORTED:
                                braintreeClient.sendAnalyticsEvent("samsung-pay.is-ready-to-pay.device-not-supported");
                                break;
                        }

                        // still callback error here; it may contain additional information about why Samsung Pay is not enabled
                        callback.onResult(false, samsungPayError);
                    }
                } else {
                    callback.onResult(false, samsungPayError);
                }
            }
        });
    }

    private void getSamsungPayStatus(final GetSamsungPayStatusCallback callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.getSamsungPayStatus(callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    private void getBraintreeSupportedSamsungPayCards(final GetAcceptedCardBrandsCallback callback) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable final SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.getAcceptedCardBrands(callback);
                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    public void startSamsungPay(final CustomSheetPaymentInfo paymentInfo, final SamsungPayStartListener listener) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.startSamsungPay(paymentInfo, listener);
                } else if (error != null) {
                    listener.onSamsungPayStartError(error);
                }
            }
        });
    }

    public void buildCustomSheetPaymentInfo(final BuildCustomSheetPaymentInfoCallback callback) {
        braintreeClient.getConfiguration(new ConfigurationCallback() {
            @Override
            public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                if (configuration != null) {
                    Set<SpaySdk.Brand> acceptedCardBrands =
                        mapToSamsungPayCardBrands(configuration.getSamsungPaySupportedCardBrands());

                    CustomSheetPaymentInfo.Builder builder = new CustomSheetPaymentInfo.Builder()
                            .setMerchantName(configuration.getSamsungPayMerchantDisplayName())
                            .setMerchantId(configuration.getSamsungPayAuthorization())
                            .setAllowedCardBrands(new ArrayList<>(acceptedCardBrands));

                    braintreeClient.sendAnalyticsEvent("samsung-pay.create-payment-info.success");
                    callback.onResult(builder, null);

                } else {
                    callback.onResult(null, error);
                }
            }
        });
    }

    public void updateCustomSheet(final CustomSheet customSheet) {
        getInternalClient(new GetSamsungPayInternalClientCallback() {
            @Override
            public void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error) {
                if (internalClient != null) {
                    internalClient.updateCustomSheet(customSheet);
                }
            }
        });
    }

    @VisibleForTesting
    void getInternalClient(final GetSamsungPayInternalClientCallback callback) {
        if (internalClient != null) {
            callback.onResult(internalClient, null);
        } else {
            braintreeClient.getConfiguration(new ConfigurationCallback() {
                @Override
                public void onResult(@Nullable Configuration configuration, @Nullable Exception error) {
                    if (configuration != null) {
                        internalClient =
                            new SamsungPayInternalClient(braintreeClient, configuration);
                        braintreeClient.sendAnalyticsEvent("samsung-pay.create-payment-manager.success");
                        callback.onResult(internalClient, null);
                    } else {
                        callback.onResult(null, error);
                    }
                }
            });
        }
    }
}
