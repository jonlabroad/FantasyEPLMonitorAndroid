package com.jlabroad.eplfantasymatchtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;
import com.jlabroad.eplfantasymatchtracker.config.CloudAppConfig;
import com.jlabroad.eplfantasymatchtracker.config.CloudAppConfigProvider;
import com.jlabroad.eplfantasymatchtracker.config.DeviceConfig;
import com.jlabroad.eplfantasymatchtracker.config.DeviceConfigurator;
import com.jlabroad.eplfantasymatchtracker.config.GlobalConfig;
import com.jlabroad.eplfantasymatchtracker.data.MatchEvent;
import com.jlabroad.eplfantasymatchtracker.data.MatchEventType;
import com.jlabroad.eplfantasymatchtracker.data.MatchInfo;
import com.jlabroad.eplfantasymatchtracker.data.ProcessedPick;
import com.jlabroad.eplfantasymatchtracker.data.ProcessedPlayer;
import com.jlabroad.eplfantasymatchtracker.data.ProcessedTeam;
import com.jlabroad.eplfantasymatchtracker.data.Team;
import com.jlabroad.eplfantasymatchtracker.aws.ApplicationEndpointRegister;
import com.jlabroad.eplfantasymatchtracker.aws.Credentials;
import com.jlabroad.eplfantasymatchtracker.data.TeamMatchEvent;
import com.jlabroad.eplfantasymatchtracker.data.eplapi.Event;
import com.jlabroad.eplfantasymatchtracker.data.eplapi.Pick;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import static com.jlabroad.eplfantasymatchtracker.notification.MatchDataAlertReceiver.EPL_FIREBASE_DATA_MESSAGE;

public class MatchView extends AppCompatActivity {
    public static final String TEAM_CHOOSE_REQUEST = "com.jlabroad.eplfantasymatchtracker.TEAM_CHOOSE_REQUEST";

    private int _leagueId = 5815;
    private int _teamId;
    BroadcastReceiver _matchAlertDataReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_view);

        // TODO Get rid of this and do it the right way (async network)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            registerDevice();
            readAppConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO really gotta make this better!
        if (GlobalConfig.deviceConfig.getAllTeamIds().isEmpty()) {
            Intent intent = new Intent(getApplicationContext(), ChooseTeamActivity.class);
            intent.putExtra(TEAM_CHOOSE_REQUEST, "testtesttest");
            startActivity(intent);
        } else {
            getTeamId();
            createUpdateReceiver();
            subscribeToUpdates();
            readLatestData();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                readLatestData();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tracker_action_menu, menu);
        return true;
    }

    private void getTeamId() {
        DeviceConfig config = GlobalConfig.deviceConfig;
        HashSet<Integer> teamIds = config.getAllTeamIds();

        //Just pick the first, we only support one subscription at this time
        _teamId = teamIds.toArray(new Integer[0])[0];
    }

    public void readLatestData() {
        MatchInfo matchInfo = null;
        try {
            String bucketData = readBucket();
            matchInfo = new Gson().fromJson(bucketData, MatchInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        printMatchInfo(matchInfo);
    }

    protected String readBucket() throws IOException {
        AmazonS3 s3 = new AmazonS3Client(Credentials.instance().creds);
        S3Object object = s3.getObject(GlobalConfig.S3Bucket, String.format(GlobalConfig.MatchInfoRootFmt, _leagueId, _teamId, GlobalConfig.cloudAppConfig.CurrentGameWeek));
        InputStream objectData = object.getObjectContent();
        String retString = readTextInputStream(objectData);
        objectData.close();

        return retString;
    }

    private static String readTextInputStream(InputStream input)
            throws IOException {
        // Read one text line at a time and display.
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String retLine = "";
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;

            retLine += line + "\n";
        }
        return retLine;
    }

    private void printMatchInfo(MatchInfo info) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.livescorelayout);
        if (info == null) {
            linearLayout.removeAllViewsInLayout();
            return;
        }

        ArrayList<Integer> teamIds = new ArrayList<>();
        teamIds.addAll(info.teams.keySet());

        int team1Id = teamIds.get(0);
        int team2Id = teamIds.get(1);
        ProcessedTeam team1 = info.teams.get(team1Id);
        ProcessedTeam team2 = info.teams.get(team2Id);

        TextView team1Name = (TextView) findViewById(R.id.team1name);
        TextView team2Name = (TextView) findViewById(R.id.team2name);
        team1Name.setText(team1.standing.entry_name);
        team2Name.setText(team2.standing.entry_name);

        TextView team1Record = (TextView) findViewById(R.id.team1record);
        TextView team2Record = (TextView) findViewById(R.id.team2record);
        team1Record.setText(String.format("W%d-L%d-D%d",
                team1.standing.matches_won,
                team1.standing.matches_lost,
                team1.standing.matches_drawn));
        team2Record.setText(String.format("W%d-L%d-D%d",
                team2.standing.matches_won,
                team2.standing.matches_lost,
                team2.standing.matches_drawn));

        TextView scoreLine = (TextView) findViewById(R.id.scoreText);
        scoreLine.setText(String.format("(%d) %d - %d (%d)",
                team1.score.subScore, team1.score.startingScore,
                team2.score.startingScore, team2.score.subScore));

        linearLayout.removeAllViewsInLayout();
        ArrayList<TextView> textViews = new ArrayList<>();
        List<TeamMatchEvent> allEvents = info.allEvents;
        for (TeamMatchEvent event : allEvents) {
            List<ProcessedTeam> teamsWithFootballer = getTeamsWithFootballer(event.footballerId, info.teams.values());
            TextView textView = new TextView(this);
            textView.setText(eventToString(event, teamsWithFootballer.get(0)));
            textView.setBackgroundResource(getEventColor(event));
            if (event.isCaptain) {
                textView.setTypeface(null, Typeface.BOLD);
            }
            textViews.add(textView);
        }

        for (TextView v : textViews) {
            linearLayout.addView(v);
        }
    }

    private List<ProcessedTeam> getTeamsWithFootballer(int footballerId, Collection<ProcessedTeam> teams) {
        List<ProcessedTeam> retList = new ArrayList<>();
        for (ProcessedTeam team : teams) {
            if (team.getPick(footballerId) != null) {
                retList.add(team);
            }
        }
        return retList;
    }

    private int getEventColor(TeamMatchEvent event) {
        if (event.teamId < 0) {
            return R.color.bothteamcolor;
        }

        if (event.teamId == _teamId) {
            return R.color.myteamcolor;
        }

        return R.color.otherteamcolor;
    }

    private String eventToString(TeamMatchEvent event, ProcessedTeam team) {
        if (isEnumerated(event.type)) {
            return String.format("%s: %d %s %s (%d%s)", stringDateToReadableString(event.dateTime),
                    event.number, event.typeToReadableString(event.number),
                    event.footballerName, event.pointDifference, getMultiplerString(event));
        }
        else {
            return String.format("%s: %s %s (%d%s)", stringDateToReadableString(event.dateTime),
                    event.typeToReadableString(event.number),
                    event.footballerName, event.pointDifference, getMultiplerString(event));
        }
    }

    private String getMultiplerString(TeamMatchEvent event) {
        String multiplierString = "";
        if (event.multiplier > 1) {
            multiplierString = String.format(" x %d", event.multiplier);
        }
        return multiplierString;
    }

    private boolean isEnumerated(MatchEventType type) {
        return  type == MatchEventType.GOAL ||
                type == MatchEventType.ASSIST ||
                type == MatchEventType.YELLOW_CARD ||
                type == MatchEventType.RED_CARD ||
                type == MatchEventType.PENALTY_MISS ||
                type == MatchEventType.GOALS_CONCEDED ||
                type == MatchEventType.SAVES ||
                type == MatchEventType.PENALTY_SAVES ||
                type == MatchEventType.OWN_GOALS ||
                type == MatchEventType.MINUTES_PLAYED;
    }

    private void createUpdateReceiver() {
        _matchAlertDataReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                readLatestData();
            }
        };
    }

    private void subscribeToUpdates() {
        LocalBroadcastManager.getInstance(this).registerReceiver((_matchAlertDataReceiver),
                new IntentFilter(EPL_FIREBASE_DATA_MESSAGE)
        );
    }

    private void registerDevice() throws IOException {
        Credentials.instance().initializeCognitoProvider(getApplicationContext());
        GlobalConfig.deviceConfig = new DeviceConfigurator().readConfig(getUniqueDeviceId());
    }

    private void readAppConfig() {
        GlobalConfig.cloudAppConfig = new CloudAppConfigProvider().read();
    }

    private  String getUniqueDeviceId() {
        return Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private String stringDateToReadableString(String fullDate) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        Date date;
        try {
            date = fmt.parse(fullDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return "";
        }
        SimpleDateFormat newFmt = new SimpleDateFormat("EEE HH:mm");
        newFmt.setTimeZone(TimeZone.getTimeZone("America/New York"));
        return newFmt.format(date);
    }
}
