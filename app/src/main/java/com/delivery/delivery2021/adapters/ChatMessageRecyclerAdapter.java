package com.delivery.delivery2021.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.delivery.delivery2021.R;
import com.delivery.delivery2021.UserClient;
import com.delivery.delivery2021.models.ChatMessage;
import com.delivery.delivery2021.models.Chatroom;
import com.delivery.delivery2021.models.User;
import com.delivery.delivery2021.ui.ChatroomActivity;
import com.delivery.delivery2021.ui.RegisterActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class ChatMessageRecyclerAdapter extends RecyclerView.Adapter<ChatMessageRecyclerAdapter.ViewHolder>{

    private ArrayList<ChatMessage> mMessages = new ArrayList<>();
    private ArrayList<User> mUsers = new ArrayList<>();
    private Context mContext;
    private Chatroom mChatroom;
    private ChatMessageRecyclerClicklistener mChatMessageRecyclerClicklistener;
    private static final String TAG = "ChatroomActivity";
    private FirebaseFirestore mDb = FirebaseFirestore.getInstance();

    public ChatMessageRecyclerAdapter(ArrayList<ChatMessage> messages,
                                      ArrayList<User> users,
                                      Context context, Chatroom chatroom,ChatMessageRecyclerClicklistener clickListener) {
        this.mMessages = messages;
        this.mUsers = users;
        this.mContext = context;
        this.mChatroom= chatroom;
        this.mChatMessageRecyclerClicklistener= clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_chat_message_list_item, parent, false);
        final ViewHolder holder = new ViewHolder(view,mChatMessageRecyclerClicklistener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        if(FirebaseAuth.getInstance().getUid().equals(mMessages.get(position).getUser().getUser_id())){
            ((ViewHolder)holder).username.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
        }
        else{
            ((ViewHolder)holder).username.setTextColor(ContextCompat.getColor(mContext, R.color.colorPrimary));
        }


        DocumentReference userRef= FirebaseFirestore.getInstance().collection("Users")
                .document(FirebaseAuth.getInstance().getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint("ResourceAsColor")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    User user = task.getResult().toObject(User.class);
                    if(!user.isService()&&!mMessages.get(position).getAccepted()){
                        holder.acceptButton.setText("Accept Pending");
                        holder.acceptButton.setEnabled(false);
                    }else if(!user.isService()&&mMessages.get(position).getAccepted()){
//                        holder.completed.setTextColor(R.color.red1);
//                        holder.collected.setTextColor(R.color.red1);
                    } else if(user.isService()){
                        holder.collected.setEnabled(true);
                        holder.completed.setEnabled(true);
                    }
                    Log.d(TAG, "UpdateOrder: view is about to be set ");
                    if(!(mMessages.get(position).getCompleted()==null)&&!(mMessages.get(position).getCollected()==null)) {
                        Log.d(TAG, "UpdateOrder: view has been set set ");
                        holder.completed.setChecked(mMessages.get(position).getCompleted());
                        holder.collected.setChecked(mMessages.get(position).getCollected());
                        if(mMessages.get(position).getCompleted()){  holder.checkboxes.setVisibility(View.GONE);
                            holder.acceptButton.setVisibility(View.VISIBLE);
                            holder.acceptButton.setText("Order Complete");
                            holder.acceptButton.setEnabled(false);}
                    }
                }
            }
        });

        if(mMessages.get(position).getAccepted()&&!mMessages.get(position).getCompleted()){
            ((ViewHolder)holder).acceptButton.setVisibility(View.GONE);
            ((ViewHolder)holder).checkboxes.setVisibility(View.VISIBLE);
        }


        ((ViewHolder)holder).username.setText(mMessages.get(position).getUser().getUsername());
        ((ViewHolder)holder).message.setText("Order description :"+mMessages.get(position).getMessage()+
                                            "\n Phone Number :"+mMessages.get(position).getPhone_number()+
                                            "\n Destination :"+mMessages.get(position).getDestination());
        ((ViewHolder)holder).acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Are you sure you want to accept this Order?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            mMessages.get(position).setAccepted(true);
                            UpdateOrderStatus(mMessages.get(position),v);
                            ((ViewHolder)holder).acceptButton.setVisibility(View.GONE);
                            ((ViewHolder)holder).checkboxes.setVisibility(View.VISIBLE);
                        })
                        .setNegativeButton("No", (dialog, id) -> mMessages.get(position).setAccepted(false));
                final AlertDialog alert = builder.create();
                alert.show();

            }
        });
        holder.completed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Has the order been completed?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            boolean checked = ((CheckBox) v).isChecked();
                            if (checked) {
                                mMessages.get(position).setCompleted(true);
                                UpdateOrderStatus(mMessages.get(position),v);
                            }
                        })
                        .setNegativeButton("No", (dialog, id) -> ((CheckBox) v).setChecked(false));
                final AlertDialog alert = builder.create();
                alert.show();
            }
        });

        holder.collected.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setMessage("Has the order been collected?")
                        .setCancelable(true)
                        .setPositiveButton("Yes", (dialog, id) -> {
                            boolean checked = ((CheckBox) v).isChecked();
                            if (checked) {
                                mMessages.get(position).setCollected(true);
                                UpdateOrderStatus(mMessages.get(position),v);
                            }
                        })
                        .setNegativeButton("No", (dialog, id) -> ((CheckBox) v).setChecked(false));
                final AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    private void UpdateOrderStatus(ChatMessage upDate, View v) {
        DocumentReference newMessageDoc = mDb
                .collection(mContext.getString(R.string.collection_chatrooms))
                .document(mChatroom.getChatroom_id())
                .collection(mContext.getString(R.string.collection_chat_messages))
                .document(upDate.getMessage_id());
        //Update order accepted Status
        newMessageDoc.update("accepted", upDate.getAccepted()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "UpdateOrder: accepted order has been successfully updated ");
                } else {
                    View parentLayout = v.findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        //Update order completed status
        newMessageDoc.update("completed", upDate.getCompleted()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "UpdateOrder: completed order has been successfully updated ");
                } else {
                    View parentLayout = v.findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        //Update order collected
        newMessageDoc.update("collected", upDate.getCollected()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "UpdateOrder: collected order has been successfully updated ");
                }else{
                    View parentLayout = v.findViewById(android.R.id.content);
                    Snackbar.make(parentLayout, "Something went wrong while updating order.", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }



    @Override
    public int getItemCount() {
        return mMessages.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        TextView message, username;
        Button acceptButton;
        View checkboxes;
        CheckBox collected, completed;
        ChatMessageRecyclerClicklistener clickListener;

        public ViewHolder(View itemView, ChatMessageRecyclerClicklistener clickListener) {
            super(itemView);
            message = itemView.findViewById(R.id.chat_message_message);
            username = itemView.findViewById(R.id.chat_message_username);
            acceptButton=itemView.findViewById(R.id.acceptOrder);
            checkboxes=itemView.findViewById(R.id.checks);
            collected=itemView.findViewById(R.id.collected);
            completed=itemView.findViewById(R.id.delivered);
            this.clickListener = clickListener;
            itemView.setOnClickListener(this);
        }

        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        @Override
        public void onClick(View v) {
            clickListener.onChatMessageSelected(getAdapterPosition());
        }
    }

    public interface ChatMessageRecyclerClicklistener {
        public void onChatMessageSelected(int position);
    }

}
















