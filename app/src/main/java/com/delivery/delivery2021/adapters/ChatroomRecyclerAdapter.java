package com.delivery.delivery2021.adapters;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.delivery.delivery2021.R;
import com.delivery.delivery2021.UserClient;
import com.delivery.delivery2021.models.Chatroom;
import com.delivery.delivery2021.models.Distance;
import com.delivery.delivery2021.models.User_Location;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.annotation.Nullable;

import static com.delivery.delivery2021.util.Haversine.haversine;

public class ChatroomRecyclerAdapter extends RecyclerView.Adapter<ChatroomRecyclerAdapter.ViewHolder>{

    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private ArrayList<Distance> mDistances= new ArrayList<>();
    private ChatroomRecyclerClickListener mChatroomRecyclerClickListener;
    private FirebaseFirestore mDb;
    private static final String TAG = "ChatRoom adapter";
    private Context context;
    private User_Location mUser_location;

    public ChatroomRecyclerAdapter(ArrayList<Chatroom> chatrooms,
                                   ChatroomRecyclerClickListener chatroomRecyclerClickListener, Context context ) {
        this.mChatrooms = chatrooms;
        this.context = context;
        //this.mDistances= mDistances;
        mChatroomRecyclerClickListener = chatroomRecyclerClickListener;
        mDb = FirebaseFirestore.getInstance();

    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chatroom_list_item, parent, false);
        final ViewHolder holder = new ViewHolder(view, mChatroomRecyclerClickListener);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        double distance=0;
//        for(Distance distance1 : mDistances){
//            if(distance1.getUserID().equals(mChatrooms.get(position).getOwner_id())){
//                distance = distance1.getDistance();
//            }else{
//                distance = haversine(30.2326472,120.0396874,30.21975900,120.02770600);
//            }
//        }
        mUser_location =   ((UserClient) context).getUser_location();
        mDb.collection(context.getString(R.string.collection_user_locations)).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "onEvent: called.");

                if (e != null) {
                    Log.e(TAG, "onEvent: Listen failed.", e);
                    return;
                }

                if(queryDocumentSnapshots != null){
                    double distance = 0;

                    Log.d(TAG, "getDistances1: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                        User_Location tempLocation = document.toObject(User_Location.class);


                        Log.d(TAG, "getDistances: getting distance from " + mUser_location.getUser());
                        distance = haversine(mUser_location.getGeo_point().getLatitude(), mUser_location.getGeo_point().getLongitude(),
                                tempLocation.getGeo_point().getLatitude(), tempLocation.getGeo_point().getLongitude());
                        Distance tempDist = new Distance(tempLocation.getUser().getUser_id(), distance);
                        if(tempDist.getUserID().equals(mChatrooms.get(position).getOwner_id())){
                            distance = tempDist.getDistance();
                            Log.d(TAG, "getDistance from "+tempLocation.getUser().getUsername()+" "+distance+"km");
                            break;
                        }else {
                            distance = haversine(30.3326472, 120.0396874, 30.21975900, 120.02770600);
                           // Log.d(TAG, "getDistance from "+tempLocation.getUser().getUsername()+" "+distance+"km");
                        }
                    }
                    DecimalFormat numberFormat =  new DecimalFormat("0.00");
                    holder.distance.setText("Distance :"+ numberFormat.format(distance) + "km away");
                }else {
                    Log.d(TAG, "Error getting documents: "+ queryDocumentSnapshots.toString());
                }
                Log.d(TAG, "getDistance3: "+ mDistances.toString());

            }
        });

        //distance = haversine(30.2326472,120.0396874,30.21975900,120.02770600);
        ((ViewHolder)holder).serviceTitle.setText(mChatrooms.get(position).getTitle());
        if(mChatrooms.get(position).getPrice()>=0 && distance>=0) {
            holder.price.setText("Price: $" + mChatrooms.get(position).getPrice()+"/km");
        }
    }

    @Override
    public int getItemCount() {
        return mChatrooms.size();
    }

    private void getDistances(Chatroom mChatrooms)  {

        //final User_Location[] serviceLocation = {new User_Location()};



    }


//    private User_Location getServiceLocation(String UserID) {
//        final User_Location[] serviceLocation = new User_Location[1];
//        DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
//                .document("abCcbhU3g5Rmrj07oGmWT3ex51t1");
//        locationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
//                if(task.isSuccessful()){
//                    if(task.getResult().toObject(User_Location.class)!=null){
//                        serviceLocation[0] =task.getResult().toObject(User_Location.class);
//                    }
//                }
//            }
//        });
//        return serviceLocation[0];
//    }

    public class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener
    {
        TextView serviceTitle;
        TextView price;
        TextView distance;
        ChatroomRecyclerClickListener clickListener;

        public ViewHolder(View itemView, ChatroomRecyclerClickListener clickListener) {
            super(itemView);
            serviceTitle = itemView.findViewById(R.id.service_title);
            price = itemView.findViewById(R.id.servicePrice);
            distance = itemView.findViewById(R.id.distance);

            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onChatroomSelected(getAdapterPosition());
        }
    }

    public interface ChatroomRecyclerClickListener {
        public void onChatroomSelected(int position);
    }
}
















