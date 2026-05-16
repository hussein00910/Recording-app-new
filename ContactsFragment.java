package com.callrecorder.app.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.callrecorder.app.R;
import com.callrecorder.app.models.Recording;
import com.callrecorder.app.viewmodels.RecordingsViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContactsFragment extends Fragment {

    private RecordingsViewModel viewModel;
    private ContactGroupAdapter adapter;
    private EditText searchView;
    private View emptyView;
    private List<ContactGroup> allGroups = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(RecordingsViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_contacts);
        searchView = view.findViewById(R.id.search_contacts);
        emptyView = view.findViewById(R.id.empty_contacts_view);

        adapter = new ContactGroupAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getAllRecordings().observe(getViewLifecycleOwner(), recordings -> {
            allGroups = groupByContact(recordings);
            adapter.submitGroups(allGroups);
            emptyView.setVisibility(allGroups.isEmpty() ? View.VISIBLE : View.GONE);
        });

        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterGroups(s.toString());
            }
        });
    }

    private List<ContactGroup> groupByContact(List<Recording> recordings) {
        Map<String, ContactGroup> map = new LinkedHashMap<>();
        if (recordings == null) return new ArrayList<>();

        for (Recording r : recordings) {
            String key = r.getDisplayName();
            if (!map.containsKey(key)) {
                map.put(key, new ContactGroup(key, r.getPhoneNumber()));
            }
            map.get(key).recordings.add(r);
        }

        return new ArrayList<>(map.values());
    }

    private void filterGroups(String query) {
        if (query.isEmpty()) {
            adapter.submitGroups(allGroups);
            return;
        }
        List<ContactGroup> filtered = new ArrayList<>();
        for (ContactGroup g : allGroups) {
            if (g.name.contains(query) || (g.phone != null && g.phone.contains(query))) {
                filtered.add(g);
            }
        }
        adapter.submitGroups(filtered);
    }

    // ── Data model ─────────────────────────────────────────────────────────────

    static class ContactGroup {
        String name;
        String phone;
        List<Recording> recordings = new ArrayList<>();

        ContactGroup(String name, String phone) {
            this.name = name;
            this.phone = phone;
        }

        String getSummary() {
            int count = recordings.size();
            int incoming = 0, outgoing = 0;
            for (Recording r : recordings) {
                if (r.getCallType() == 1) incoming++;
                else outgoing++;
            }
            return count + " تسجيل • " + incoming + " واردة، " + outgoing + " صادرة";
        }

        String getLastCallDate() {
            if (recordings.isEmpty()) return "";
            SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy", new Locale("ar"));
            return "آخر مكالمة: " + fmt.format(new Date(recordings.get(0).getDate()));
        }
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    static class ContactGroupAdapter extends RecyclerView.Adapter<ContactGroupAdapter.VH> {
        private List<ContactGroup> groups = new ArrayList<>();

        void submitGroups(List<ContactGroup> g) {
            groups = g != null ? g : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_contact_group, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.bind(groups.get(pos));
        }

        @Override
        public int getItemCount() { return groups.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvSummary, tvDate, tvAvatar;
            ImageView btnCall;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_contact_name);
                tvSummary = v.findViewById(R.id.tv_contact_summary);
                tvDate = v.findViewById(R.id.tv_last_call_date);
                tvAvatar = v.findViewById(R.id.tv_avatar_letter);
                btnCall = v.findViewById(R.id.btn_call_contact);
            }

            void bind(ContactGroup g) {
                tvName.setText(g.name);
                tvSummary.setText(g.getSummary());
                tvDate.setText(g.getLastCallDate());

                String letter = (g.name != null && !g.name.isEmpty())
                        ? String.valueOf(g.name.charAt(0)).toUpperCase() : "?";
                tvAvatar.setText(letter);

                if (g.phone != null && btnCall != null) {
                    btnCall.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL,
                                Uri.parse("tel:" + g.phone));
                        v.getContext().startActivity(intent);
                    });
                }
            }
        }
    }
}
