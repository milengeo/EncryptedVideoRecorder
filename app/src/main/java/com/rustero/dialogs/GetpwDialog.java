
package com.rustero.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.rustero.App;
import com.rustero.R;
import com.rustero.app.MainActivity;
import com.rustero.gadgets.Tools;


public class GetpwDialog extends DialogFragment {


	public interface Eventer {
		void onOk();
	}

	private static boolean sDialogShown = false;


	public GetpwDialog() {
		setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogStyle);
	}



	public void ask() {
		if (sDialogShown) return;
		show(MainActivity.get().getSupportFragmentManager(), "GetpwDialog");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		sDialogShown = true;
		View dialogLayout = inflater.inflate(R.layout.get_pw, container, false);
		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		final TextView tvFile = (TextView) dialogLayout.findViewById(R.id.get_pw_filename);
		final EditText edPass = (EditText) dialogLayout.findViewById(R.id.get_pw_edit_pass);
		final CheckBox cbHide = (CheckBox) dialogLayout.findViewById(R.id.get_pw_hide);

		final Button btnReuse = (Button) dialogLayout.findViewById(R.id.get_pw_reuse);
		final Button btnCancel = (Button) dialogLayout.findViewById(R.id.get_pw_cancel);
		final Button btnOk = (Button) dialogLayout.findViewById(R.id.get_pw_ok);

		tvFile.setText(Tools.getFileNameExt(App.gPlayPath));
		if (App.gHidePass) {
			cbHide.setChecked(true);
			edPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		} else {
			cbHide.setChecked(false);
			edPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
		}

		cbHide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					// to hide
					edPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
					edPass.setSelection(edPass.length());
					App.gHidePass = true;
				} else {
					// to show
					edPass.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
					edPass.setSelection(edPass.length());
					App.gHidePass = false;
				}
			}
		});

		edPass.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					if (edPass.getText().toString().length() < 4)
						return false;
					else {
						btnOk.performClick();
						return true;
					}
				}
				return false;
			}
		});

		if (App.gPlayPass.isEmpty()) {
			btnReuse.setVisibility(View.GONE);
		} else {
			btnReuse.setVisibility(View.VISIBLE);
			btnReuse.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					edPass.setText(App.gPlayPass);
					edPass.setSelection(edPass.length());
				}
			});
		}

		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});

		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (edPass.getText().toString().length() < 6) {
					edPass.setError("Typed text is too short");
					return;
				}

				App.gPlayPass = edPass.getText().toString();
				App.gPassMils = System.currentTimeMillis();

				dialog.dismiss();
				Eventer eventer = MainActivity.getGetpwDialogEventer();
				if (null == eventer) return;
				eventer.onOk();
			}
		});

		dialog.show();
		return dialogLayout;
	}



	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		sDialogShown = false;
	}




}
