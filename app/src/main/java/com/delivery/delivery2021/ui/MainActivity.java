package com.delivery.delivery2021.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.delivery.delivery2021.UserClient;
import com.delivery.delivery2021.models.Distance;
import com.delivery.delivery2021.models.User;
import com.delivery.delivery2021.models.User_Location;
import com.delivery.delivery2021.sevices.LocationService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.delivery.delivery2021.R;
import com.delivery.delivery2021.adapters.ChatroomRecyclerAdapter;
import com.delivery.delivery2021.models.Chatroom;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import static com.delivery.delivery2021.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.delivery.delivery2021.Constants.ERROR_DIALOG_REQUEST;
import static com.delivery.delivery2021.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;
import static com.delivery.delivery2021.util.Haversine.haversine;


public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        ChatroomRecyclerAdapter.ChatroomRecyclerClickListener {

    private static final String TAG = "MainActivity";


    //widgets
    private ProgressBar mProgressBar;
    private FloatingActionButton fab;
    private TextView welcome;

    //vars
    private ArrayList<Chatroom> mChatrooms = new ArrayList<>();
    private Set<String> mChatroomIds = new HashSet<>();
    private ChatroomRecyclerAdapter mChatroomRecyclerAdapter;
    private RecyclerView mChatroomRecyclerView;
    private ListenerRegistration mChatroomEventListener;
    private FirebaseFirestore mDb;
    private ArrayList<Distance> mDistances = new ArrayList<>();
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private User_Location mUser_location;
    private User currentUser;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private ArrayList<User> mUserList = new ArrayList<>();
    private ArrayList<User_Location> mUser_locations= new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressBar = findViewById(R.id.progressBar);
        mChatroomRecyclerView = findViewById(R.id.chatrooms_recycler_view);
        welcome=findViewById(R.id.welcome);

        fab = findViewById(R.id.fab_create_chatroom);
        fab.setOnClickListener(this);
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mDb = FirebaseFirestore.getInstance();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        initSupportActionBar();
        getServiceLocation();
    }



    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationService.class);
//          this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                MainActivity.this.startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.delivery.googledirectionstest.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;

    }
    private void getUserDetails(){
            if (mUser_location == null) {
                mUser_location = new User_Location();
                Log.d(TAG, "getUserDetails");
                DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                        .document(FirebaseAuth.getInstance().getUid());
                userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: successfully get the user details.");
                            User user = task.getResult().toObject(User.class);
                            if (user.isService()) {
                                fab.setVisibility(View.VISIBLE);
                            }
                            currentUser = user;
                            startCourierRunnable();
                            welcome.setText("Welcome back "+user.getUsername()+" time to make a decision");
                            mUser_location.setUser(user);
                            ((UserClient) getApplicationContext()).setUser(user);
                            getLastKnownLocation();
                            startLocationService();
                            mUser_locations.add(mUser_location);
                            mUserList.add(mUser_location.getUser());

                        }
                    }
                });

            } else {
                getLastKnownLocation();

            }
    }

    private void saveUserLocation(){
        if(mUser_location!=null){
            DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
                    .document(FirebaseAuth.getInstance().getUid());
            locationRef.set(mUser_location).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG,"saveUserLocation: \ninserted user location into database."+
                                "\n latitude:" +mUser_location.getGeo_point().getLatitude()+"\n longitude: "
                                + mUser_location.getGeo_point().getLongitude());
                    }
                }
            });
        }
    }


    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    {
                       GeoPoint getPoint =new GeoPoint(location.getLatitude(),location.getLongitude());
                        Log.d(TAG, "onComplete latitude: "+ getPoint.getLatitude());
                        Log.d(TAG, "onComplete longitude: "+ getPoint.getLongitude());

                        mUser_location.setGeo_point(getPoint);
                        mUser_location.setTimestamp(null);
                        saveUserLocation();
                        ((UserClient) getApplicationContext()).setUser_location(mUser_location);
                        initChatroomRecyclerView();
                        mChatroomRecyclerAdapter.notifyDataSetChanged();
                    }
                }
                else {
                   // Log.w(TAG, "getLastLocation:exception", task.getException());
                }
            }
        });

    }


    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );
        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        Log.d(TAG, "isServicesOK: checking Maps services is working");
        return true;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            getUserDetails();
            getChatrooms();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }



    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");

        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(available == ConnectionResult.SUCCESS){
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionGranted){
                    getUserDetails();
                    getChatrooms();
                }
                else{
                    getLocationPermission();
                }
            }
        }

    }
    private void hideSoftKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    private void  inflateUserListFragment(){
        hideSoftKeyboard();


        Set<User> set = new HashSet<>(mUserList);
        mUserList.clear();
        mUserList.addAll(set);

        Set<User_Location> set1 = new HashSet<>(mUser_locations);
        mUser_locations.clear();
        mUser_locations.addAll(set1);

        UserListFragment fragment = UserListFragment.newInstance();
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList(getString(R.string.intent_user_list), mUserList);
        bundle.putParcelableArrayList(getString(R.string.intent_user_locations), mUser_locations);

        fragment.setArguments(bundle);

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.user_list_container, fragment, getString(R.string.fragment_user_list));
        transaction.addToBackStack(getString(R.string.fragment_user_list));
        transaction.commit();
    }


    public void getServiceLocation(){
        mDb.collection(getString(R.string.collection_user_locations)).addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                        try {
                            if (tempLocation.getUser().isService()) {
                                Log.d(TAG, "getServiceLocation " + (mUserList.contains(tempLocation.getUser())));
                                if (!mUserList.contains(tempLocation.getUser())) {
                                    mUser_locations.add(tempLocation);
                                    mUserList.add(tempLocation.getUser());
                                }
                            }
                        }catch (NullPointerException e1){
                            Toast.makeText(MainActivity.this, "Couldn't load couriers, Check your internet connection", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "getCouriers()", e1);
                        }
                    }
                }else {
                    Log.d(TAG, "Error getting documents: "+ queryDocumentSnapshots.toString());
                }
            }
        });

    }

    public User_Location getUser_location() {
        return mUser_location;
    }

    private void initSupportActionBar(){
        setTitle("Zeppli couriers");
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){

            case R.id.fab_create_chatroom:{
                newChatroomDialog();
            }
        }
    }

    private void initChatroomRecyclerView(){
            mChatroomRecyclerAdapter = new ChatroomRecyclerAdapter(mChatrooms, this,getApplicationContext());
            mChatroomRecyclerView.setAdapter(mChatroomRecyclerAdapter);
            mChatroomRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }


    private void startCourierRunnable(){
        Log.d(TAG, "startCourierRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                getChatrooms();
                for(Chatroom chatroom: mChatrooms){
                    if(chatroom.getOwner_id().equals(currentUser.getUser_id()))
                    {
                        Intent intent = new Intent(MainActivity.this, ChatroomActivity.class);
                        intent.putExtra(getString(R.string.intent_chatroom), chatroom);
                        startActivity(intent);
                        finish();
                    }
                }
                Log.d(TAG, "startCourierRunnable: running");
                mHandler.postDelayed(mRunnable, 1000);
            }
        }, 1000);
    }
    private void stopCourierUpdates(){
        mHandler.removeCallbacks(mRunnable);
    }

    private void getChatrooms(){

        CollectionReference chatroomsCollection = mDb
                .collection(getString(R.string.collection_chatrooms));

        mChatroomEventListener = chatroomsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                Log.d(TAG, "getServices onEvent: called");

                if (e != null) {
                    Log.e(TAG, "getServices onEvent: Listen failed.", e);
                    return;
                }
                Log.d(TAG, "getServices onEvent: Getting all the service provider data: " + mChatrooms.size());
                if(queryDocumentSnapshots != null){
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        Chatroom chatroom = doc.toObject(Chatroom.class);
                        if(!mChatroomIds.contains(chatroom.getChatroom_id())){
                            mChatroomIds.add(chatroom.getChatroom_id());
                            mChatrooms.add(chatroom);
                        }
                    }
                    Log.d(TAG, "getServices onEvent: Number services received: " + mChatrooms.size());

                }

            }
        });
    }

    private void buildNewChatroom(String chatroomName, double price){

        final Chatroom chatroom = new Chatroom();
        chatroom.setTitle(chatroomName);
        chatroom.setPrice(price);
//        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
//                .setTimestampsInSnapshotsEnabled(true)
//                .build();
//        mDb.setFirestoreSettings(settings);

        DocumentReference newChatroomRef = mDb
                .collection(getString(R.string.collection_chatrooms))
                .document();

        chatroom.setChatroom_id(newChatroomRef.getId());
        chatroom.setOwner_id(currentUser.getUser_id());

        newChatroomRef.set(chatroom).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                hideDialog();

                if(task.isSuccessful()){
                    navChatroomActivity(chatroom);
                }else{
                    View parentLayout = findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navChatroomActivity(Chatroom chatroom){
        Intent intent = new Intent(MainActivity.this, ChatroomActivity.class);
        intent.putExtra(getString(R.string.intent_chatroom), chatroom);
        startActivity(intent);
    }

    private void newChatroomDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Company details");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText name = new EditText(this);
        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("Name");
        layout.addView(name);

        final EditText price = new EditText(this);
       price.setInputType(InputType.TYPE_CLASS_NUMBER);
        price.setHint("Price Per Kilometer");
        layout.addView(price);
        builder.setView(layout);
        builder.setPositiveButton("CREATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!name.getText().toString().equals("")&&!price.getText().toString().equals("")){
                    buildNewChatroom(name.getText().toString(),Double.parseDouble(price.getText().toString()));
                }
                else {
                    Toast.makeText(MainActivity.this, "Fill in all the fields", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
//    Thread trd= new Thread(new Runnable() {
//        @Override
//        public void run() {
//                Log.d(TAG, "turd started ");
//                getUserDetails();
//            }
//        });


//    private User_Location getServiceLocation(String UserID) {
//            final User_Location[] serviceLocation = {new User_Location()};
//        DocumentReference locationRef = mDb.collection(getString(R.string.collection_user_locations))
//                .document(UserID);
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




    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mChatroomEventListener != null){
            mChatroomEventListener.remove();
        }
        stopCourierUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(checkMapServices()){
            if(mLocationPermissionGranted){
                getUserDetails();
                getChatrooms();
                startCourierRunnable();
            }
            else{
                getLocationPermission();
            }
        }

    }

    @Override
    public void onChatroomSelected(int position) {
        navChatroomActivity(mChatrooms.get(position));
    }

    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_sign_out:{
                signOut();
                return true;
            }
            case R.id.action_profile:{
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            case android.R.id.home:{
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
                return true;
            }
            default:{
                return super.onOptionsItemSelected(item);
            }
        }

    }

    private void showDialog(){
        mProgressBar.setVisibility(View.VISIBLE);
    }

    private void hideDialog(){
        mProgressBar.setVisibility(View.GONE);
    }


}
