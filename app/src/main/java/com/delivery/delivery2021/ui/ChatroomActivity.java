package com.delivery.delivery2021.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.delivery.delivery2021.models.User_Location;


import com.google.android.gms.common.api.Status;


import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.os.Handler;
import android.os.Parcelable;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.delivery.delivery2021.R;
import com.delivery.delivery2021.UserClient;
import com.delivery.delivery2021.adapters.ChatMessageRecyclerAdapter;
import com.delivery.delivery2021.models.ChatMessage;
import com.delivery.delivery2021.models.Chatroom;
import com.delivery.delivery2021.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.GeoApiContext;
import com.google.maps.PlacesApi;
import com.google.maps.TextSearchRequest;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceDetails;
import com.google.maps.model.PlacesSearchResponse;
import com.google.maps.model.PlacesSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class ChatroomActivity extends AppCompatActivity implements
        View.OnClickListener, ChatMessageRecyclerAdapter.ChatMessageRecyclerClicklistener
{

    private static final String TAG = "ChatroomActivity";
    private static int AUTOCOMPLETE_REQUEST_CODE = 1;

    //widgets
    //private EditText mMessage;
    private FloatingActionButton addOrder;
    private EditText mSearchText;
    private RelativeLayout searchBar;

    //vars
    private ListenerRegistration mChatMessageEventListener, mUserListEventListener;
    private RecyclerView mChatMessageRecyclerView;
    private ChatMessageRecyclerAdapter mChatMessageRecyclerAdapter;
    private FirebaseFirestore mDb;
    private ArrayList<ChatMessage> mMessages = new ArrayList<>();
    private Set<String> mMessageIds = new HashSet<>();
    private ArrayList<User> mUserList = new ArrayList<>();
    private UserListFragment mUserListFragment;
    private ArrayList<User_Location> mUser_locations= new ArrayList<>();
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private User currentUser;
    private LatLng mLatLng;
    private String searchBarText;
    private String mDestination;
    private String mSource;
    private Chatroom mChatroom;




    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatroom);
       // mMessage = findViewById(R.id.input_message);
        mChatMessageRecyclerView = findViewById(R.id.chatmessage_recycler_view);
        addOrder = findViewById(R.id.addMessage);
        addOrder.setVisibility(View.VISIBLE);
//        findViewById(R.id.checkmark).setOnClickListener(this);
        currentUser = ((UserClient)(getApplicationContext())).getUser();
        try {
            if (currentUser.isService()) {
                addOrder.setVisibility(View.GONE);
            }
        }catch (NullPointerException e){ }

        mDb = FirebaseFirestore.getInstance();
        //  Initialize Places.
        Places.initialize(getApplicationContext(), String.valueOf(R.string.google_api_key));

        //Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);
        getIncomingIntent();
        initChatroomRecyclerView();
        getChatroomUsers();
    }

    private void getChatMessages() {
            DocumentReference userRef = FirebaseFirestore.getInstance().collection("Users")
                    .document(FirebaseAuth.getInstance().getUid());
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        currentUser = task.getResult().toObject(User.class);
                    }
                    CollectionReference messagesRef = mDb
                            .collection(getString(R.string.collection_chatrooms))
                            .document(mChatroom.getChatroom_id())
                            .collection(getString(R.string.collection_chat_messages));

                    mChatMessageEventListener = messagesRef
                            .orderBy("timestamp", Query.Direction.ASCENDING)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.e(TAG, "onEvent: Listen failed.", e);
                                        return;
                                    }
                                    if (queryDocumentSnapshots != null) {
                                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                            // String cUser=FirebaseAuth.getInstance().getCurrentUser().getUid();
                                            //Boolean userIsService= mDb.child();

                                            ChatMessage message = doc.toObject(ChatMessage.class);
                                            if (!currentUser.isService()) {
                                                if (message.getUser().getUser_id().equals(currentUser.getUser_id()) && !mMessageIds.contains(message.getMessage_id())) {
                                                    mMessageIds.add(message.getMessage_id());
                                                    mMessages.add(message);
                                                    mChatMessageRecyclerView.smoothScrollToPosition(mMessages.size() - 1);
                                                }
                                            } else {
                                                if (!mMessageIds.contains(message.getMessage_id())) {
                                                    mMessageIds.add(message.getMessage_id());
                                                    mMessages.add(message);
                                                    mChatMessageRecyclerView.smoothScrollToPosition(mMessages.size() - 1);
                                                }
                                            }

                                        }
                                        Collections.sort(mMessages, new Comparator<ChatMessage>() {
                                            @Override
                                            public int compare(ChatMessage o1, ChatMessage o2) {

                                                int temp = o1.getCompleted().compareTo(o2.getCompleted());
                                                return temp;
                                            }
                                        });
                                        mChatMessageRecyclerAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                }
            });
        }


//    private void init(){
//        Log.d(TAG,"Init has been called");
//        geoLocate();
////        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
////            @Override
////            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
////                if(actionId== EditorInfo.IME_ACTION_SEARCH
////                        ||actionId==EditorInfo.IME_ACTION_DONE
////                        ||event.getAction()==KeyEvent.ACTION_DOWN
////                        ||event.getAction()==KeyEvent.KEYCODE_ENTER){
////                    geoLocate();
////                }
////                return false;
////            }
////        });
//    }


    Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                GeoApiContext context = new GeoApiContext.Builder()
                        .apiKey(getString(R.string.google_api_key))
                        .build();
                Log.d(TAG, "geoLocate : GeoContext "+context);
                TextSearchRequest req = PlacesApi.textSearchQuery(context, searchBarText);
                //TextSearchRequest req1 = PlacesApi.
                synchronized (thread){
                    try {
                        PlacesSearchResponse resp = req.await();
                        if (resp.results != null && resp.results.length > 0) {
                            Log.d(TAG, "geoLocate :result list size is" + resp.results.length);
                            for (PlacesSearchResult r : resp.results) {
                                PlaceDetails details = PlacesApi.placeDetails(context, r.placeId).await();
                                double lat = details.geometry.location.lat;
                                double lng = details.geometry.location.lng;
                                mLatLng = new LatLng(lat, lng);
                                String name = details.name;
                                String address = details.formattedAddress;
                                mDestination= name + " " + address;
                                com.google.maps.model.LatLng latLng = new com.google.maps.model.LatLng(lat, lng);
                                Log.d(TAG, "geoLocate : Returned result for place " + name + " " + address + " " + latLng.toString());
                            }
                        } else {
                            Toast.makeText(ChatroomActivity.this, "Error getting places ", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "geoLocate : Error getting places");
                            Exception error=new Exception();
                            throw error;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting places", e);
                    }
                    thread.notify();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    });

    private void getChatroomUsers(){

        CollectionReference usersRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chatroom_user_list));

        mUserListEventListener = usersRef
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@javax.annotation.Nullable QuerySnapshot queryDocumentSnapshots, @javax.annotation.Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.e(TAG, "onEvent: Listen failed.", e);
                            return;
                        }

                        if(queryDocumentSnapshots != null){

                            // Clear the list and add all the users again
                            mUserList.clear();
                            mUserList = new ArrayList<>();

                            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                User user = doc.toObject(User.class);
                                mUserList.add(user);
                                getUserLocation(user);
                            }

                            Log.d(TAG, "onEvent: user list size: " + mUserList.size());
                        }
                    }
                });
    }

    private void getUserLocation(User user) {
        DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
                .document(user.getUser_id());
        locationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    if(task.getResult().toObject(User_Location.class)!=null){
                        mUser_locations.add(task.getResult().toObject(User_Location.class));
                    }
                }
            }
        });
    }

    private void initChatroomRecyclerView(){

        mChatMessageRecyclerAdapter = new ChatMessageRecyclerAdapter(mMessages, new ArrayList<User>(), this,mChatroom,this::onChatMessageSelected);
        mChatMessageRecyclerView.setAdapter(mChatMessageRecyclerAdapter);
        mChatMessageRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mChatMessageRecyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (bottom < oldBottom) {
                    mChatMessageRecyclerView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(mMessages.size() > 0){
                                mChatMessageRecyclerView.smoothScrollToPosition(
                                        mChatMessageRecyclerView.getAdapter().getItemCount() - 1);
                            }
                        }
                    }, 100);
                }
            }
        });

    }


    private void insertNewMessage(ChatMessage newOrder){
        String message = newOrder.getMessage();
        newOrder.setAccepted(false);
        newOrder.setCompleted(false);
        newOrder.setCollected(false);
        if(!message.equals("")){
            message = message.replaceAll(System.getProperty("line.separator"), "");

            DocumentReference newMessageDoc = mDb
                    .collection(getString(R.string.collection_chatrooms))
                    .document(mChatroom.getChatroom_id())
                    .collection(getString(R.string.collection_chat_messages))
                    .document();

//            ChatMessage newChatMessage = new ChatMessage();
//            newChatMessage.setMessage(message);
            newOrder.setMessage_id(newMessageDoc.getId());

            User user = ((UserClient)(getApplicationContext())).getUser();
            Log.d(TAG, "insertNewMessage: retrieved user client: " + user.toString());
            newOrder.setUser(user);

            newMessageDoc.set(newOrder).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        //clearMessage();
                        Log.d(TAG, "insertNewMessage: new Order has been made ");
                    }else{
                        View parentLayout = findViewById(android.R.id.content);
                        Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }




//    public void UpdateOrderStatus(ChatMessage upDate){
//        DocumentReference newMessageDoc = mDb
//                .collection("Chatrooms")
//                .document(mChatroom.getChatroom_id())
//                .collection("Chat Messages")
//                .document(upDate.getMessage_id());
//        //Update order accepted Status
//        newMessageDoc.update("accepted", upDate.getAccepted()).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if(task.isSuccessful()){
//                    Log.d(TAG, "UpdateOrder: accepted order has been successfully updated ");
//                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
//                }
//            }
//        });
//        //Update order completed status
//        newMessageDoc.update("completed", upDate.getCompleted()).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if(task.isSuccessful()){
//                    Log.d(TAG, "UpdateOrder: completed order has been successfully updated ");
//                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
//                }
//            }
//        });
//        //Update order collected
//        newMessageDoc.update("collected", upDate.getCollected()).addOnCompleteListener(new OnCompleteListener<Void>() {
//            @Override
//            public void onComplete(@NonNull Task<Void> task) {
//                if(task.isSuccessful()){
//                    Log.d(TAG, "UpdateOrder: collected order has been successfully updated ");
//                }else{
//                    View parentLayout = findViewById(android.R.id.content);
//                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }


    private void  inflateUserListFragment(){
        hideSoftKeyboard();

        ///////////////////////////test code/////////////////////////////////
        ArrayList<User_Location> tempUser_locations = new ArrayList<>();
        ArrayList<User> tempUserList = new ArrayList<>();
        if(!currentUser.isService()) {
            for (User_Location user_location : mUser_locations) {
                if (user_location.getUser().getUser_id().equals(currentUser.getUser_id()) ||
                        user_location.getUser().getUser_id().equals(mChatroom.getOwner_id())) {
                    tempUser_locations.add(user_location);
                }
            }
            for (User user : mUserList) {
                if (user.getUser_id().equals(currentUser.getUser_id()) ||
                        user.getUser_id().equals(mChatroom.getOwner_id())) {
                    tempUserList.add(user);
                }
            }
        }else{
            tempUser_locations=mUser_locations;
            tempUserList=mUserList;
        }
        /////////test code///////////////////////////////////////
        UserListFragment fragment = UserListFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(getString(R.string.intent_user_list), tempUserList);
        bundle.putParcelableArrayList(getString(R.string.intent_user_locations), tempUser_locations);
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.user_list_container, fragment, getString(R.string.fragment_user_list));
        transaction.addToBackStack(getString(R.string.fragment_user_list));
        transaction.commit();
    }

    private void inflateUserListFragment(ChatMessage chatMessage,Chatroom chatroom){
        hideSoftKeyboard();
        ArrayList<User_Location> tempUser_locations = new ArrayList<>();
        ArrayList<User> tempUserList = new ArrayList<>();
        for(User_Location user_location : mUser_locations){
            if(user_location.getUser().getUser_id().equals(chatMessage.getUser().getUser_id())||
                    user_location.getUser().getUser_id().equals(chatroom.getOwner_id())){
                tempUser_locations.add(user_location);
            }
        }
        for(User user : mUserList){
            if(user.getUser_id().equals(chatMessage.getUser().getUser_id())||
                    user.getUser_id().equals(chatroom.getOwner_id())){
                tempUserList.add(user);
            }
        }

        UserListFragment fragment = UserListFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(getString(R.string.intent_user_list), tempUserList);
        bundle.putParcelableArrayList(getString(R.string.intent_user_locations), tempUser_locations);
        bundle.putParcelable("chatmessage",chatMessage);// we're gonna pass the whole chat message so that we can add destination to the map
        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.user_list_container, fragment, getString(R.string.fragment_user_list));
        transaction.addToBackStack(getString(R.string.fragment_user_list));
        transaction.commit();
    }

    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    private void getIncomingIntent(){
        if(getIntent().hasExtra(getString(R.string.intent_chatroom))){
            mChatroom = getIntent().getParcelableExtra(getString(R.string.intent_chatroom));
            try {
                setChatroomName();
                joinChatroom();
            }catch (NullPointerException e){}
        }
    }

    private void leaveChatroom(){

        DocumentReference joinChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chatroom_user_list))
                .document(FirebaseAuth.getInstance().getUid());

        joinChatroomRef.delete();
    }

    private void joinChatroom() throws NullPointerException{

        DocumentReference joinChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(getString(R.string.collection_chatroom_user_list))
                .document(FirebaseAuth.getInstance().getUid());

        User user = ((UserClient)(getApplicationContext())).getUser();
        joinChatroomRef.set(user); // Don't care about listening for completion.
    }

    private void setChatroomName(){
        getSupportActionBar().setTitle(mChatroom.getTitle());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private void startOrdersRunnable(){
        Log.d(TAG, "startOrdersRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                getChatMessages();
                Log.d(TAG, "startorderRunnable: running");
                mHandler.postDelayed(mRunnable, 3000);
            }
        }, 3000);
    }
    private void stopOrderUpdates(){
        mHandler.removeCallbacks(mRunnable);
    }

    private void newOrderDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter order Details");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText name = new EditText(this);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("Name");
        layout.addView(name);

        final EditText phoneNumber = new EditText(this);
        phoneNumber.setInputType(InputType.TYPE_CLASS_PHONE);
        phoneNumber.setHint("Phone Number");
        layout.addView(phoneNumber);


        final EditText receivingAddress = new EditText(this);
        receivingAddress.setInputType(InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS);
        receivingAddress.setHint("Detailed receiving Address");
        layout.addView(receivingAddress);


        final EditText description = new EditText(this);
        description.setInputType(InputType.TYPE_CLASS_TEXT);
        description.setHint("Goods description");
        layout.addView(description);
        builder.setView(layout);


        final ChatMessage order1 = new ChatMessage();


        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!description.getText().toString().equals("")){
                    //buildNewChatroom(input.getText().toString());
                    order1.setMessage(description.getText().toString());
                    //order1.setOrder_id(newMessageDoc.getId());
                    order1.setPhone_number(phoneNumber.getText().toString());

                    searchBarText = receivingAddress.getText().toString();
                    for(User_Location user_location : mUser_locations){
                        if(user_location.getUser().getUser_id().equals(currentUser.getUser_id())){
                            order1.setSource(mUser_locations
                                .get(mUser_locations.indexOf(user_location))
                                    .getGeo_point().toString());
                        }
                    }
                    thread.start();
                    synchronized (thread) {
                        try {
                            thread.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Thread is done coordinates are :"+mLatLng);
                        order1.setDestinationLat(mLatLng.lat);
                        order1.setDestinationLng(mLatLng.lng);
                        order1.setDestination(mDestination);
                        insertNewMessage(order1);
                    }

                }
                else {
                    Toast.makeText(ChatroomActivity.this, "Enter a chatroom name", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel Order", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getChatMessages();
        startOrdersRunnable();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mChatMessageEventListener != null){
            mChatMessageEventListener.remove();
        }
        if(mUserListEventListener != null){
            mUserListEventListener.remove();
        }
        stopOrderUpdates();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatroom_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onChatMessageSelected(int position){
        addOrder.setVisibility(View.GONE);
        inflateUserListFragment(mMessages.get(position),mChatroom);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:{
                addOrder.setVisibility(View.VISIBLE);
                UserListFragment fragment =
                        (UserListFragment) getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_user_list));
                if(fragment != null){
                    if(fragment.isVisible()){
                        getSupportFragmentManager().popBackStack();
                        return true;
                    }
                }
                finish();
                return true;
            }
            case R.id.action_chatroom_user_list:{
                inflateUserListFragment();
                addOrder.setVisibility(View.GONE);
                return true;
            }
            case R.id.action_chatroom_leave:{
                leaveChatroom();
                return true;
            }
            case R.id.action_sign_out:{
                signOut();
                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.addMessage:{
                //Intent intent = new Intent(v.getContext(), addOrder.class);
                // startActivityForResult(intent,ADD_ORDER_ACTIVITY);
                newOrderDialog();
            }
        }
    }

}
