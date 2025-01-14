/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telebon.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import org.telebon.messenger.AndroidUtilities;
import org.telebon.messenger.ApplicationLoader;
import org.telebon.messenger.FileLog;
import org.telebon.messenger.MessageObject;
import org.telebon.messenger.R;
import org.telebon.messenger.UserConfig;
import org.telebon.messenger.Utilities;
import org.telebon.tgnet.SerializedData;
import org.telebon.tgnet.TLRPC;
import org.telebon.ui.Components.ShareAlert;

public class ShareActivity extends Activity {

    private Dialog visibleDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ApplicationLoader.postInitApplication();
        AndroidUtilities.checkDisplaySize(this, getResources().getConfiguration());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.Theme_TMessages_Transparent);
        super.onCreate(savedInstanceState);
        setContentView(new View(this), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Intent intent = getIntent();
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction()) || intent.getData() == null) {
            finish();
            return;
        }
        Uri data = intent.getData();
        String scheme = data.getScheme();
        String url = data.toString();
        String hash = data.getQueryParameter("hash");
        if (!"tgb".equals(scheme) || !url.toLowerCase().startsWith("tgb://share_game_score") || TextUtils.isEmpty(hash)) {
            finish();
            return;
        }

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("botshare", Activity.MODE_PRIVATE);
        String message = sharedPreferences.getString(hash + "_m", null);
        if (TextUtils.isEmpty(message)) {
            finish();
            return;
        }
        SerializedData serializedData = new SerializedData(Utilities.hexToBytes(message));
        TLRPC.Message mess = TLRPC.Message.TLdeserialize(serializedData, serializedData.readInt32(false), false);
        mess.readAttachPath(serializedData, 0);
        serializedData.cleanup();
        if (mess == null) {
            finish();
            return;
        }
        String link = sharedPreferences.getString(hash + "_link", null);
        MessageObject messageObject = new MessageObject(UserConfig.selectedAccount, mess, false);
        messageObject.messageOwner.with_my_score = true;

        try {
            visibleDialog = ShareAlert.createShareAlert(this, messageObject, null, false, link, false);
            visibleDialog.setCanceledOnTouchOutside(true);
            visibleDialog.setOnDismissListener(dialog -> {
                if (!isFinishing()) {
                    finish();
                }
                visibleDialog = null;
            });
            visibleDialog.show();
        } catch (Exception e) {
            FileLog.e(e);
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (visibleDialog != null && visibleDialog.isShowing()) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
