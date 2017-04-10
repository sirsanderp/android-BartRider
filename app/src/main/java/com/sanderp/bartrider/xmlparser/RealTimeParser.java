package com.sanderp.bartrider.xmlparser;

import android.util.Log;
import android.util.Xml;

import com.sanderp.bartrider.structure.TripEstimate;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Returns the Estimated Time of Departure (ETD) for the specified station.
 */
public class RealTimeParser {
    private static final String TAG = "RealTimeParser";

    // Important XML field names
    private static final String STATION = "station";
    private static final String ESTIMATE_TIME_TO_DEPARTURE = "etd";
    private static final String ABBREVIATION = "abbreviation";
    private static final String ESTIMATE = "estimate";
    private static final String ERROR = "error";

    private static final String ns = null;

    // Other variables
    private static String trainHeadStation;

    public List<TripEstimate> parse(InputStream in, String trainHeadStation) throws XmlPullParserException, IOException {
        Log.i(TAG, "Parsing Real-Time Estimates API...");
        this.trainHeadStation = trainHeadStation;
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);

            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                if (parser.getName().equals(STATION)) break;
            }
            return readAPI(parser);
        } finally {
            in.close();
        }
    }

    private List<TripEstimate> readAPI(XmlPullParser parser) throws XmlPullParserException, IOException {
        Log.i(TAG, "Parsing <station> tag...");
        parser.require(XmlPullParser.START_TAG, ns, STATION);
        while (parser.next() !=  XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            if (name.equals(ERROR)) {
                parser.next();
                if (parser.getText().equals("Updates are temporarily unavailable.")) {
                    Log.e(TAG, "Real time estimates API is down");
                    return null;
                }
            } else if (name.equals(ESTIMATE_TIME_TO_DEPARTURE)) {
                return readEstimatedDepartures(parser);
            }
        }
        return null;
    }

    private List<TripEstimate> readEstimatedDepartures(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<TripEstimate> estimates = new ArrayList<>();
        boolean isTrainHeadStation = false;

        Log.i(TAG, "Parsing <etd> tag...");
        parser.require(XmlPullParser.START_TAG, ns, ESTIMATE_TIME_TO_DEPARTURE);
        while (!isTrainHeadStation && parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String name = parser.getName();
            if (name.equals(ABBREVIATION)) {
                parser.next();
                if (parser.getText().equals(trainHeadStation)) {
                    isTrainHeadStation = true;
                }
            }
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(ESTIMATE_TIME_TO_DEPARTURE)) break;
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if (parser.getName().equals(ESTIMATE)) estimates.add(readEstimate(parser));
        }
        return estimates;
    }

    private TripEstimate readEstimate(XmlPullParser parser) throws XmlPullParserException, IOException {
        TripEstimate estimate = new TripEstimate();

        Log.i(TAG, "Parsing <estimate> tag...");
        parser.require(XmlPullParser.START_TAG, ns, ESTIMATE);
        while (parser.next() !=  XmlPullParser.END_DOCUMENT) {
            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals(ESTIMATE)) break;
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String attr = parser.getName();
            parser.next();
            String value = parser.getText();
            Log.d(TAG, attr + ": " + value);
            switch (attr) {
                case "minutes":
                    if (value.equals("Leaving")) estimate.setMinutes(0);
                    else estimate.setMinutes(Integer.parseInt(value));
                    break;
                case "platform":
                    estimate.setPlatform(Integer.parseInt(value));
                    break;
                case "direction":
                    estimate.setDirection(value);
                    break;
                case "length":
                    estimate.setLength(Integer.parseInt(value));
                    break;
                case "color":
                    estimate.setColor(value);
                    break;
                case "hexcolor":
                    estimate.setHexColor(value);
                    break;
                case "bikeflag":
                    estimate.setBikeFlag(Boolean.parseBoolean(value));
                    break;
            }
        }
        return estimate;
    }
}
