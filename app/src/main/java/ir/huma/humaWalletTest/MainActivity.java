package ir.huma.humaWalletTest;

import android.app.Activity;
import android.os.Bundle;

import ir.huma.humawallet.lib.HumaWallet;

public class MainActivity extends Activity {

    private HumaWallet humaWallet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /**
         * Get temporaryToken from your server.
         * Your server must call Huma Wallet Service to get temporaryToken.
         */
        humaWallet = new HumaWallet(this)
                .setPaymentToken("your new temeporaryToken")
                .setOnPayListener(new HumaWallet.OnPayListener() {
                    @Override
                    public void onPayComplete(String code) {
                        // Verify payment success with your server.
                    }

                    @Override
                    public void onPayFail(String message) {
                        // Payment was cancelled or failed.
                    }
                });

        humaWallet.send();
    }

    @Override
    protected void onDestroy() {
        if (humaWallet != null) {
            humaWallet.unregister();
        }
        super.onDestroy();
    }
}
