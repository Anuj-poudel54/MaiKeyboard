package com.myapp.maikeyboard;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class GifAdapter extends RecyclerView.Adapter<GifAdapter.GifViewHolder> {

    private Context context;
    private List<String> gifUrls;
    private OnGifSelectedListener onGifSelectedListener;

    public GifAdapter(Context context, List<String> gifUrls, OnGifSelectedListener listener) {
        this.context = context;
        this.gifUrls = gifUrls;
        this.onGifSelectedListener = listener;
    }

    @NonNull
    @Override
    public GifViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gif, parent, false);
        return new GifViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GifViewHolder holder, int position) {
        String gifUrl = gifUrls.get(position);

        // Use Glide to load GIFs
        Glide.with(context)
                .asGif()
                .load(gifUrl)
                .into(holder.gifImageView);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onGifSelectedListener != null) {
                    onGifSelectedListener.onGifSelected(gifUrl);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return gifUrls.size();
    }

    public static class GifViewHolder extends RecyclerView.ViewHolder {
        public ImageView gifImageView;

        public GifViewHolder(View itemView) {
            super(itemView);
            gifImageView = itemView.findViewById(R.id.gifImageView);
        }
    }

    // Interface for GIF selection callback
    public interface OnGifSelectedListener {
        void onGifSelected(String gifUrl);
    }
}
