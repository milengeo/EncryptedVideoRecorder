
package com.rustero.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
import com.rustero.gadgets.PassHelper;
import com.rustero.gadgets.NewPass;
import com.rustero.widgets.MessageBox;


public class NewpwDialog extends DialogFragment {


	public interface Eventer {
		void onOk();
	}


	private static boolean sDialogShown = false;
	private Handler mPassHandler;
	PassHelper mPassHelper = new PassHelper();


	public NewpwDialog() {
		setStyle(DialogFragment.STYLE_NORMAL, R.style.MyDialogStyle);
		mPassHandler = new Handler();
		mPassHandler.postDelayed(passTimer, 222);
	}



	public void ask() {
		if (sDialogShown) return;
		show(MainActivity.get().getSupportFragmentManager(), "NewpwDialog");
	}




	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		sDialogShown = true;
		View dialogLayout = inflater.inflate(R.layout.new_pw, container, false);
		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		App.gNewPass = new NewPass();
		App.gNewPass.teStatus = (TextView) dialogLayout.findViewById(R.id.new_pw_status);
		App.gNewPass.tePass2 = (TextView) dialogLayout.findViewById(R.id.new_pw_text_pass2);
		App.gNewPass.edPass1 = (EditText) dialogLayout.findViewById(R.id.new_pw_pass1);
		App.gNewPass.edPass2 = (EditText) dialogLayout.findViewById(R.id.new_pw_edit_pass2);
		App.gNewPass.cbHide = (CheckBox) dialogLayout.findViewById(R.id.new_pw_hide);
		App.gNewPass.cbRepeat = (CheckBox) dialogLayout.findViewById(R.id.new_pw_repeat);

		hideNewPass(App.gHidePass);
		App.gNewPass.cbHide.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hideNewPass(isChecked);
			}
		});

		repeatNewPass(App.gRepeatPass);
		App.gNewPass.cbRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				repeatNewPass(isChecked);
			}
		});

		App.gNewPass.edPass2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					validateNewPass();
					if (!App.gNewPass.okay)
						return false;
					else {
						App.gNewPass.btnOk.performClick();
						return true;
					}
				}
				return false;
			}
		});


		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				dialog.dismiss();
				App.gNewPass = null;
			}
		});

		App.gNewPass.btnReuse = (Button) dialogLayout.findViewById(R.id.new_pw_reuse);
		if (App.gRecordPass.isEmpty()) {
			App.gNewPass.btnReuse.setVisibility(View.GONE);
		} else {
			App.gNewPass.btnReuse.setVisibility(View.VISIBLE);
			App.gNewPass.btnReuse.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					App.gNewPass.edPass1.setText(App.gRecordPass);
					App.gNewPass.edPass2.setText(App.gRecordPass);
				}
			});
		}

		App.gNewPass.btnCancel = (Button) dialogLayout.findViewById(R.id.new_pw_cancel);
		App.gNewPass.btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				App.gNewPass = null;
			}
		});

		App.gNewPass.btnOk = (Button) dialogLayout.findViewById(R.id.new_pw_ok);
		App.gNewPass.btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				if (App.gNewPass.edPass1.getText().toString().length() < 6) {
					App.gNewPass.edPass1.setError("Typed text is too short!");
					return;
				}
				validateNewPass();

				if (!App.gNewPass.okay) {
					MessageBox.show("Password Error!", App.gNewPass.teStatus.getText().toString());
					return;
				}

				App.gRecordPass = App.gNewPass.edPass1.getText().toString();
				App.gPassMils = System.currentTimeMillis();
				App.gNewPass = null;
				dialog.dismiss();

				Eventer eventer = MainActivity.getNewpwDialogEventer();
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



	private void hideNewPass(boolean aHide) {
		if (aHide) {
			// to hide
			App.gHidePass = true;
			App.gNewPass.cbHide.setChecked(true);
			App.gNewPass.edPass1.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			App.gNewPass.edPass1.setSelection(App.gNewPass.edPass1.length());
			App.gNewPass.edPass2.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			App.gNewPass.edPass2.setSelection(App.gNewPass.edPass2.length());
		} else {
			// to show
			App.gHidePass = false;
			App.gNewPass.cbHide.setChecked(false);
			App.gNewPass.edPass1.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			App.gNewPass.edPass1.setSelection(App.gNewPass.edPass1.length());
			App.gNewPass.edPass2.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			App.gNewPass.edPass2.setSelection(App.gNewPass.edPass2.length());
		}
	}



	private void repeatNewPass(boolean aRepeat) {
		if (aRepeat) {
			// enable second pass
			App.gRepeatPass = true;
			App.gNewPass.cbRepeat.setChecked(true);
			App.gNewPass.tePass2.setTextColor(Color.parseColor("#000000"));
			App.gNewPass.edPass2.setEnabled(true);
		} else {
			// disable second pass
			App.gRepeatPass = false;
			App.gNewPass.cbRepeat.setChecked(false);
			App.gNewPass.tePass2.setTextColor(Color.parseColor("#888888"));
			App.gNewPass.edPass2.setEnabled(false);
		}
	}




	public void validateNewPass() {
		App.gNewPass.okay = false;
		String pass1 = App.gNewPass.edPass1.getText().toString();
		String pass2 = App.gNewPass.edPass2.getText().toString();

		if (pass1.length() < 6) {
			App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#ffcccc"));
			App.gNewPass.teStatus.setText(App.resstr(R.string.six_characters_required));
			return;
		}

		if (App.gRepeatPass) {
			if (pass2.length() == 0) {
				App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#ffcccc"));
				App.gNewPass.teStatus.setText(App.resstr(R.string.please_retype_password));
				return;
			}
			if (!pass1.equals(pass2)) {
				App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#ffcccc"));
				App.gNewPass.teStatus.setText(App.resstr(R.string.passwords_do_not_match));
				return;
			}
		}

        App.gNewPass.okay = true;
		mPassHelper.validate(pass1);
		App.gNewPass.teStatus.setText(mPassHelper.getStatus());
		if (mPassHelper.isWeak())
			App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#ffcccc"));
		else if (mPassHelper.isReasonable())
			App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#cccccc"));
		else
			App.gNewPass.teStatus.setBackgroundColor(Color.parseColor("#ccffcc"));
	}



	private Runnable passTimer = new Runnable() {
		public void run() {
			if ((null != App.gNewPass) && (null != App.gNewPass.btnOk)) {
				//App.log("passTimer: " + App.gNewPass.edPass1.getText().toString());
				validateNewPass();
			}
			mPassHandler.postDelayed(this, 222);
		}
	};


}
