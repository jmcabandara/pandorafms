/*
Pandora FMS - http://pandorafms.com

==================================================
Copyright (c) 2005-2011 Artica Soluciones Tecnologicas
Please see http://pandorafms.org for full contribution list

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public License
as published by the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
 */
package pandroid_event_viewer.pandorafms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Activity with the filter options.
 * 
 * @author Miguel de Dios Matías
 * 
 */
public class Main extends Activity {
	private static String TAG = "MAIN";
	private PandroidEventviewerActivity object;
	private HashMap<Integer, String> pandoraGroups;
	private Spinner comboSeverity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent i = getIntent();
		this.object = (PandroidEventviewerActivity) i
				.getSerializableExtra("object");

		this.pandoraGroups = new HashMap<Integer, String>();

		setContentView(R.layout.main);

		final Button buttonReset = (Button) findViewById(R.id.button_reset);
		final Button buttonSearch = (Button) findViewById(R.id.button_send);
		final Button buttonbuttonSetAsFilterWatcher = (Button) findViewById(R.id.button_set_as_filter_watcher);

		// Check if the user preferences it is set.
		if (object.user.length() == 0 || object.password.length() == 0
				|| object.url.length() == 0) {
			Toast toast = Toast.makeText(this.getApplicationContext(),
					this.getString(R.string.please_set_preferences_str),
					Toast.LENGTH_SHORT);
			toast.show();

			buttonReset.setEnabled(false);
			buttonSearch.setEnabled(false);
			buttonbuttonSetAsFilterWatcher.setEnabled(false);
		} else if (object.user.equals("demo") || object.password.equals("demo")) {
			Toast toast = Toast.makeText(this.getApplicationContext(),
					this.getString(R.string.preferences_set_demo_pandora_str),
					Toast.LENGTH_LONG);
			toast.show();
		} else {
			buttonSearch.setEnabled(false);
			buttonReset.setEnabled(false);
			buttonbuttonSetAsFilterWatcher.setEnabled(false);

			new GetGroupsAsyncTask().execute();
			new GetTagsAsyncTask().execute();
		}

		final SharedPreferences preferences = getSharedPreferences(
				this.getString(R.string.const_string_preferences),
				Activity.MODE_PRIVATE);

		comboSeverity = (Spinner) findViewById(R.id.severity_combo);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				this, R.array.severity_array_values,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		comboSeverity.setAdapter(adapter);

		Spinner combo;
		combo = (Spinner) findViewById(R.id.status_combo);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.event_status_values,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		combo.setAdapter(adapter);
		combo.setSelection(3);

		combo = (Spinner) findViewById(R.id.max_time_old_event_combo);
		adapter = ArrayAdapter.createFromResource(this,
				R.array.max_time_old_event_values,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		combo.setAdapter(adapter);
		combo.setSelection(preferences.getInt("filterLastTime", 6));

		buttonReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				reset_form();
			}
		});

		buttonSearch.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				search_form();
			}
		});

		buttonbuttonSetAsFilterWatcher
				.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						save_filter_watcher();
					}
				});
		LinearLayout advancedOptions = (LinearLayout) findViewById(R.id.show_hide_layout);
		// Show advanced options?
		if (preferences.getBoolean("show_advanced", false)) {
			advancedOptions.setVisibility(View.VISIBLE);
		} else {
			advancedOptions.setVisibility(View.GONE);
			setAdvancedOptionsDefaults();
			clearAdvancedOptions();
		}
		CheckBox cb = (CheckBox) findViewById(R.id.checkBox_advanced_options);
		cb.setChecked(preferences.getBoolean("show_advanced", false));
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				LinearLayout advancedOptions = (LinearLayout) findViewById(R.id.show_hide_layout);
				Editor preferencesEditor = preferences.edit();
				if (isChecked) {
					advancedOptions.setVisibility(View.VISIBLE);
					preferencesEditor.putBoolean("show_advanced", true);
				} else {
					advancedOptions.setVisibility(View.GONE);
					preferencesEditor.putBoolean("show_advanced", false);
					setAdvancedOptionsDefaults();
					clearAdvancedOptions();
				}
				preferencesEditor.commit();
			}
		});

		if (this.object.show_popup_info) {
			this.object.show_popup_info = false;
			i = new Intent(this, About.class);
			startActivity(i);
		}
	}

	public void onRestart() {
		super.onRestart();
		SharedPreferences preferences = getSharedPreferences(
				this.getString(R.string.const_string_preferences),
				Activity.MODE_PRIVATE);
		SharedPreferences.Editor editorPreferences = preferences.edit();
		if (preferences.getBoolean("url_changed", false)) {
			Log.i(TAG, "Getting groups and tags");
			new GetGroupsAsyncTask().execute();
			new GetTagsAsyncTask().execute();
			editorPreferences.putBoolean("url_changed", false);
			editorPreferences.commit();
		}
	}

	/**
	 * Get groups through an api call.
	 * 
	 * @return A list of groups.
	 */
	private List<String> getGroups() {
		ArrayList<String> array = new ArrayList<String>();

		try {
			List<NameValuePair> parameters = new ArrayList<NameValuePair>();
			parameters.add(new BasicNameValuePair("op", "get"));
			parameters.add(new BasicNameValuePair("op2", "groups"));
			parameters.add(new BasicNameValuePair("other_mode",
					"url_encode_separator_|"));
			parameters.add(new BasicNameValuePair("return_type", "csv"));
			parameters.add(new BasicNameValuePair("other", ";"));

			String return_api = Core.httpGet(getApplicationContext(),
					parameters);
			String[] lines = return_api.split("\n");

			for (int i = 0; i < lines.length; i++) {
				String[] groups = lines[i].split(";", 21);

				this.pandoraGroups.put(Integer.valueOf(groups[0]), groups[1]);

				array.add(groups[1]);
			}
		} catch (Exception e) {
			Log.e(TAG + ": getting groups", e.getMessage());
		}

		return array;
	}

	/**
	 * Async task which get groups.
	 * 
	 * @author Miguel de Dios Matías
	 * 
	 */
	private class GetGroupsAsyncTask extends AsyncTask<Void, Void, Void> {
		private List<String> list;

		@Override
		protected Void doInBackground(Void... params) {
			list = getGroups();
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			Spinner combo = (Spinner) findViewById(R.id.group_combo);

			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
					getApplicationContext(),
					android.R.layout.simple_spinner_item, list);
			combo.setAdapter(spinnerArrayAdapter);
			combo.setSelection(0);

			ProgressBar loadingGroup = (ProgressBar) findViewById(R.id.loading_group);

			loadingGroup.setVisibility(ProgressBar.GONE);
			combo.setVisibility(Spinner.VISIBLE);

			Button buttonReset = (Button) findViewById(R.id.button_reset);
			Button buttonSearch = (Button) findViewById(R.id.button_send);
			Button buttonbuttonSetAsFilterWatcher = (Button) findViewById(R.id.button_set_as_filter_watcher);

			buttonReset.setEnabled(true);
			buttonSearch.setEnabled(true);
			buttonbuttonSetAsFilterWatcher.setEnabled(true);
		}
	}

	/**
	 * Async task which get tags.
	 * 
	 * @author Santiago Munín González
	 * 
	 */
	private class GetTagsAsyncTask extends AsyncTask<Void, Void, Void> {
		private List<String> list;

		@Override
		protected Void doInBackground(Void... params) {
			list = API.getTags(getApplicationContext());
			return null;
		}

		@Override
		protected void onPostExecute(Void unused) {
			Spinner combo = (Spinner) findViewById(R.id.tag);

			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
					getApplicationContext(),
					android.R.layout.simple_spinner_item, list);
			combo.setAdapter(spinnerArrayAdapter);
			combo.setSelection(0);

			ProgressBar loadingGroup = (ProgressBar) findViewById(R.id.loading_tag);

			loadingGroup.setVisibility(ProgressBar.GONE);
			combo.setVisibility(Spinner.VISIBLE);
		}
	}

	// For options
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent i;
		switch (item.getItemId()) {
		case R.id.options_button_menu_options:
			startActivity(new Intent(this, Options.class));
			break;
		case R.id.about_button_menu_options:
			i = new Intent(this, About.class);
			startActivity(i);
			break;
		}
		return true;
	}

	/**
	 * Performs the search choice
	 */
	private void search_form() {
		// Clean the EventList
		this.object.eventList = new ArrayList<EventListItem>();

		this.object.loadInProgress = true;

		int timeKey = 0;
		Spinner combo = (Spinner) findViewById(R.id.max_time_old_event_combo);
		timeKey = combo.getSelectedItemPosition();

		this.object.timestamp = Core.convertMaxTimeOldEventValuesToTimestamp(0,
				timeKey);

		EditText text = (EditText) findViewById(R.id.agent_name);
		this.object.agentNameStr = text.getText().toString();

		this.object.id_group = 0;

		combo = (Spinner) findViewById(R.id.group_combo);
		if (combo.getSelectedItem() != null) {
			String selectedGroup = combo.getSelectedItem().toString();

			Iterator<Entry<Integer, String>> it = pandoraGroups.entrySet()
					.iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, String> e = (Map.Entry<Integer, String>) it
						.next();

				if (e.getValue().equals(selectedGroup)) {
					this.object.id_group = e.getKey();
				}
			}
		}

		combo = (Spinner) findViewById(R.id.severity_combo);
		this.object.severity = combo.getSelectedItemPosition() - 1;

		combo = (Spinner) findViewById(R.id.status_combo);
		this.object.status = combo.getSelectedItemPosition() - 0;

		text = (EditText) findViewById(R.id.event_search_text);
		this.object.eventSearch = text.getText().toString();

		combo = (Spinner) findViewById(R.id.tag);
		if (combo.getSelectedItem() != null) {
			this.object.eventTag = combo.getSelectedItem().toString();
		}
		this.object.getNewListEvents = true;
		this.object.executeBackgroundGetEvents();

		TabActivity ta = (TabActivity) this.getParent();
		ta.getTabHost().setCurrentTab(1);
	}

	/**
	 * Saves filter data
	 */
	private void save_filter_watcher() {
		String filterAgentName = "";
		int filterIDGroup = 0;
		int filterSeverity = -1;
		int filterStatus = -1;
		int filterLastTime = 0;
		String filterEventSearch = "";
		String filterTag = "";

		EditText text = (EditText) findViewById(R.id.agent_name);
		filterAgentName = text.getText().toString();

		Spinner combo;
		combo = (Spinner) findViewById(R.id.group_combo);
		if ((combo != null) && (combo.getSelectedItem() != null)) {
			String selectedGroup = combo.getSelectedItem().toString();

			Iterator<Entry<Integer, String>> it = pandoraGroups.entrySet()
					.iterator();
			while (it.hasNext()) {
				Map.Entry<Integer, String> e = it.next();

				if (e.getValue().equals(selectedGroup)) {
					filterIDGroup = e.getKey();
				}
			}
		}

		combo = (Spinner) findViewById(R.id.severity_combo);
		filterSeverity = combo.getSelectedItemPosition() - 1;

		combo = (Spinner) findViewById(R.id.status_combo);
		filterStatus = combo.getSelectedItemPosition() - 0;

		combo = (Spinner) findViewById(R.id.max_time_old_event_combo);
		filterLastTime = combo.getSelectedItemPosition();

		combo = (Spinner) findViewById(R.id.tag);
		filterTag = combo.getSelectedItem().toString();

		text = (EditText) findViewById(R.id.event_search_text);
		filterEventSearch = text.getText().toString();

		SharedPreferences preferences = getSharedPreferences(
				this.getString(R.string.const_string_preferences),
				Activity.MODE_PRIVATE);
		SharedPreferences.Editor editorPreferences = preferences.edit();

		editorPreferences.putString("filterAgentName", filterAgentName);
		editorPreferences.putInt("filterIDGroup", filterIDGroup);
		editorPreferences.putInt("filterSeverity", filterSeverity);
		editorPreferences.putInt("filterStatus", filterStatus);
		editorPreferences.putString("filterTag", filterTag);
		editorPreferences.putString("filterEventSearch", filterEventSearch);
		editorPreferences.putInt("filterLastTime", filterLastTime);

		if (editorPreferences.commit()) {
			Core.setFetchFrequency(getApplicationContext());
			Toast toast = Toast.makeText(getApplicationContext(),
					this.getString(R.string.filter_update_succesful_str),
					Toast.LENGTH_SHORT);
			toast.show();
		} else {
			Toast toast = Toast.makeText(getApplicationContext(),
					this.getString(R.string.filter_update_fail_str),
					Toast.LENGTH_SHORT);
			toast.show();
		}
	}

	/**
	 * Resets the filter form.
	 * 
	 */
	private void reset_form() {
		Spinner combo = (Spinner) findViewById(R.id.group_combo);
		combo.setSelection(0);
		combo = (Spinner) findViewById(R.id.status_combo);
		combo.setSelection(3);
		combo = (Spinner) findViewById(R.id.tag);
		combo.setSelection(0);

		EditText text = (EditText) findViewById(R.id.agent_name);
		text.setText("");

		combo = (Spinner) findViewById(R.id.severity_combo);
		combo.setSelection(0);

		combo = (Spinner) findViewById(R.id.max_time_old_event_combo);
		combo.setSelection(6);

		text = (EditText) findViewById(R.id.event_search_text);
		text.setText("");

	}

	/**
	 * Clears advanced options.
	 */
	private void clearAdvancedOptions() {

		EditText text = (EditText) findViewById(R.id.agent_name);
		text.setText("");

		Spinner combo = (Spinner) findViewById(R.id.severity_combo);
		combo.setSelection(0);

		combo = (Spinner) findViewById(R.id.max_time_old_event_combo);
		combo.setSelection(6);

		text = (EditText) findViewById(R.id.event_search_text);
		text.setText("");
	}

	/**
	 * Puts advanced options to default values.
	 */
	private void setAdvancedOptionsDefaults() {
		SharedPreferences preferences = getSharedPreferences(
				this.getString(R.string.const_string_preferences),
				Activity.MODE_PRIVATE);
		SharedPreferences.Editor editorPreferences = preferences.edit();

		editorPreferences.putString("filterAgentName", "");
		editorPreferences.putInt("filterIDGroup", 0);
		editorPreferences.putInt("filterSeverity", -1);
		editorPreferences.putString("filterEventSearch", "");
		editorPreferences.putInt("filterLastTime", 6);
		// There were changes
		editorPreferences.putBoolean("filterChanges", true);
		editorPreferences.commit();
	}
}