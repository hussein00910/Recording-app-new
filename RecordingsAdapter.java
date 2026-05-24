package com.callrecorder.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.callrecorder.app.R;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.utils.ContactUtils;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RecordingsAdapter extends ListAdapter<Recording, RecordingsAdapter.RecordingViewHolder> {

    private final RecordingItemListener listener;

    public RecordingsAdapter(RecordingItemListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recording, parent, false);
        return new RecordingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class RecordingViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameView;
        private final TextView dateTimeView;
        private final TextView durationView;
        private final TextView fileSizeView;
        private final ImageView callTypeIcon;
        private final ImageButton starBtn;
        private final ImageButton moreBtn;
        private final ImageView bookmarkIcon;
        private final Chip playCountChip;

        RecordingViewHolder(@NonNull View itemView) {
            super(itemView);
            nameView = itemView.findViewById(R.id.text_view_name);
            dateTimeView = itemView.findViewById(R.id.text_view_date_time);
            durationView = itemView.findViewById(R.id.text_view_duration);
            fileSizeView = itemView.findViewById(R.id.text_view_file_size);
            callTypeIcon = itemView.findViewById(R.id.image_view_call_type);
            starBtn = itemView.findViewById(R.id.button_star);
            moreBtn = itemView.findViewById(R.id.button_more);
            bookmarkIcon = itemView.findViewById(R.id.image_view_bookmark);
            playCountChip = itemView.findViewById(R.id.chip_play_count);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null)
                    listener.onItemClick(getItem(pos));
            });

            starBtn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null)
                    listener.onStarClick(getItem(pos));
            });

            moreBtn.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || listener == null) return;
                Recording r = getItem(pos);
                showPopupMenu(v, r);
            });
        }

        private void showPopupMenu(View anchor, Recording recording) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_recording_item);
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_share) {
                    listener.onShareClick(recording);
                    return true;
                } else if (id == R.id.action_bookmark) {
                    listener.onBookmarkClick(recording);
                    return true;
                } else if (id == R.id.action_delete) {
                    listener.onDeleteClick(recording);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        void bind(Recording r) {
            nameView.setText(r.getDisplayName());

            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy، hh:mm a", new Locale("ar"));
            dateTimeView.setText(fmt.format(new Date(r.getDate())));

            durationView.setText(r.getFormattedDuration());

            String size = r.getFormattedFileSize();
            if (size.isEmpty()) {
                fileSizeView.setVisibility(View.GONE);
            } else {
                fileSizeView.setVisibility(View.VISIBLE);
                fileSizeView.setText(size);
            }

            int callIcon = r.getCallType() == 1 ? R.drawable.ic_call_received : R.drawable.ic_call_made;
            callTypeIcon.setImageResource(callIcon);

            int starIcon = r.isStarred() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline;
            starBtn.setImageResource(starIcon);

            bookmarkIcon.setVisibility(r.isBookmarked() ? View.VISIBLE : View.GONE);

            if (r.getPlayCount() > 0) {
                playCountChip.setVisibility(View.VISIBLE);
                playCountChip.setText(String.valueOf(r.getPlayCount()));
            } else {
                playCountChip.setVisibility(View.GONE);
            }
        }
    }

    public interface RecordingItemListener {
        void onItemClick(Recording recording);
        void onStarClick(Recording recording);
        void onDeleteClick(Recording recording);
        void onShareClick(Recording recording);
        void onBookmarkClick(Recording recording);
    }

    private static final DiffUtil.ItemCallback<Recording> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Recording>() {
                @Override
                public boolean areItemsTheSame(@NonNull Recording o, @NonNull Recording n) {
                    return o.getId() == n.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Recording o, @NonNull Recording n) {
                    return o.isStarred() == n.isStarred()
                            && o.isBookmarked() == n.isBookmarked()
                            && o.getPlayCount() == n.getPlayCount()
                            && o.getDate() == n.getDate();
                }
            };
}
