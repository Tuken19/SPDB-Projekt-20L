package com.example.spdb;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;


public class OverpassQueryResultNew {
    @SerializedName("elements")
    public List<OverpassQueryResultNew.Element> elements = new ArrayList<>();

    public static class Element {
        @SerializedName("type")
        public String type;

        @SerializedName("id")
        public long id;

        @SerializedName("lat")
        public double lat;

        @SerializedName("lon")
        public double lon;

        @SerializedName("tags")
        public OverpassQueryResultNew.Element.Tags tags = new OverpassQueryResultNew.Element.Tags();

        @SerializedName("nodes")
        public List<Long> nodes;

        public static class Tags {
            @SerializedName("type")
            public String type;

            @SerializedName("amenity")
            public String amenity;

            @SerializedName("name")
            public String name;

            @SerializedName("phone")
            public String phone;

            @SerializedName("contact:email")
            public String contactEmail;

            @SerializedName("website")
            public String website;

            @SerializedName("addr:city")
            public String addressCity;

            @SerializedName("addr:postcode")
            public String addressPostCode;

            @SerializedName("addr:street")
            public String addressStreet;

            @SerializedName("addr:housenumber")
            public String addressHouseNumber;

            @SerializedName("wheelchair")
            public String wheelchair;

            @SerializedName("wheelchair:description")
            public String wheelchairDescription;

            @SerializedName("opening_hours")
            public String openingHours;

            @SerializedName("internet_access")
            public String internetAccess;

            @SerializedName("fee")
            public String fee;

            @SerializedName("operator")
            public String operator;

        }
    }
}
