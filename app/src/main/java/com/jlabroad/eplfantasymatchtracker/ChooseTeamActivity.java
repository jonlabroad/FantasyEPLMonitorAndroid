package com.jlabroad.eplfantasymatchtracker;

import android.app.Activity;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.jlabroad.eplfantasymatchtracker.config.CloudAppConfig;
import com.jlabroad.eplfantasymatchtracker.config.DeviceConfigurator;
import com.jlabroad.eplfantasymatchtracker.config.GlobalConfig;
import com.jlabroad.eplfantasymatchtracker.data.TeamIdName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChooseTeamActivity extends Activity {

    private ArrayList<TeamIdName> _teams = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_team);

        final ListView listview = (ListView) findViewById(R.id.team_list_view);

        for (TeamIdName teamIdName : GlobalConfig.cloudAppConfig.AvailableTeams.values()) {
            _teams.add(teamIdName);
        }

        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, _teams);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final TeamIdName item = (TeamIdName) parent.getItemAtPosition(position);
                view.animate().setDuration(500).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                GlobalConfig.deviceConfig.addSubscription(item.teamId, item.teamName);
                                new DeviceConfigurator().writeConfig(GlobalConfig.deviceConfig, getUniqueDeviceId());

                                // Go to main activity
                                //Intent intent = new Intent(getApplicationContext(), MatchView.class);
                                //startActivity(intent);
                                finish();
                            }
                        });
            }
        });
    }

    private String getUniqueDeviceId() {
        return Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private class StableArrayAdapter extends ArrayAdapter<TeamIdName> {

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  ArrayList<TeamIdName> objects) {
            super(context, textViewResourceId, objects);
        }

        @Override
        public long getItemId(int position) {
            TeamIdName item = getItem(position);
            return item.teamId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String teamText = String.format("%s - %s",_teams.get(position).teamName, _teams.get(position).teamOwner);
            TextView newView = null;
            if (convertView == null) {
                newView = new TextView(getApplicationContext());
            }
            else {
                newView = (TextView) convertView;
            }
            newView.setText(teamText);
            newView.setTextColor(Color.BLACK);
            newView.setTextSize(22.0f);
            newView.setPadding(0, 20, 0, 20);
            return newView;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
