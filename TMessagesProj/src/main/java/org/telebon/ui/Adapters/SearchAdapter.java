/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telebon.ui.Adapters;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import org.telebon.PhoneFormat.PhoneFormat;
import org.telebon.messenger.AndroidUtilities;
import org.telebon.messenger.LocaleController;
import org.telebon.messenger.R;
import org.telebon.tgnet.TLObject;
import org.telebon.tgnet.TLRPC;
import org.telebon.messenger.ContactsController;
import org.telebon.messenger.FileLog;
import org.telebon.messenger.MessagesController;
import org.telebon.messenger.UserConfig;
import org.telebon.messenger.Utilities;
import org.telebon.ui.ActionBar.Theme;
import org.telebon.ui.Cells.GraySectionCell;
import org.telebon.ui.Cells.ProfileSearchCell;
import org.telebon.ui.Cells.TextCell;
import org.telebon.ui.Cells.UserCell;
import org.telebon.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import androidx.recyclerview.widget.RecyclerView;

public class SearchAdapter extends RecyclerListView.SelectionAdapter {

    private Context mContext;
    private SparseArray<TLRPC.User> ignoreUsers;
    private ArrayList<TLObject> searchResult = new ArrayList<>();
    private ArrayList<CharSequence> searchResultNames = new ArrayList<>();
    private SearchAdapterHelper searchAdapterHelper;
    private SparseArray<?> checkedMap;
    private Timer searchTimer;
    private boolean allowUsernameSearch;
    private boolean useUserCell;
    private boolean onlyMutual;
    private boolean allowChats;
    private boolean allowBots;
    private boolean allowPhoneNumbers;
    private int channelId;

    public SearchAdapter(Context context, SparseArray<TLRPC.User> arg1, boolean usernameSearch, boolean mutual, boolean chats, boolean bots, boolean phones, int searchChannelId) {
        mContext = context;
        ignoreUsers = arg1;
        onlyMutual = mutual;
        allowUsernameSearch = usernameSearch;
        allowChats = chats;
        allowBots = bots;
        channelId = searchChannelId;
        allowPhoneNumbers = phones;
        searchAdapterHelper = new SearchAdapterHelper(true);
        searchAdapterHelper.setDelegate(new SearchAdapterHelper.SearchAdapterHelperDelegate() {
            @Override
            public void onDataSetChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onSetHashtags(ArrayList<SearchAdapterHelper.HashtagObject> arrayList, HashMap<String, SearchAdapterHelper.HashtagObject> hashMap) {

            }

            @Override
            public SparseArray<TLRPC.User> getExcludeUsers() {
                return ignoreUsers;
            }
        });
    }

    public void setCheckedMap(SparseArray<?> map) {
        checkedMap = map;
    }

    public void setUseUserCell(boolean value) {
        useUserCell = value;
    }

    public void searchDialogs(final String query) {
        try {
            if (searchTimer != null) {
                searchTimer.cancel();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (query == null) {
            searchResult.clear();
            searchResultNames.clear();
            if (allowUsernameSearch) {
                searchAdapterHelper.queryServerSearch(null, true, allowChats, allowBots, true, channelId, allowPhoneNumbers, 0);
            }
            notifyDataSetChanged();
        } else {
            searchTimer = new Timer();
            searchTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        searchTimer.cancel();
                        searchTimer = null;
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    processSearch(query);
                }
            }, 200, 300);
        }
    }

    private void processSearch(final String query) {
        AndroidUtilities.runOnUIThread(() -> {
            if (allowUsernameSearch) {
                searchAdapterHelper.queryServerSearch(query, true, allowChats, allowBots, true, channelId, allowPhoneNumbers, -1);
            }
            final int currentAccount = UserConfig.selectedAccount;
            final ArrayList<TLRPC.TL_contact> contactsCopy = new ArrayList<>(ContactsController.getInstance(currentAccount).contacts);
            Utilities.searchQueue.postRunnable(() -> {
                String search1 = query.trim().toLowerCase();
                if (search1.length() == 0) {
                    updateSearchResults(new ArrayList<>(), new ArrayList<>());
                    return;
                }
                String search2 = LocaleController.getInstance().getTranslitString(search1);
                if (search1.equals(search2) || search2.length() == 0) {
                    search2 = null;
                }
                String[] search = new String[1 + (search2 != null ? 1 : 0)];
                search[0] = search1;
                if (search2 != null) {
                    search[1] = search2;
                }

                ArrayList<TLObject> resultArray = new ArrayList<>();
                ArrayList<CharSequence> resultArrayNames = new ArrayList<>();

                for (int a = 0; a < contactsCopy.size(); a++) {
                    TLRPC.TL_contact contact = contactsCopy.get(a);
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(contact.user_id);
                    if (user.id == UserConfig.getInstance(currentAccount).getClientUserId() || onlyMutual && !user.mutual_contact || ignoreUsers != null && ignoreUsers.indexOfKey(contact.user_id) >= 0) {
                        continue;
                    }

                    String name = ContactsController.formatName(user.first_name, user.last_name).toLowerCase();
                    String tName = LocaleController.getInstance().getTranslitString(name);
                    if (name.equals(tName)) {
                        tName = null;
                    }

                    int found = 0;
                    for (String q : search) {
                        if (name.startsWith(q) || name.contains(" " + q) || tName != null && (tName.startsWith(q) || tName.contains(" " + q))) {
                            found = 1;
                        } else if (user.username != null && user.username.startsWith(q)) {
                            found = 2;
                        }

                        if (found != 0) {
                            if (found == 1) {
                                resultArrayNames.add(AndroidUtilities.generateSearchName(user.first_name, user.last_name, q));
                            } else {
                                resultArrayNames.add(AndroidUtilities.generateSearchName("@" + user.username, null, "@" + q));
                            }
                            resultArray.add(user);
                            break;
                        }
                    }
                }

                updateSearchResults(resultArray, resultArrayNames);
            });
        });
    }

    private void updateSearchResults(final ArrayList<TLObject> users, final ArrayList<CharSequence> names) {
        AndroidUtilities.runOnUIThread(() -> {
            searchResult = users;
            searchResultNames = names;
            searchAdapterHelper.mergeResults(users);
            notifyDataSetChanged();
        });
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int type = holder.getItemViewType();
        return type == 0 || type == 2;
    }

    @Override
    public int getItemCount() {
        int count = searchResult.size();
        int globalCount = searchAdapterHelper.getGlobalSearch().size();
        if (globalCount != 0) {
            count += globalCount + 1;
        }
        int phoneCount = searchAdapterHelper.getPhoneSearch().size();
        if (phoneCount != 0) {
            count += phoneCount;
        }
        return count;
    }

    public boolean isGlobalSearch(int i) {
        int localCount = searchResult.size();
        int globalCount = searchAdapterHelper.getGlobalSearch().size();
        int phoneCount = searchAdapterHelper.getPhoneSearch().size();
        if (i >= 0 && i < localCount) {
            return false;
        } else if (i > localCount && i < localCount + phoneCount) {
            return false;
        } else if (i > localCount + phoneCount && i <= globalCount + phoneCount + localCount) {
            return true;
        }
        return false;
    }

    public Object getItem(int i) {
        int localCount = searchResult.size();
        int globalCount = searchAdapterHelper.getGlobalSearch().size();
        int phoneCount = searchAdapterHelper.getPhoneSearch().size();
        if (i >= 0 && i < localCount) {
            return searchResult.get(i);
        } else {
            i -= localCount;
            if (i >= 0 && i < phoneCount) {
                return searchAdapterHelper.getPhoneSearch().get(i);
            } else {
                i -= phoneCount;
                if (i > 0 && i <= globalCount) {
                    return searchAdapterHelper.getGlobalSearch().get(i - 1);
                }
            }
        }
        return null;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                if (useUserCell) {
                    view = new UserCell(mContext, 1, 1, false);
                    if (checkedMap != null) {
                        ((UserCell) view).setChecked(false, false);
                    }
                } else {
                    view = new ProfileSearchCell(mContext);
                }
                break;
            case 1:
                view = new GraySectionCell(mContext);
                break;
            case 2:
            default:
                view = new TextCell(mContext, 16);
                break;
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                TLObject object = (TLObject) getItem(position);
                if (object != null) {
                    int id = 0;
                    String un = null;
                    if (object instanceof TLRPC.User) {
                        un = ((TLRPC.User) object).username;
                        id = ((TLRPC.User) object).id;
                    } else if (object instanceof TLRPC.Chat) {
                        un = ((TLRPC.Chat) object).username;
                        id = ((TLRPC.Chat) object).id;
                    }

                    CharSequence username = null;
                    CharSequence name = null;
                    if (position < searchResult.size()) {
                        name = searchResultNames.get(position);
                        if (name != null && un != null && un.length() > 0) {
                            if (name.toString().startsWith("@" + un)) {
                                username = name;
                                name = null;
                            }
                        }
                    } else if (position > searchResult.size() && un != null) {
                        String foundUserName = searchAdapterHelper.getLastFoundUsername();
                        if (foundUserName.startsWith("@")) {
                            foundUserName = foundUserName.substring(1);
                        }
                        try {
                            int index;
                            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                            spannableStringBuilder.append("@");
                            spannableStringBuilder.append(un);
                            if ((index = AndroidUtilities.indexOfIgnoreCase(un, foundUserName)) != -1) {
                                int len = foundUserName.length();
                                if (index == 0) {
                                    len++;
                                } else {
                                    index++;
                                }
                                spannableStringBuilder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            username = spannableStringBuilder;
                        } catch (Exception e) {
                            username = un;
                            FileLog.e(e);
                        }
                    }

                    if (useUserCell) {
                        UserCell userCell = (UserCell) holder.itemView;
                        userCell.setData(object, name, username, 0);
                        if (checkedMap != null) {
                            userCell.setChecked(checkedMap.indexOfKey(id) >= 0, false);
                        }
                    } else {
                        ProfileSearchCell profileSearchCell = (ProfileSearchCell) holder.itemView;
                        profileSearchCell.setData(object, null, name, username, false, false);
                        profileSearchCell.useSeparator = (position != getItemCount() - 1 && position != searchResult.size() - 1);
                        /*if (ignoreUsers != null) {
                            if (ignoreUsers.containsKey(id)) {
                                profileSearchCell.drawAlpha = 0.5f;
                            } else {
                                profileSearchCell.drawAlpha = 1.0f;
                            }
                        }*/
                    }
                }
                break;
            }
            case 1: {
                GraySectionCell cell = (GraySectionCell) holder.itemView;
                if (getItem(position) == null) {
                    cell.setText(LocaleController.getString("GlobalSearch", R.string.GlobalSearch));
                } else {
                    cell.setText(LocaleController.getString("PhoneNumberSearch", R.string.PhoneNumberSearch));
                }
                break;
            }
            case 2: {
                String str = (String) getItem(position);
                TextCell cell = (TextCell) holder.itemView;
                cell.setColors(null, Theme.key_windowBackgroundWhiteBlueText2);
                cell.setText(LocaleController.formatString("AddContactByPhone", R.string.AddContactByPhone, PhoneFormat.getInstance().format("+" + str)), false);
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int i) {
        Object item = getItem(i);
        if (item == null) {
            return 1;
        } else if (item instanceof String) {
            String str = (String) item;
            if ("section".equals(str)) {
                return 1;
            } else {
                return 2;
            }
        }
        return 0;
    }
}
