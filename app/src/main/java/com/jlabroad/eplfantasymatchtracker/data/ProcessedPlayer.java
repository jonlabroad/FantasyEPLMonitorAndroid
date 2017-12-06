package com.jlabroad.eplfantasymatchtracker.data;

import com.jlabroad.eplfantasymatchtracker.data.eplapi.Footballer;
import com.jlabroad.eplfantasymatchtracker.data.eplapi.FootballerDetails;

import java.util.ArrayList;
import java.util.List;

public class ProcessedPlayer {
    public FullFootballerData rawData = new FullFootballerData();
    public List<MatchEvent> events = new ArrayList<>();

    public ProcessedPlayer(Footballer footballer, FootballerDetails details, ProcessedPlayer oldData) {
        rawData.footballer = footballer;
        rawData.details = details;
        if (oldData != null) {
            events.addAll(oldData.events);
        }
    }
}
