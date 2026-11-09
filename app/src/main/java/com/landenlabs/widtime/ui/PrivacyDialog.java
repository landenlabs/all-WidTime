package com.landenlabs.widtime.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.text.HtmlCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.landenlabs.widtime.R;

/**
 * Shows the privacy policy from the app's own strings (so it reads offline), with a
 * button to open the published copy in the browser. Keep R.string.privacy_text in step with
 * docs/privacy-policy.md.
 */
final class PrivacyDialog {

    private PrivacyDialog() {
    }

    static void show(Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.privacy_title)
                .setIcon(R.drawable.ic_privacy)
                .setMessage(HtmlCompat.fromHtml(context.getString(R.string.privacy_text).trim(),
                        HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton(R.string.close, null)
                .setNeutralButton(R.string.privacy_open_web, (d, w) -> openWebPage(context))
                .show();
    }

    private static void openWebPage(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.privacy_url))));
        } catch (ActivityNotFoundException ignored) {
            // No browser installed; the dialog text already holds the whole policy.
        }
    }
}
