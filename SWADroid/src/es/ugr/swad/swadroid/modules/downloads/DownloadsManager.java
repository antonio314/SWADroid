/*
 *  This file is part of SWADroid.
 *
 *  Copyright (C) 2012 Helena Rodriguez Gijon <hrgijon@gmail.com>
 *
 *  SWADroid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  SWADroid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with SWADroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package es.ugr.swad.swadroid.modules.downloads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import es.ugr.swad.swadroid.Constants;
import es.ugr.swad.swadroid.R;
import es.ugr.swad.swadroid.gui.MenuActivity;
import es.ugr.swad.swadroid.model.Group;
import es.ugr.swad.swadroid.model.GroupType;
import es.ugr.swad.swadroid.modules.GroupTypes;
import es.ugr.swad.swadroid.modules.Groups;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity to navigate through the directory tree of documents and to manage
 * the downloads of documents
 *
 * @author Helena Rodriguez Gijon <hrgijon@gmail.com>
 * @author Juan Miguel Boyero Corral <juanmi1982@gmail.com>
 */
public class DownloadsManager extends MenuActivity {
    /**
     * Class that contains the directory tree and gives information of each
     * level
     */
    private DirectoryNavigator navigator;

    /**
     * Specifies whether to display the documents or the shared area of the
     * subject 1 specifies documents area 2 specifies shared area
     */
    private int downloadsAreaCode = 0;
    /**
     * Specifies chosen group to show its documents
     * 0 -
     */
    private long chosenGroupCode = 0;
    /**
     * String that contains the xml files recevied from the web service
     */
    private String tree = null;

    /**
     * Downloads tag name for Logcat
     */
    private static final String TAG = Constants.APP_TAG + " Downloads";

    /**
     * List of group of the selected course to which the user belongs
     */
    private List<Group> myGroups;

    /**
     * Indicates if the groups has been requested
     */
    private boolean groupsRequested = false;

    /**
     * Indicates whether the refresh button was pressed
     */
    private boolean refresh = false;

    private TextView noConnectionText;
    private GridView grid;

    private TextView currentPathText;

    private String chosenNodeName = null;


    /**
     * fileSize stores the size of the last file name chosen to be downloaded
     */
    private long fileSize = 0;

    /**
     * Indicates the selected position in the groups spinner
     * by default the whole course is selected
     */
    private int groupPosition = 0;
    /**
     * Indicates if the menu no connection is visible
     */
    private boolean noConnectionView = false;
    /**
     * Indicates that the current state should be saved in case the activity is brought to background
     */
    private boolean saveState = false;

    /**
     * Indicates if the state before the activity was brought to background has o not connection
     */
    private boolean previousConnection = false;

    @Override
    protected void onStart() {
        super.onStart();
        List<Group> allGroups = dbHelper.getGroups(Constants.getSelectedCourseCode());
        int nGroups = allGroups.size();

        if (!saveState) {
            if (nGroups != 0 || groupsRequested) { //groupsRequested is used to avoid continue requests of groups on courses that have not any group.
                myGroups = getFilteredGroups(); //only groups where the user is enrolled.
                int nMyGroups = myGroups.size();
                this.loadGroupsSpinner(myGroups);
                // the tree request must be explicit only when there are not any groups(where the user is enrolled), and therefore any Spinner.
                //in case there are groups(where the user is enrolled), it will be a spinner, and the tree request will be automatic made by OnItemSelectedListener
                if (nMyGroups == 0 && tree == null)
                    requestDirectoryTree();
            } else {
                Intent activity = new Intent(this, GroupTypes.class);
                activity.putExtra("courseCode", Constants.getSelectedCourseCode());
                startActivityForResult(activity, Constants.GROUPTYPES_REQUEST_CODE);
            }
        } else {
            myGroups = getFilteredGroups();
            this.loadGroupsSpinner(myGroups);
            if (previousConnection) {
                setMainView();
            } else {
                setNoConnectionView();
            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            this.saveState = savedInstanceState.getBoolean("saveState", false);
            if (saveState) {
                this.groupsRequested = true;
                this.previousConnection = savedInstanceState.getBoolean("previousConnection", false);
                this.chosenGroupCode = savedInstanceState.getLong("chosenGroupCode", 0);
                this.groupPosition = savedInstanceState.getInt("groupPosition", 0);
                if (previousConnection) {
                    this.tree = savedInstanceState.getString("tree");
                    String path = savedInstanceState.getString("path");
                    this.navigator = new DirectoryNavigator(this.tree);
                    if (path.equals("/")) {
                        int firstBar = path.indexOf('/', 0);
                        int nextBar = path.indexOf('/', firstBar + 1);
                        while (nextBar != -1) {
                            String dir = path.substring(firstBar + 1, nextBar);
                            this.navigator.goToSubDirectory(dir);
                            firstBar = nextBar;
                            nextBar = path.indexOf('/', firstBar + 1);
                        }
                    }
                }
            }

        }
        setContentView(R.layout.navigation);

        downloadsAreaCode = getIntent().getIntExtra("downloadsAreaCode",
                Constants.DOCUMENTS_AREA_CODE);

        noConnectionText = (TextView) this.findViewById(R.id.noConnectionText);

        grid = (GridView) this.findViewById(R.id.gridview);
        grid.setOnItemClickListener((new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                //TextView text = (TextView) v.findViewById(R.id.icon_text);
                //chosenNodeName = text.getText().toString();
                //DirectoryItem node = navigator.getDirectoryItem(chosenNodeName);
                DirectoryItem node = navigator.getDirectoryItem(position);
                if (node.getFileCode() == -1) //it is a directory therefore navigates into it
                    updateView(navigator.goToSubDirectory(position));
                    //updateView(navigator.goToSubDirectory(chosenNodeName));
                else { //it is a files therefore gets its information through web service GETFILE
                    chosenNodeName = node.getName();
                    AlertDialog fileInfoDialog = createFileInfoDialog(node.getName(), node.getSize(), node.getTime(), node.getPublisher(), node.getFileCode(), node.getLicense());
                    fileInfoDialog.show();
                }
            }
        }));

        ImageButton homeButton = (ImageButton) this
                .findViewById(R.id.home_button);
        homeButton.setOnClickListener((new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (navigator != null) {
                    updateView(navigator.goToRoot());
                }
            }

        }));

        ImageButton parentButton = (ImageButton) this
                .findViewById(R.id.parent_button);
        parentButton.setOnClickListener((new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (navigator != null) {
                    updateView(navigator.goToParentDirectory());
                }
            }

        }));

        setupActionBar();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
    	//If back button is pressed, go to parent directory
	    if ((keyCode == KeyEvent.KEYCODE_BACK))
	    {
	    	if (navigator != null) {
	    		//If current directory is not the root, go to parent directory
	    		if (!navigator.isRootDirectory()) {
	                updateView(navigator.goToParentDirectory());
	             //If current directory is the root, exit module
	    		} else {
	    			return super.onKeyDown(keyCode, event);
	    		}
            }
		
		    return true;	
	    }
	
	    return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("saveState", this.saveState);
        if (this.saveState) {
            outState.putBoolean("previousConnection", this.previousConnection);
            outState.putLong("chosenGroupCode", this.chosenGroupCode);
            outState.putInt("groupPosition", this.groupPosition);
            if (this.previousConnection) {
                outState.putString("tree", this.tree);
                outState.putString("path", this.navigator.getPath());
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                // After get the list of courses, a dialog is launched to choice the
                // course
                case Constants.DIRECTORY_TREE_REQUEST_CODE:
                    tree = data.getStringExtra("tree");
                    if (!refresh) {
                        setMainView();
                    } else {
                        refresh = false;
                        if (!noConnectionView)
                            refresh();
                        else
                            setMainView();
                    }
                    break;
                case Constants.GETFILE_REQUEST_CODE:
                    Log.d(TAG, "Correct get file");
                    //if the sd card is not busy, the file can be downloaded
                    if (this.checkMediaAvailability() == 2) {
                        Log.i(TAG, "External storage is available");
                        String url = data.getExtras().getString("link");
                        downloadFile(getDirectoryPath(), url, fileSize);
                        //Toast.makeText(this, chosenNodeName +" "+ this.getResources().getString(R.string.notificationDownloadTitle) , Toast.LENGTH_LONG).show();
                    } else { //if the sd card is busy, it shows a alert dialog
                        Log.i(TAG, "External storage is NOT available");
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        AlertDialog dialog;
                        builder.setTitle(R.string.sdCardBusyTitle);
                        builder.setMessage(R.string.sdCardBusy);
                        builder.setIcon(android.R.drawable.ic_dialog_alert);
                        builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                        dialog = builder.create();
                        dialog.show();
                    }
                    break;
                case Constants.GROUPS_REQUEST_CODE:
                    groupsRequested = true;
                    myGroups = getFilteredGroups(); //only groups where the user is enrolled.
                    this.loadGroupsSpinner(myGroups);
                    if (myGroups.size() == 0)
                        requestDirectoryTree();
                    break;
                case Constants.GROUPTYPES_REQUEST_CODE:
                    Intent activity = new Intent(this, Groups.class);
                    activity.putExtra("courseCode", Constants.getSelectedCourseCode());
                    startActivityForResult(activity, Constants.GROUPS_REQUEST_CODE);
                    break;
            }

        } else {
            setNoConnectionView();
            if (refresh) {
                refresh = false;
            }
        }
    }


    /**
     * Having connection is mandatory for the Download Module.
     * Therefore when there is not connection, the grid of nodes is disabled and instead it is showed an info messages
     */
    private void setNoConnectionView() {
        noConnectionView = true;
        noConnectionText.setVisibility(View.VISIBLE);
        grid.setVisibility(View.GONE);

        this.findViewById(R.id.groupSpinner).setVisibility(View.GONE);

        getSupportActionBar().setSubtitle(Constants.getSelectedCourseShortName());
        
        this.saveState = true;
        this.previousConnection = false;

    }

    /**
     * This method set the grid of nodes visible and paints the directory tree in its root node
     */
    private void setMainView() {

        noConnectionText.setVisibility(View.GONE);
        grid.setVisibility(View.VISIBLE);

        noConnectionView = false;

        currentPathText = (TextView) this.findViewById(R.id.path);

        ArrayList<DirectoryItem> items;
        if (!(this.saveState && this.previousConnection)) {
            navigator = new DirectoryNavigator(tree);
            items = navigator
                    .goToRoot();
        } else {
            items = navigator.goToCurrentDirectory();
        }

        currentPathText.setText(navigator.getPath());
        grid.setAdapter(new NodeAdapter(this, items));
        //this is used for the activity restart in case it was taken background
        this.saveState = true;
        this.previousConnection = true;
    }

    /**
     * This method is called after the new file tree is received when the refresh button is pressed
     */
    private void refresh() {
        if (navigator != null) {
            navigator.refresh(tree);
        }
    }

    /**
     * When the user moves into a new directory, this method updates the set of new directories and files and paints it
     */
    private void updateView(ArrayList<DirectoryItem> items) {
        currentPathText.setText(navigator.getPath());
        ((NodeAdapter) grid.getAdapter()).change(items);

    }

    /**
     * Get the list of the groups of the course with a documents zone to whom the user belongs
     */
    private List<Group> getFilteredGroups() {
        List<Group> currentGroups = dbHelper.getGroups(Constants.getSelectedCourseCode());
        //remove groups that do not have a file zone assigned
        int j = 0;
        while (j < currentGroups.size()) {
            if (currentGroups.get(j).getDocumentsArea() != 0 && currentGroups.get(j).isMember())
                ++j;
            else
                currentGroups.remove(j);
        }
        return currentGroups;
    }

    /**
     * If there are not groups to which the user belong in the database, it makes the request
     */
    private void loadGroupsSpinner(List<Group> currentGroups) {

        if (!currentGroups.isEmpty()) { //there are groups in the selected course, therefore the groups spinner should be loaded
            Spinner groupsSpinner = (Spinner) this.findViewById(R.id.groupSpinner);
            groupsSpinner.setVisibility(View.VISIBLE);

            ArrayList<String> spinnerNames = new ArrayList<String>(currentGroups.size() + 1);
            spinnerNames.add(getString(R.string.course) + "-" + Constants.getSelectedCourseShortName());
            for (Group g : currentGroups) {
                GroupType gType = dbHelper.getGroupTypeFromGroup(g.getId());
                spinnerNames.add(getString(R.string.group) + "-" + gType.getGroupTypeName() + " " + g.getGroupName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            groupsSpinner.setAdapter(adapter);
            groupsSpinner.setOnItemSelectedListener(new onGroupSelectedListener());
            groupsSpinner.setSelection(groupPosition);
        } else {
            this.findViewById(R.id.groupSpinner).setVisibility(View.GONE);

            getSupportActionBar().setSubtitle(Constants.getSelectedCourseShortName());
        }
    }

    /**
     * Listener associated with the spinner. With a new group / course is selected, it is requested the right file tree
     */
    private class onGroupSelectedListener implements OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position,
                                   long id) {
            //if the position is 0, it is chosen the whole course. Otherwise a group has been chosen
            //position - 0 belongs to the whole course
            long newGroupCode = position == 0 ? 0 : myGroups.get(position - 1).getId();
            if (chosenGroupCode != newGroupCode || tree == null) {
                chosenGroupCode = newGroupCode;
                groupPosition = position;
                requestDirectoryTree();
            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }

    }

    /**
     * Method to request files tree to SWAD thought the web services GETDIRECTORYTREE
     */

    private void requestDirectoryTree() {
        Intent activity;
        activity = new Intent(this, DirectoryTreeDownload.class);
        activity.putExtra("treeCode", downloadsAreaCode);
        activity.putExtra("groupCode", (int) chosenGroupCode);
        startActivityForResult(activity, Constants.DIRECTORY_TREE_REQUEST_CODE);
    }

    /**
     * It checks if the external storage is available
     *
     * @return 0 - if external storage can not be read either wrote
     *         1 - if external storage can only be read
     *         2 - if external storage can be read and wrote
     */

    private int checkMediaAvailability() {
        String state = Environment.getExternalStorageState();
        int returnValue;
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            returnValue = 2;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            returnValue = 1;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            returnValue = 0;
        }
        return returnValue;
    }

    /**
     * it gets the directory path where the files will be located.This will be /$EXTERNAL_STORAGE/$DOWNLOADS
     */
    private String getDirectoryPath() {
        //String downloadsDirName = Environment.getExternalStorageDirectory()+File.separator+"download";
        return getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
    }

    /**
     * it initializes the download the file from the url @a url and stores it in the directory name @directory
     *
     * @param directory - directory where the downloaded file will be stored
     * @param url       - url from which the file is downloaded
     * @param fileSize  - file size of the file. It is used to show the download progress in the notification
     */
    private void downloadFile(String directory, String url, long fileSize) {
        new FileDownloaderAsyncTask(this, this.chosenNodeName, true, fileSize).execute(directory, url);
    }

    /**
     * Method to request info file identified with @a fileCode to SWAD thought the web services GETFILE
     *
     * @param fileCode file code
     */
    private void requestGetFile(long fileCode) {
        Intent activity;
        activity = new Intent(this, GetFile.class);
        activity.putExtra("fileCode", fileCode);
        //activity.putExtra("path", navigator.getPath() + fileName);
        startActivityForResult(activity, Constants.GETFILE_REQUEST_CODE);
    }

    /**
     * Method that shows information file and allows its download
     * It has a button to confirm the download. If It is confirmed  getFile will be requested to get the link
     */
    private AlertDialog createFileInfoDialog(String name, long size, long time, String uploader,
            long fileCode, String license) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        final long code = fileCode;
        this.fileSize = size;

        Date d = new Date(time * 1000);
        java.text.DateFormat dateShortFormat = android.text.format.DateFormat.getDateFormat(this);
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this);

        String uploaderName;
        if (uploader.compareTo("") != 0)
            uploaderName = uploader;
        else
            uploaderName = this.getResources().getString(R.string.unknown);

        StringBuilder message;
        
        Resources res = getResources();
        
        message = new StringBuilder(res.getString(R.string.fileTitle))
                .append(" ")
                .append(name)
                .append("\n")
                .append(getString(R.string.sizeFileTitle))
                .append(" ")
                .append(humanReadableByteCount(size, true))
                .append("\n")
                .append(res.getString(R.string.uploaderTitle))
                .append(" ")
                .append(uploaderName)
                .append("\n")
                .append(res.getString(R.string.licenseType))
                .append(" ")
                .append(license)
                .append("\n")
                .append(res.getString(R.string.creationTimeTitle))
                .append(" ")
                .append(dateShortFormat.format(d))
                .append("  ")
                .append(timeFormat.format(d));
        
        builder.setTitle(name);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.downloadFileTitle, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                requestGetFile(code);
            }
        });
        builder.setNegativeButton(R.string.cancelMsg, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });

        dialog = builder.create();
        return dialog;
    }

    /**
     * Method to show file size in bytes in a human readable way
     * http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
     */
    private static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * Launches an action when refresh button is pushed.
     * <p/>
     * The listener onClick is set in action_bar.xml
     *
     * @param v Actual view
     */
    public void onRefreshClick(View v) {

        refresh = true;

        Intent activity = new Intent(this, GroupTypes.class);
        activity.putExtra("courseCode", Constants.getSelectedCourseCode());
        startActivityForResult(activity, Constants.GROUPTYPES_REQUEST_CODE);

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
		
        if(downloadsAreaCode == 1) {
			setTitle(R.string.documentsDownloadModuleLabel);
			getSupportActionBar().setIcon(R.drawable.folder);
        } else {
			setTitle(R.string.sharedsDownloadModuleLabel);
			getSupportActionBar().setIcon(R.drawable.folder_users);
		}
    }
    
//	/**
//	 * This method is launched instead of onCreate when device rotates
//	 * It prevents from repeating calls to web services when they are not necessary
//	 * */
//	@Override
//	public void onConfigurationChanged(Configuration newConfig) {        
//	    super.onConfigurationChanged(newConfig);
//	    Log.i(TAG,"Device rotation");
//	    // Checks the orientation of the screen
///*	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//	        Log.i(TAG,"onConfigChanged - Landscape");
//	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
//	    	Log.i(TAG,"onConfigChanged - Portrait");
//	    }*/
//	}
}
