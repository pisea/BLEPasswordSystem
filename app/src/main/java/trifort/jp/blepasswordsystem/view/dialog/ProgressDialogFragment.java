package trifort.jp.blepasswordsystem.view.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import trifort.jp.blepasswordsystem.R;

public class ProgressDialogFragment extends DialogFragment {

    public static final String TAG_DELETING = "deleting";

    private static final String TAG = "ProgressDialogFragment";
    private static final String BUNDLE_KEY_MESSAGE = "bundle_key_message";

    public static ProgressDialogFragment getInstance(String message) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        Bundle b = new Bundle();
        b.putString(BUNDLE_KEY_MESSAGE, message);
        fragment.setArguments(b);
        return fragment;
    }

    @SuppressLint("ValidFragment")
    private ProgressDialogFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();
        String message = args.getString(BUNDLE_KEY_MESSAGE, "");

        View progressView = LayoutInflater.from(getActivity()).inflate(R.layout.view_progress_dialog, null);
        ((TextView) progressView.findViewById(R.id.tv_loading)).setText(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(progressView);

        setCancelable(false);

        return builder.create();
    }

    public void showDialog(FragmentManager fm) {
        showDialog(fm, TAG);
    }

    public void showDialog(FragmentManager fm, String tag) {
        Fragment progress = fm.findFragmentByTag(tag);
        if (progress != null) {
            return;
        }
        // TODO: 非同期処理でライフサイクルから外れることが多いのでStateLossを許容するようにしてみる
//        super.showNow(fm, tag);
        FragmentTransaction ft = fm.beginTransaction();
        ft.add(this, tag);
        ft.commitNowAllowingStateLoss();
    }

    public static boolean isProgressShowing(FragmentManager fm, String tag) {
        Fragment progress = fm.findFragmentByTag(tag);
        if (progress != null) {
            if (progress instanceof ProgressDialogFragment) {
                return progress.isVisible();
            }
        }
        return false;
    }

    public static boolean isProgressShowing(FragmentManager fm) {
        return isProgressShowing(fm, TAG);
    }

    public static void dismissDialog(FragmentManager fm) {
        dismissDialog(fm, TAG);
    }

    public static void dismissDialog(FragmentManager fm, String tag) {
        Fragment progress = fm.findFragmentByTag(tag);
        if (progress != null) {
            if (progress instanceof ProgressDialogFragment) {
                ((ProgressDialogFragment) progress).dismissAllowingStateLoss();
            }
        }
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
}
