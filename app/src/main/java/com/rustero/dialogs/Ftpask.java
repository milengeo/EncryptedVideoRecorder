package com.rustero.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.rustero.R;
import com.rustero.app.MainActivity;
import com.rustero.gadgets.FtpServer;


public class Ftpask extends DialogFragment {


	public interface Eventer {
		void onOk(FtpServer aServer);
	}

	private static boolean sDialogShown = false;
	private static FtpServer sServer;


	public void ask(FragmentActivity aContext, FtpServer aServer) {
		if (sDialogShown) return;
		sServer = aServer;
		show(MainActivity.get().getSupportFragmentManager(), "Ftpask");
	}



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		sDialogShown = true;
		View dialogLayout = inflater.inflate(R.layout.ftpask, container, false);
		final Dialog dialog = getDialog();
		dialog.setCanceledOnTouchOutside(false);

		final EditText evHost = (EditText)dialogLayout.findViewById(R.id.ftpask_host);
		evHost.setText(sServer.host);

		final EditText evUser = (EditText)dialogLayout.findViewById(R.id.ftpask_user);
		evUser.setText(sServer.user);

		final EditText evPass = (EditText)dialogLayout.findViewById(R.id.ftpask_pass);
		evPass.setText(sServer.pass);

		final EditText evDir = (EditText)dialogLayout.findViewById(R.id.ftpask_dir);
		evDir.setText(sServer.dir);

		final Button cancelButton = (Button)dialogLayout.findViewById(R.id.ftpask_cancel);
		cancelButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						dialog.dismiss();
					}
				}
		);

		final Button okButton = (Button)dialogLayout.findViewById(R.id.ftpask_ok);
		okButton.setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v)
					{

						if (!isValidated(evHost.getText().toString())) {
							evHost.setError("incorrect value");
							return;
						}
						if (!isValidated(evHost.getText().toString())) {
							evUser.setError("incorrect value");
							return;
						}
						if (!isValidated(evHost.getText().toString())) {
							evPass.setError("incorrect value");
							return;
						}
						if (!isValidated(evHost.getText().toString())) {
							evDir.setError("incorrect value");
							return;
						}

						sServer.host = evHost.getText().toString();
						sServer.user = evUser.getText().toString();
						sServer.pass = evPass.getText().toString();
						sServer.dir = evDir.getText().toString();

						dialog.dismiss();
						Eventer eventer = MainActivity.get().new FtpaskEventer();
						eventer.onOk(sServer);
					}
				}
		);

		return dialogLayout;
	}



	private boolean isValidated(String aText) {
		if (aText.length() < 4) return false;
		if (aText.substring(0,1).equals(" ")) return false;
		return true;
	}


	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
		sDialogShown = false;
	}


}
