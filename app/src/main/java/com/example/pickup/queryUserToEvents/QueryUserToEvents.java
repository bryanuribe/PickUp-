package com.example.pickup.queryUserToEvents;

import android.location.Location;
import android.util.Pair;

import com.example.pickup.models.ParseUserToEvent;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;

public class QueryUserToEvents implements Callable<List<Pair<ParseUserToEvent, Integer>>> {

    private static final String TAG = "QueryUserToEvents";

    public static final String QUERY_ALL = "all";
    public static final String QUERY_USER = "userSpecific";

    public static final String AVAILABILITY_GOING = "Going";
    public static final String AVAILABILITY_MAYBE = "Maybe";
    public static final String AVAILABILITY_NO = "No";
    public static final String AVAILABILITY_NA = "NA";

    private String queryType;
    private String availability;
    private Location userLocation;

    public QueryUserToEvents(String queryType, String availability, Location userLocation) {
        this.queryType = queryType;
        this.availability = availability;
        this.userLocation = userLocation;
    }

    private List<Pair<ParseUserToEvent, Integer>> queryUserToEvents() {

        List<ParseUserToEvent> userToEvents;

        // Specify which class to query
        final ParseQuery<ParseUserToEvent> query = ParseQuery.getQuery(ParseUserToEvent.class);
        query.include(ParseUserToEvent.KEY_USER);
        query.include(ParseUserToEvent.KEY_EVENT);
        if (queryType == QUERY_ALL) {}

        else if (queryType == QUERY_USER) {
            query.whereEqualTo(ParseUserToEvent.KEY_USER, ParseUser.getCurrentUser());

            if (availability == AVAILABILITY_GOING) {
                query.whereEqualTo(ParseUserToEvent.KEY_AVAILABILITY, AVAILABILITY_GOING);
            }
            else if (availability == AVAILABILITY_MAYBE) {
                query.whereEqualTo(ParseUserToEvent.KEY_AVAILABILITY, AVAILABILITY_MAYBE);
            }
            else if (availability == AVAILABILITY_NO) {
                query.whereEqualTo(ParseUserToEvent.KEY_AVAILABILITY, AVAILABILITY_NO);
            }
        }

        try {
            userToEvents = query.find();
        } catch (ParseException e) {
            userToEvents = new ArrayList<>();
            e.printStackTrace();
        }

        return sortByDistanceToEvent(userLocation, userToEvents);
    }

    public static List<Pair<ParseUserToEvent, Integer>> sortByDistanceToEvent(Location userLocation, List<ParseUserToEvent> userToEvents) {

        ArrayList<Pair<ParseUserToEvent, Integer>> userToEventsSorted = new ArrayList<>();

        for (ParseUserToEvent userToEvent : userToEvents) {
            int distance = findDistance(userLocation, userToEvent.getEvent().getGeopoint());
            Pair<ParseUserToEvent, Integer> pair = new Pair<>(userToEvent, distance);
            userToEventsSorted.add(pair);
        }

        Collections.sort(userToEventsSorted, new Comparator<Pair>() {
            @Override
            public int compare(Pair p1, Pair p2) {
                Integer p1Second = (Integer) p1.second;
                Integer p2Second = (Integer) p2.second;

                return p1Second.compareTo(p2Second);
            }
        });

        return userToEventsSorted;
    }

    public static int findDistance(Location userLocation, ParseGeoPoint eventLocation) {

        double lon1 = Math.toRadians(userLocation.getLongitude());
        double lon2 = Math.toRadians(eventLocation.getLongitude());
        double lat1 = Math.toRadians(userLocation.getLatitude());
        double lat2 = Math.toRadians(eventLocation.getLatitude());

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in miles
        double r = 3956;

        return (int) Math.ceil(c * r);

    }

    @Override
    public List<Pair<ParseUserToEvent, Integer>> call() throws Exception {
        return queryUserToEvents();
    }
}
