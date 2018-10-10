package trifort.jp.blepasswordsystem.view.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;


public class AlertDialogFragment extends DialogFragment {

    private static final String TAG = "AlertDialogFragment";

    private static final String BUNDLE_KEY_MESSAGE = "bundle_key_message";
    private static final String BUNDLE_KEY_POSITIVE_TEXT = "bundle_key_positive_text";
    private static final String BUNDLE_KEY_NEGATIVE_TEXT = "bundle_key_negative_text";

    private Callback mCallback;

    public static AlertDialogFragment getInstance(@NonNull String message, @Nullable String positiveText) {
        return getInstance(message, positiveText, null);
    }

    @SuppressLint("ValidFragment")
    private AlertDialogFragment() {}

    public static AlertDialogFragment getInstance(@NonNull String message, @Nullable String positiveText, @Nullable String negativeText) {
        AlertDialogFragment fragment = new AlertDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_MESSAGE, message);
        bundle.putString(BUNDLE_KEY_POSITIVE_TEXT, positiveText);
        bundle.putString(BUNDLE_KEY_NEGATIVE_TEXT, negativeText);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();
        String message = args.getString(BUNDLE_KEY_MESSAGE, null);
        String positiveText = args.getString(BUNDLE_KEY_POSITIVE_TEXT, null);
        String negativeText = args.getString(BUNDLE_KEY_NEGATIVE_TEXT, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (message != null) {
            builder.setMessage(message);
        }
        if (positiveText != null) {
            builder.setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mCallback != null) {
                        mCallback.onPositive();
                    }
                }
            });
        }
        if (negativeText != null) {
            builder.setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mCallback != null) {
                        mCallback.onNegative();
                    }
                }
            });
        }


        setCancelable(false);

        return builder.create();
    }

    public void showDialog(FragmentManager fm, Callback callback) {
        if (fm.isStateSaved()) {
            return;
        }
        mCallback = callback;
//        show(fm, TAG);
        Fragment alert = fm.findFragmentByTag(TAG);
        if (alert != null) {
            return;
        }
        // TODO: 非同期処理でライフサイクルから外れることが多いのでStateLossを許容するようにしてみる
//        super.showNow(fm, tag);
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(this, TAG);
        ft.commitNowAllowingStateLoss();
    }

    // showDialogを使ってください
    @Deprecated
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
    }

    // showDialogを使ってください
    @Deprecated
    @Override
    public int show(FragmentTransaction transaction, String tag) {
        return super.show(transaction, tag);
    }

    public static class SimpleCallback implements Callback {
        @Override
        public void onPositive() {

        }

        @Override
        public void onNegative() {

        }
    }

    private interface Callback {
        void onPositive();
        void onNegative();
    }
}
