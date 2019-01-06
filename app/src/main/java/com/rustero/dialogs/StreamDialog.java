package com.rustero.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;

import com.rustero.R;
import com.rustero.app.MainActivity;


public class StreamDialog extends DialogFragment {


	public interface Eventer {
		void onDone(String aUrl);
	}


	private static boolean sDialogShown = false;
	private static String mUrl;



	public StreamDialog() {
		setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogStyle);
	}



	public void ask(String aUrl) {
		if (sDialogShown) return;
		mUrl = aUrl;
		show(MainActivity.get().getSupportFragmentManager(), "StreamDialog");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		sDialogShown = true;
		View dialogLayout = inflater.inflate(R.layout.stream_dialog, container, false);
		final Dialog dialog = getDialog();
		dialog.setTitle("Please create a password");
		dialog.setCanceledOnTouchOutside(false);

		final EditText nameView = (EditText)dialogLayout.findViewById(R.id.stream_dlg_edit_url);
		nameView.setText(mUrl);
		nameView.setSelection(nameView.length());

		final Button cancelButton = (Button)dialogLayout.findViewById(R.id.stream_dlg_button_cancel);
		final Button doneButton = (Button)dialogLayout.findViewById(R.id.stream_dlg_button_done);

		cancelButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				}
		);

		doneButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v)
					{
						String newUrl = nameView.getText().toString();
						if (!URLUtil.isValidUrl(newUrl)) {
							nameView.setError("not a valid URL");
							return;
						}

						dialog.dismiss();
						Eventer eventer = MainActivity.get().new StreamDialogEventer();
						eventer.onDone(newUrl);
					}
				}
		);

		return dialogLayout;
	}


	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		sDialogShown = false;
	}



}
