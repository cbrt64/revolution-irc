package io.mrarm.irc;

import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import io.mrarm.irc.util.EntryRecyclerViewAdapter;
import io.mrarm.irc.util.SimpleCounter;

public class SettingsListAdapter extends EntryRecyclerViewAdapter {

    private Activity mActivity;
    private SimpleCounter mRequestCodeCounter;

    public SettingsListAdapter(Activity activity) {
        mActivity = activity;
    }

    public void setRequestCodeCounter(SimpleCounter counter) {
        mRequestCodeCounter = counter;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        for (Entry e : mEntries) {
            if (e instanceof ActivityResultCallback)
                ((ActivityResultCallback) e).onActivityResult(requestCode, resultCode, data);
        }
    }

    private interface ActivityResultCallback {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    public static class HeaderEntry extends Entry {

        private static final int sHolder = registerViewHolder(HeaderEntryHolder.class, R.layout.settings_list_header);

        protected String mTitle;

        public HeaderEntry(String title) {
            mTitle = title;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class HeaderEntryHolder extends EntryHolder<HeaderEntry> {

        protected TextView mTitle;

        public HeaderEntryHolder(View itemView) {
            super(itemView);

            mTitle = (TextView) itemView.findViewById(R.id.title);
        }

        @Override
        public void bind(HeaderEntry entry) {
            mTitle.setText(entry.mTitle);
        }

    }

    public static class SimpleEntry extends Entry {

        private static final int sHolder = registerViewHolder(SimpleEntryHolder.class, R.layout.settings_list_entry);

        protected String mName;
        protected String mValue;

        public SimpleEntry(String name, String value) {
            mName = name;
            mValue = value;
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class SimpleEntryHolder extends EntryHolder<SimpleEntry>
            implements View.OnClickListener {

        protected SettingsListAdapter mAdapter;
        protected TextView mName;
        protected TextView mValue;
        protected View mDivider;

        public SimpleEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView);

            mAdapter = adapter;
            mName = (TextView) itemView.findViewById(R.id.name);
            mValue = (TextView) itemView.findViewById(R.id.value);
            mDivider = itemView.findViewById(R.id.divider);
            itemView.setOnClickListener(this);
        }

        @Override
        public void bind(SimpleEntry entry) {
            mName.setText(entry.mName);
            mValue.setText(entry.mValue);
            mDivider.setVisibility(getAdapterPosition() == mAdapter.mEntries.size() - 1 ? View.GONE : View.VISIBLE);
        }

        @Override
        public void onClick(View v) {
            //
        }

    }

    public static class ListEntry extends SimpleEntry {

        private static final int sHolder = registerViewHolder(ListEntryHolder.class, R.layout.settings_list_entry);

        String[] mOptions;
        int mSelectedOption;

        public ListEntry(String name, String[] options, int selectedOption) {
            super(name, null);
            mOptions = options;
            mSelectedOption = selectedOption;
        }

        public void setSelectedOption(int index) {
            mSelectedOption = index;
            onUpdated();
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class ListEntryHolder extends SimpleEntryHolder {

        public ListEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            ListEntry listEntry = (ListEntry) entry;
            mValue.setText(listEntry.mOptions[listEntry.mSelectedOption]);
        }

        @Override
        public void onClick(View v) {
            ListEntry listEntry = (ListEntry) getEntry();
            new AlertDialog.Builder(v.getContext())
                    .setTitle(listEntry.mName)
                    .setSingleChoiceItems(listEntry.mOptions, listEntry.mSelectedOption,
                            (DialogInterface i, int which) -> {
                                listEntry.setSelectedOption(which);
                        i.cancel();
                    })
                    .setPositiveButton(R.string.action_cancel, null)
                    .show();
        }

    }

    public static class RingtoneEntry extends SimpleEntry implements ActivityResultCallback {

        private static final int sHolder = registerViewHolder(RingtoneEntryHolder.class, R.layout.settings_list_entry);

        SettingsListAdapter mAdapter;
        Uri mValue;
        int mRequestCode;

        public static String getValueDisplayString(Context context, Uri uri) {
            if (uri == null)
                return context.getString(R.string.value_none);
            if (uri.equals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)))
                return context.getString(R.string.value_default);
            Ringtone ret = RingtoneManager.getRingtone(context, uri);
            if (ret != null)
                return ret.getTitle(context);
            return null;
        }

        public RingtoneEntry(SettingsListAdapter adapter, String name, Uri value) {
            super(name, null);
            mAdapter = adapter;
            mValue = value;
            mRequestCode = adapter.mRequestCodeCounter.next();
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == mRequestCode && data != null) {
                mValue = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                onUpdated();
            }
        }

    }

    public static class RingtoneEntryHolder extends SimpleEntryHolder {

        public RingtoneEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            mValue.setText(RingtoneEntry.getValueDisplayString(mAdapter.mActivity, ((RingtoneEntry) entry).mValue));
        }

        @Override
        public void onClick(View v) {
            RingtoneEntry entry = (RingtoneEntry) getEntry();
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, entry.mValue);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, entry.mName);
            intent.putExtra("android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS", 0x1 << 6); // Perhaps a bit of a hack, but imo needed
            mAdapter.mActivity.startActivityForResult(intent, entry.mRequestCode);
        }

    }

    public static class ColorEntry extends SimpleEntry {

        private static final int sHolder = registerViewHolder(ColorEntryHolder.class, R.layout.settings_list_entry_color);

        int[] mColors;
        String[] mColorNames;
        int mSelectedIndex;

        public ColorEntry(String name, int[] colors, String[] colorNames, int selectedIndex) {
            super(name, null);
            mColors = colors;
            mColorNames = colorNames;
            mSelectedIndex = selectedIndex;
        }

        public void setSelectedColor(int index) {
            mSelectedIndex = index;
            onUpdated();
        }

        @Override
        public int getViewHolder() {
            return sHolder;
        }

    }

    public static class ColorEntryHolder extends SimpleEntryHolder {

        private ImageView mColor;

        public ColorEntryHolder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
            mColor = (ImageView) itemView.findViewById(R.id.color);
        }

        @Override
        public void bind(SimpleEntry entry) {
            super.bind(entry);
            ColorEntry colorEntry = (ColorEntry) entry;
            mColor.setColorFilter(colorEntry.mColors[colorEntry.mSelectedIndex], PorterDuff.Mode.MULTIPLY);
            mValue.setText(colorEntry.mColorNames[colorEntry.mSelectedIndex]);
        }

        @Override
        public void onClick(View v) {
            ColorEntry entry = (ColorEntry) getEntry();
            ColorPickerDialog dialog = new ColorPickerDialog(v.getContext());
            dialog.setTitle(entry.mName);
            dialog.setColors(entry.mColors, entry.mSelectedIndex);
            dialog.setPositiveButtonText(R.string.action_cancel);
            dialog.setOnColorChangeListener((ColorPickerDialog d, int colorIndex, int color) -> {
                entry.setSelectedColor(colorIndex);
                dialog.cancel();
            });
            dialog.show();
        }

    }


}
