package com.delivery.delivery2021.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import com.google.maps.GeoApiContext;

import java.util.Date;

public class ChatMessage implements Parcelable {

    private User user;
    private String OrderDescription;
    private String message_id;
    private @ServerTimestamp Date timestamp;
    private double destinationLat;
    private double destinationLng;
    private String source;
    private String destination;
    private String phone_number;
    private Boolean accepted;
    private Boolean completed;
    private Boolean collected;

    public ChatMessage(User user, String message, String message_id, Date timestamp) {
        this.user = user;
        this.OrderDescription = message;
        this.message_id = message_id;
        this.timestamp = timestamp;
    }

    public ChatMessage() {

    }

    protected ChatMessage(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
        OrderDescription = in.readString();
        message_id = in.readString();
        destinationLat = in.readDouble();
        destinationLng = in.readDouble();
//        double sLat=in.readDouble();
//        double sLon=in.readDouble();
//        source = new GeoPoint(sLat,sLon);
        phone_number = in.readString();
        byte tmpAccepted = in.readByte();
        accepted = tmpAccepted == 0 ? null : tmpAccepted == 1;
        byte tmpCompleted = in.readByte();
        completed = tmpCompleted == 0 ? null : tmpCompleted == 1;
        byte tmpCollected = in.readByte();
        collected = tmpCollected == 0 ? null : tmpCollected == 1;
    }

    public static final Creator<ChatMessage> CREATOR = new Creator<ChatMessage>() {
        @Override
        public ChatMessage createFromParcel(Parcel in) {
            return new ChatMessage(in);
        }

        @Override
        public ChatMessage[] newArray(int size) {
            return new ChatMessage[size];
        }
    };

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() { return OrderDescription; }

    public void setMessage(String message) {
        this.OrderDescription = message;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(double destinationLat) {
        this.destinationLat = destinationLat;
    }

//    public GeoPoint getSource() {
//        return source;
//    }
//
    public void setSource(String source) {
        this.source = source;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public Boolean getAccepted() {
        return accepted;
    }

    public void setAccepted(Boolean accepted) {
        this.accepted = accepted;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public double getDestinationLng() {
        return destinationLng;
    }

    public void setDestinationLng(double destinationLng) {
        this.destinationLng = destinationLng;
    }

    public Boolean getCollected() {
        return collected;
    }

    public void setCollected(Boolean collected) {
        this.collected = collected;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "user=" + user +
                ", message='" + OrderDescription + '\'' +
                ", message_id='" + message_id + '\'' +
                ", timestamp=" + timestamp + '\'' +
                "destination latitude"+destinationLat+ '\'' +
                "destination longitude"+destinationLng+ '\'' +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(user, flags);
        dest.writeString(OrderDescription);
        dest.writeString(message_id);
        dest.writeDouble(destinationLat);
        dest.writeDouble(destinationLng);
//        dest.writeDouble(source.getLatitude());
//        dest.writeDouble(source.getLongitude());
        dest.writeString(phone_number);
        dest.writeByte((byte) (accepted == null ? 0 : accepted ? 1 : 2));
        dest.writeByte((byte) (completed == null ? 0 : completed ? 1 : 2));
        dest.writeByte((byte) (collected == null ? 0 : collected ? 1 : 2));
    }
}
