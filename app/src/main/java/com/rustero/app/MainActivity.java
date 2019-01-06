package com.rustero.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.rustero.Errors;
import com.rustero.dialogs.DeleteDialog;
import com.rustero.dialogs.Ftpask;
import com.rustero.dialogs.Ftprun;
import com.rustero.dialogs.RenameDialog;
import com.rustero.App;
import com.rustero.gadgets.Connectivity;
import com.rustero.gadgets.Tools;
import com.rustero.gadgets.Mycams;
import com.rustero.gadgets.FilmItem;
import com.rustero.gadgets.FilmList;
import com.rustero.gadgets.FilmMeta;
import com.rustero.dialogs.GetpwDialog;
import com.rustero.gadgets.FtpServer;
import com.rustero.widgets.MessageBox;
import com.rustero.widgets.MyActivity;
import com.rustero.widgets.MyDragButton;
import com.rustero.dialogs.NewpwDialog;
import com.rustero.dialogs.StreamDialog;
import com.rustero.widgets.PanelSlider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import com.rustero.R;


@SuppressWarnings("ResourceType")



public class MainActivity extends MyActivity implements AdapterView.OnItemClickListener {

	public static MainActivity self;

	private final int FORGET_SECS = 60;
	private final int KNOB_SIZE = 180;
	private final int ACT_RESULT_SETTINGS = 	11;
	private final int ACT_RESULT_RECORD = 		12;
	private final int ACT_RESULT_REPLAY = 		13;
	private final int ACT_RESULT_EXPORT =		14;

	private final int PANEL_ITEM_SETTINGS 		= 1;
	private final int PANEL_ITEM_HELP 			= 2;
	private final int PANEL_ITEM_ABOUT 			= 3;
	private final int PANEL_ITEM_ENCOURAGE 		= 4;
	private final int PANEL_ITEM_INVITE 		= 5;
	private final int PANEL_ITEM_MENTION		= 6;



	private PanelSlider mPanelSlider;
	private View mLeftPanel, mPanelBack;
	private ImageButton mOpenPanel;
	private ListView mPanelList;
	private ArrayList<MameItem> mMameItems;

	private ActionBar mActBar;
	private Toolbar mToolbar;
	private ListView mFilmView;
	private FilmAdapter mAdapter;
	private FilmList mFilmList;
	private MetaTask mMetaTask = new MetaTask();
	private MyDragButton btnRecord;
	private MenuItem mRenameMeit, mDeleteMeit, mExportMeit, mStreamMeit, mUploadMeit, mShareMeit;
	private ArrayList<String> mFolders = new ArrayList<String>();
	private HashSet<String> mScannedFolders = new HashSet<>();

	private Handler mMyHandler;
	private boolean mTacked, mExport;

	private long mTotalFiles;
	private long mTotalBytes;
	private static int sScrollIndex;
	private int mListCount;







	public static MainActivity get() {
		return self;
	}



	public void onResume() {
		super.onResume();
		self = this;
		hideNotification();

		long awaySecs = App.aliveMeter.sinceMils() / 1000;
		if (awaySecs > FORGET_SECS) {
			App.gRecordPass = "";
			App.gPlayPass = "";
		}

		scanFolders();
	}



	@Override
	protected void onPause() {
		super.onPause();
		self = null;

		App.aliveMeter.click();
		sScrollIndex = mFilmView.getFirstVisiblePosition();

		if (Ftprun.isActive()) {
			showNotification();
		}

	}





	@Override
	protected void onCreate(Bundle savedInstanceState) {
		long took = App.mils();

		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);

		App.aliveMeter.click();
		App.log("onCreate");
		App.log(Tools.getDisplayInfo(this));

		Mycams.attach(this);

		mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
		setSupportActionBar(mToolbar);

		mActBar = getSupportActionBar();
		mActBar.setDisplayShowTitleEnabled(false); // Hide default toolbar title
		View container2 = findViewById(R.id.main_container_2);

		mPanelBack = findViewById(R.id.main_panel_back);
		mLeftPanel = findViewById(R.id.main_panel_left);
		mOpenPanel = (ImageButton) findViewById(R.id.main_open_panel);
		mOpenPanel.setOnClickListener(OpenPanelClicker);
		mPanelSlider = new PanelSlider(222, mPanelBack, null);

		mPanelList = (ListView) findViewById(R.id.main_panel_list);
		mPanelList.setOnItemClickListener(mDrawerItemClicker);

		btnRecord = (MyDragButton) findViewById(R.id.main_new_record);
		btnRecord.setAnchor(container2);
		btnRecord.setOnPressListener(new RecordClicker());
		App.screenScaled(btnRecord, KNOB_SIZE);

		mFilmView = (ListView) findViewById(R.id.main_film_view);
		mFilmView.setOnItemClickListener(this);

		if (!havePermissions()) {
			return;
		}

		mMyHandler = new Handler();
		mMyHandler.postDelayed(clickTack, 999);

		mPanelBack.post(new Runnable() {
			@Override
			public void run() {
				onShow();
			}
		});

		took = App.mils() - took;
		App.log("  * onCreate: " + took);
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		self = null;
	}


	private boolean isFileIntent() {
		Intent intent = getIntent();
		String action = intent.getAction();
		App.log("isFileIntent_11 action: " + action);

		try {
			if (action.compareTo(Intent.ACTION_VIEW) == 0) {
				App.log(" ***** action.compareTo(Intent.ACTION_VIEW) ");
				String scheme = intent.getScheme();

				ContentResolver resolver = getContentResolver();
				if (scheme.compareTo(ContentResolver.SCHEME_FILE) == 0) {
					Uri uri = intent.getData();
					String path = uri.getPath();
					App.log("File intent detected: " + path);

					App.gPlayPath = path;
					mExport = false;
					launchPlayback();
					return true;
				}
			}
		}
		catch (Exception e) {
			App.log(" ***_ex isFileIntent: " + e.getMessage());
		}
		return false;
	}



	public void onShow() {
		if (PanelSlider.type == PanelSlider.TYPE.LEFT)
			OpenPanelClicker.onClick(null);
		else
			isFileIntent();
	}




	@Override
	public void onBackPressed() {
		if (mPanelSlider.clear()) {
			return;
		}
		super.onBackPressed();
	}




	private void showNotification() {
		Intent intent = new Intent(this, MainActivity.class);
		intent.setAction(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		Notification.Builder builder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.app_icon_96)
				.setContentTitle(getString(R.string.app_name) + " is running")
				.setContentText("Tap to open")
				.setContentIntent(contentIntent);

		Notification notification = builder.build();
		notification.flags = Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL;

		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(App.NOTIFICATION_BAR_MAIN, notification);
	}



	public void hideNotification() {
		NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		manager.cancel(App.NOTIFICATION_BAR_MAIN);
	}




	private boolean havePermissions() {
		if (!App.selfPermissionGranted(this, Manifest.permission.INTERNET)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow access to internet!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.CAMERA)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow access to camera!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.RECORD_AUDIO)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow audio recording!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow storage reading!");
			return false;
		}

		if (!App.selfPermissionGranted(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
			App.finishActivityAlert(this, "Permission needed!", "You need to allow storage writing!");
			return false;
		}

		return true;
	}



	private Runnable clickTack = new Runnable() {
		@Override
		public void run() {
			mMyHandler.postDelayed(this, 999);
			firstTack();
		}
	};



	void firstTack() {
		if (mTacked) return;
		mTacked = true;
	}







	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		App.log("onCreateOptionsMenu");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		mShareMeit = menu.findItem(R.id.main_meit_share);
		mStreamMeit = menu.findItem(R.id.main_meit_download);
		mUploadMeit = menu.findItem(R.id.main_meit_upload);
		mExportMeit = menu.findItem(R.id.main_meit_export);
		mRenameMeit = menu.findItem(R.id.main_meit_rename);
		mDeleteMeit = menu.findItem(R.id.main_meit_delete);

		return true;
	}




	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		updateMenu();
		return super.onPrepareOptionsMenu(menu);
	}



	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		App.log("onOptionsItemSelected");
		switch (item.getItemId()) {
			case R.id.main_meit_share:
				shareFilm();
				return true;
			case R.id.main_meit_download:
				askStream();
				return true;
			case R.id.main_meit_upload:
				askFtp();
				return true;
			case R.id.main_meit_export:
				exportFilm();
				return true;
			case R.id.main_meit_rename:
				renameFilm();
				return true;
			case R.id.main_meit_delete:
				deleteFilm();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}






	private void shareFilm() {
		if (mFilmList.getSelected() == null) return;
		String path = mFilmList.getSelected().path;
		String name = mFilmList.getSelected().name;
		File inFile = new File(path);
		if (!inFile.exists()) return;

		try {
			final Uri uri = FileProvider.getUriForFile(this, "rustero.encryptedvideorecorder.fileprovider", inFile);
			final Intent intent = ShareCompat.IntentBuilder.from(this)
					.setType("application/savi")
					.setSubject(name)
					.setStream(uri)
					.setChooserTitle("Share this file with")
					.createChooserIntent()
					.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

			this.startActivity(intent);
		} catch (Exception e) {	}
	}





	private void askFtp() {
		if (mFilmList.getSelected() == null) return;
		if (!App.isDevel()) {
			if (!Connectivity.isConnectedWifi(MainActivity.this)) {
				MessageBox.show("Cannot uppload to FTP!", "Upload to FTP web requires WiFi network!");
				return;
			}
		}

        //App.fbLog("askFtp");
		FtpServer server = new FtpServer();
		server.path = mFilmList.getSelected().path;
		server.host = App.getPrefStr("ftp_host");
		server.user = App.getPrefStr("ftp_user");
		server.pass = App.getPrefStr("ftp_pass");
		server.dir = App.getPrefStr("ftp_dir");

//		if (App.IS_DEVEL) {
//			server.host = "ftp.rustero.com";
//			server.user = "test12@rustero.com";
//			server.pass = "zxcvbn12";
//			server.dir = "";
//		}

		new Ftpask().ask(this, server);
	}



	public class FtpaskEventer implements Ftpask.Eventer {

		public void onOk(FtpServer aServer) {
			FilmItem fiit = mFilmList.getFilm(aServer.path);
			if (null == fiit) return;
			Ftprun.begin(fiit, aServer);
			App.setPrefStr("ftp_host", aServer.host);
			App.setPrefStr("ftp_user", aServer.user);
			App.setPrefStr("ftp_pass", aServer.pass);
			App.setPrefStr("ftp_dir", aServer.dir);
		}
	}




	private void renameFilm() {
        //App.fbLog("renameFilm");
		if (mFilmList.getSelected() == null) return;
		String name = mFilmList.getSelected().name;
		name = name.substring(0, name.indexOf("."));

		new RenameDialog().ask(this, mFilmList.getSelected().path, name);
	}



	public class RenameDialogEventer implements RenameDialog.Eventer {

		public void onDone(String aPath, String aName) {

			FilmItem film = mFilmList.getFilm(aPath);
			if (film == null) return;
			File oldFile = new File(aPath);
			String newPath = oldFile.getParent() + "/" + aName + "." + film.ext;
			File newFile = new File(newPath);
			if (newFile.exists()) {
				MessageBox.show("Error renaming", "File already exists!");
				return;
			}

			oldFile.renameTo(newFile);
			film.path = newPath;
			film.name = newFile.getName();

			mAdapter.notifyDataSetChanged();
		}
	}



	private void deleteFilm() {
		if (mFilmList.getSelected() == null) return;
		final String path = mFilmList.getSelected().path;
		new DeleteDialog().ask(path);
	}



	public class DeleteDialogEventer implements DeleteDialog.Eventer {
		public void onOk(String aPath) {
			File file = new File(aPath);
			boolean deleted = file.delete();
			if (deleted) {
				FilmItem film = mFilmList.getFilm(aPath);
				if (null == film) return;
				mAdapter.remove(film);
				mFilmList.remove(film);
				mFilmList.setSelected(null);
				countTotals();
				updateUI();
			}
		}
	}








	private void showSettings() {
		Mycams.verifySelectedResolutions();
		Intent intent = new Intent(this, SettingsActivity.class);
		startActivityForResult(intent, ACT_RESULT_SETTINGS);
	}



	private void showHelp() {
        //App.fbLog("showHelp");
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.rustero.com/encryptedvideorecorder/index.html#main"));
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		startActivityForResult(intent, 0);
	}


	private void showAbout() {
        //App.fbLog("showAbout");
		Intent intent = new Intent(this, AboutActivity.class);
		startActivityForResult(intent, 0);
	}


	private void showEncourage() {
        //App.fbLog("showEncourge");
		Uri uri = Uri.parse("market://details?id=" + getPackageName());
		Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
		try {
			startActivity(myAppLinkToMarket);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, " unable to find market app", Toast.LENGTH_LONG).show();
		}
	}






	private void showMention() {
		try {
			String urlToShare = "https://play.google.com/store/apps/details?id=rustero.encryptedvideorecorder";

			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, urlToShare);

			// As fallback, launch sharer.php in a browser
			String fbPackage = getFacebookPackage(intent);
			if ("" == fbPackage) {
				String sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=" + urlToShare;
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl));
			} else
				intent.setPackage(fbPackage);

			startActivity(intent);

            //App.fbLog("showMention");
		} catch (Exception e) {	}
	}



	private String getFacebookPackage(Intent intent) {
		String result = "";
		List<ResolveInfo> matches = getPackageManager().queryIntentActivities(intent, 0);
		for (ResolveInfo info : matches) {
			if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
				result = info.activityInfo.packageName;
				break;
			}
		}
		return result;
	}



	private void showInvite() {
		try {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_SUBJECT, "Record password protected videos");
			intent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=rustero.encryptedvideorecorder");
			startActivity(Intent.createChooser(intent, "Share with ..."));

            //App.fbLog("showInvite");
		} catch (Exception e) {
			//e.toString();
		}
	}






	private class RecordClicker implements MyDragButton.OnPressListener {

		public void onPress(MyDragButton aSender) {
			launchRecording();
		}
	};







	private void launchRecording() {
		if (App.isDevel()) {
			App.gRecordPass = "qwerty";
			new NewpwDialogEventer().onOk();
		} else {
			new NewpwDialog().ask();
		}
	}



	public static NewpwDialogEventer getNewpwDialogEventer() {
		if (null == self) return null;
		return self.new NewpwDialogEventer();
	}



	public class NewpwDialogEventer implements NewpwDialog.Eventer {
		public void onOk() {
				mListCount = mFilmView.getCount();
				sScrollIndex = mFilmView.getFirstVisiblePosition();
				Intent intent = new Intent(MainActivity.this, RecordActivity.class);
				startActivityForResult(intent, ACT_RESULT_RECORD);
		}
	}




	private void exportFilm() {
		if (mFilmList.getSelected() == null) return;
		App.gPlayPath = mFilmList.getSelected().path;
		mExport = true;
		launchPlayback();
	}



	private class PlayClicker implements View.OnClickListener {

		public void onClick(View view) {
			sScrollIndex = mFilmView.getFirstVisiblePosition();
			mFilmList.setSelected(null);

			FilmItem item = (FilmItem) view.getTag();
			App.gPlayPath = item.path;
			mExport = false;
			launchPlayback();
		}

	}



	private void launchPlayback() {
		if (App.isDevel()) {
			App.gPlayPass = "qwerty";
			new GetpwDialogEventer().onOk();
		} else {
			new GetpwDialog().ask();
		}
	}


	public static GetpwDialogEventer getGetpwDialogEventer() {
		if (null == self) return null;
		return self.new GetpwDialogEventer();
	}


	public class GetpwDialogEventer implements GetpwDialog.Eventer {

		public void onOk() {
			if (mExport) {
				Intent intent = new Intent(MainActivity.this, ExportActivity.class);
				startActivityForResult(intent, ACT_RESULT_EXPORT);
			} else {
				Intent intent = new Intent(MainActivity.this, ReplayActivity.class);
				startActivityForResult(intent, ACT_RESULT_REPLAY);
			}
		}
	}





	private void askStream() {

		if (!App.isDevel()) {
			if (!Connectivity.isConnectedWifi(MainActivity.this)) {
				MessageBox.show("Cannot stream!", "Streaming from web requires WiFi network!");
				return;
			}
		}

		String url = App.getPrefStr("stream_from_web_url");

//		if (App.isDevel()) {
//			url = "http://rustero.com/test12/ftp_1.savi";
//			//url = "https://drive.google.com/uc?export=download&id=0B0f6-rb43dAxOHBFN1FuOFFBUGs";
//		}

		new StreamDialog().ask(url);
	}



	public class StreamDialogEventer implements StreamDialog.Eventer {
		public void onDone(String aUrl) {
			App.log("StreamDialog: " + aUrl);

			if (!URLUtil.isValidUrl(aUrl)) {
				MessageBox.show("Error!", "URL is not valid!");
				return;
			}

			App.setPrefStr("stream_from_web_url", aUrl);
			App.gPlayPath = aUrl;
			launchPlayback();
		}
	}





	@Override
	protected void onActivityResult(int aSender, int resultCode, Intent data) {
		super.onActivityResult(aSender, resultCode, data);
		App.aliveMeter.click();
		switch (aSender) {
			case ACT_RESULT_SETTINGS:
				onSettingsIntentResult(resultCode);
				break;
			case ACT_RESULT_RECORD:
				onRecordIntentResult(resultCode);
				break;
			case ACT_RESULT_REPLAY:
				onReplayIntentResult(resultCode);
				break;
			case ACT_RESULT_EXPORT:
				onExportIntentResult(resultCode);
				break;
		}
	}



	public void onSettingsIntentResult(int aResultCode) {
		int bitrate = App.getPrefAsInt("record_bitrate");
		App.log("bitrate: " + bitrate);
	}


	public void onRecordIntentResult(int aResultCode) {
		App.log("onRecordIntentResult: " + App.gRecordedPath);
		if (!App.gRecordedPath.isEmpty()) {
			String name = Tools.getFileNameExt(App.gRecordedPath);
			App.gMetaHeap.remove(name);
			//mNeedScroll = true;
		}
	}


	private void onReplayIntentResult(final int aResultCode) {
		App.log("onReplayIntentResult: " + aResultCode);
		new Handler().postDelayed(new Runnable() {
			public void run() {

				if (aResultCode < 0) {
					if (aResultCode == Errors.WRONG_PASSWORD)
						MessageBox.show(App.resstr(R.string.error), App.resstr(R.string.wrong_password));
					else
						MessageBox.show(App.resstr(R.string.error), App.sErrorText);
					return;
				}
			}
		}, 555);
	}




	private void onExportIntentResult(final int aResultCode) {
		App.log("onExportIntentResult: " + aResultCode);
		new Handler().postDelayed(new Runnable() {
			public void run() {
				if (ExportActivity.sLastResult == 0) {
					MessageBox.show("Success", "The export was successful!");
				} else if (ExportActivity.sLastResult > 0) {
					MessageBox.show("Warning", "The export was not completed!");
				} else if (ExportActivity.sLastResult < 0) {
					if (ExportActivity.sLastResult == Errors.WRONG_PASSWORD)
						MessageBox.show(App.resstr(R.string.error), App.resstr(R.string.wrong_password));
					else
						MessageBox.show(App.resstr(R.string.error), App.sErrorText);
				}
			}
		}, 555);
	}



    @Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		FilmItem item = mAdapter.getItem(position);
        if (mFilmList.getSelected() != item)
			mFilmList.setSelected(item);
        else
			mFilmList.setSelected(null);
        mAdapter.notifyDataSetChanged();
        updateUI();
    }




    // take one folder content
    public void pickupFolder()
    {
		String path = mFolders.get(0);
		mFolders.remove(0);
		if (mScannedFolders.contains(path)) {
			return;
		}

		mScannedFolders.add(path);
        File folder = new File(path);
        File[] files = folder.listFiles();
        try {
            for(File file: files)
            {
                if (file.isDirectory()) {
					mFolders.add(file.getAbsolutePath());
					continue;
				}
                String ext = Tools.getFileExt(file.getName());
                if (!ext.equals("savi")) continue;

                Date lastModDate = new Date(file.lastModified());
				SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				String date_modify = formater.format(lastModDate);

                FilmItem fiit = new FilmItem();
                mFilmList.add(fiit);
				fiit.path = file.getAbsolutePath();
                fiit.ext = ext;
                fiit.name = file.getName();
				fiit.folder = Tools.getFileFolder(fiit.path);
                fiit.bytes = Tools.formatSize(file.length());
                fiit.date = date_modify;
                fiit.size = file.length();
            }
        } catch(Exception e) {}
    }



    // load the folder content from the disk with icons and dates
    public void scanFolders() {
        mMetaTask.quit = true;

		mFilmList = new FilmList();
		mFolders.add(App.getOutputFolder());
		mFolders.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString());
		mFolders.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString());
		mFolders.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString());

		mScannedFolders.clear();
		while (mFolders.size() > 0) {
			pickupFolder();
		}
		countTotals();

		Collections.sort(mFilmList);
        mAdapter = new FilmAdapter(this, R.layout.film_row, mFilmList);
        mFilmView.setAdapter(mAdapter);
        updateUI();

        mMetaTask = new MetaTask();
        mMetaTask.execute();
    }



	private void countTotals() {
		mTotalFiles = 0;
		mTotalBytes = 0;
		for (FilmItem fiit : mFilmList) {
			mTotalFiles++;
			mTotalBytes += fiit.size;
		}
	}



    private void updateStatus() {
        TextView view = (TextView) findViewById(R.id.main_tevi_status);
        if (mTotalFiles == 0) {
            view.setText("No videos found");
        } else {
            String text;
            text = mTotalFiles + " " + App.resstr(R.string.videos) + ", " + App.resstr(R.string.total_size) + " " + Tools.formatSize(mTotalBytes);
            view.setText(text);
        }
    }



    private void updateUI() {
        updateStatus();
        updateMenu();
    }



    public void updateMenu() {
		if (null == mDeleteMeit) return;

		boolean haveWifi = Connectivity.isConnectedWifi(this);
		if (!App.isDevel()) {
			mStreamMeit.setVisible(haveWifi);
		}

		FilmItem seldFilm = mFilmList.getSelected();
		mShareMeit.setVisible((seldFilm != null));
		mUploadMeit.setVisible(haveWifi && (seldFilm != null));
		mExportMeit.setVisible((seldFilm != null));
		mRenameMeit.setVisible((seldFilm != null));
        mDeleteMeit.setVisible((seldFilm != null));
    }






    public class FilmAdapter extends ArrayAdapter<FilmItem> {

        private Context mContext;
        private int mLayout;
        List<FilmItem> mItems;

        public FilmAdapter(Context context, int resource, List<FilmItem> objects) {
            super(context, resource, objects);
            this.mContext = context;
            this.mLayout = resource;
            this.mItems = objects;
        }


        public FilmItem getItem(int i)
        {
            return mItems.get(i);
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                rowView = inflater.inflate(mLayout, null);
            }
            // create a new view of my layout and inflate it in the row
            final FilmItem item = mItems.get(position);
			FilmItem seldFilm = mFilmList.getSelected();
            if ( (seldFilm != null) && seldFilm.path.equals(item.path))
                rowView.setBackgroundColor(0xffcccccc);
            else
                rowView.setBackgroundColor(0xffffffff);

            TextView tevi;
			tevi = (TextView) rowView.findViewById(R.id.film_row_size);
			tevi.setText(item.bytes);

			if (null != item.meta) {
				tevi = (TextView) rowView.findViewById(R.id.film_row_duration);
				if (item.meta.duration2 > 0)
					tevi.setText(Tools.formatDuration(item.meta.duration2/1000));
			}

            tevi = (TextView) rowView.findViewById(R.id.film_row_name);
            tevi.setText(item.name);

			tevi = (TextView) rowView.findViewById(R.id.film_row_folder);
			tevi.setText(item.folder);

            tevi = (TextView) rowView.findViewById(R.id.film_row_date);
            tevi.setText(item.date);

			ImageView iv = (ImageView) rowView.findViewById(R.id.film_row_icon_play);
			iv.setTag(item);
			iv.setOnClickListener(new PlayClicker());

            return rowView;
        }
    }





    public class MetaTask extends AsyncTask<Void, Integer, String> {

        private volatile boolean quit;
        private FilmList list = new FilmList();
        private App.MetaHeapC heap = new App.MetaHeapC();

        @Override
        protected String doInBackground(Void... params) {
            try {
                for (FilmItem fi : mFilmList) {
                    if (quit) return "";
					FilmMeta meta = App.gMetaHeap.get(fi.name);
                    if (null == meta)
                        meta = getMeta(fi);
                    fi.meta = meta;
                    list.add(fi);
                    heap.put(fi.name, meta);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }



        @Override
        protected void onPostExecute(String result) {
            if (quit) return;
            App.gMetaHeap = heap;
            mFilmList = list;
            mAdapter = new FilmAdapter(MainActivity.this, R.layout.film_row, mFilmList);
            mFilmView.setAdapter(mAdapter);

			if ( (mListCount > 0) && (mListCount != mAdapter.getCount()) ) {
				mFilmView.setSelection(mAdapter.getCount());
				mListCount = 0;
			} else if (sScrollIndex > 0) {
				mFilmView.setSelection(sScrollIndex);
				sScrollIndex = 0;
			}
        }



        private FilmMeta getMeta(FilmItem aFilm) {
			FilmMeta result = new FilmMeta();

            if (aFilm.ext.equals("savi")) {
                try {
                    result.duration2 = App.gSavi2Lib.getSaviLength(aFilm.path);
                } catch (Exception ex) {

				}
            }

            return result;
        }

    }




	View.OnClickListener OpenPanelClicker = new View.OnClickListener() {
		@Override
		public void onClick(final View v) {
			mMameItems = new ArrayList<MameItem>();
			mMameItems.add(new MameItem(PANEL_ITEM_SETTINGS, R.drawable.settings_54, App.resstr(R.string.settings)));
			mMameItems.add(new MameItem(PANEL_ITEM_ENCOURAGE, R.drawable.star_54, App.resstr(R.string.encaurageus)));
			mMameItems.add(new MameItem(PANEL_ITEM_INVITE, R.drawable.thumb_54, App.resstr(R.string.invite_friend)));
			mMameItems.add(new MameItem(PANEL_ITEM_MENTION, R.drawable.facebook_54, App.resstr(R.string.mention_facebook)));
			mMameItems.add(new MameItem(PANEL_ITEM_ABOUT, R.drawable.about_54, App.resstr(R.string.about)));

			DrawerAdapter adapter = new DrawerAdapter(MainActivity.this, R.layout.panel_row, mMameItems);
			mPanelList.setAdapter(adapter);

			if (null == v) mPanelSlider.quick = true;
			mPanelSlider.openLeft(mLeftPanel);
		}
	};



	private ListView.OnItemClickListener mDrawerItemClicker = new ListView.OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int tag = 0;
			if (position < mMameItems.size())
				tag = mMameItems.get(position).tag;
			mPanelSlider.clear();
			mPanelList.setItemChecked(position, false);

			//App.showShortToast("tag: " + tag);
			switch (tag) {
				case PANEL_ITEM_SETTINGS:
					showSettings();
					return;
				case PANEL_ITEM_ABOUT:
					showAbout();
					return;
				case PANEL_ITEM_ENCOURAGE:
					showEncourage();
					return;
				case PANEL_ITEM_INVITE:
					showInvite();
					return;
				case PANEL_ITEM_MENTION:
					showMention();
					return;
			}
		}
	};



	public class DrawerAdapter extends ArrayAdapter<MameItem> {

		Context mContext;
		int layoutResourceId;
		ArrayList<MameItem> mItems = null;

		public DrawerAdapter(Context mContext, int layoutResourceId, ArrayList<MameItem> data) {
			super(mContext, layoutResourceId, data);
			this.layoutResourceId = layoutResourceId;
			this.mContext = mContext;
			this.mItems = data;
		}



		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			View listItem = convertView;
			LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
			listItem = inflater.inflate(layoutResourceId, parent, false);

			ImageView imageViewIcon = (ImageView) listItem.findViewById(R.id.imageViewIcon);
			TextView textViewName = (TextView) listItem.findViewById(R.id.textViewName);

			MameItem item = mItems.get(position);
			imageViewIcon.setImageResource(item.icon);
			textViewName.setText(item.name);

			return listItem;
		}
	}



	public class MameItem {
		public int tag, icon;
		public String name;

		// Constructor.
		public MameItem(int tag, int icon, String name) {
			this.tag = tag;
			this.icon = icon;
			this.name = name;
		}
	}



}
