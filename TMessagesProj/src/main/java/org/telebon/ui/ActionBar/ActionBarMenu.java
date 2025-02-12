/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telebon.ui.ActionBar;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telebon.messenger.AndroidUtilities;

public class ActionBarMenu extends LinearLayout {

    protected ActionBar parentActionBar;
    protected boolean isActionMode;

    public ActionBarMenu(Context context, ActionBar layer) {
        super(context);
        setOrientation(LinearLayout.HORIZONTAL);
        parentActionBar = layer;
    }

    public ActionBarMenu(Context context) {
        super(context);
    }

    protected void updateItemsBackgroundColor() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                view.setBackgroundDrawable(Theme.createSelectorDrawable(isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor));
            }
        }
    }

    protected void updateItemsColor() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) view).setIconColor(isActionMode ? parentActionBar.itemsActionModeColor : parentActionBar.itemsColor);
            }
        }
    }

    public ActionBarMenuItem addItem(int id, Drawable drawable) {
        return addItem(id, 0, null, isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor, drawable, AndroidUtilities.dp(48), null);
    }

    public ActionBarMenuItem addItem(int id, int icon) {
        return addItem(id, icon, isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor);
    }

    public ActionBarMenuItem addItem(int id, CharSequence text) {
        return addItem(id, 0, text, isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor, null, 0, text);
    }

    public ActionBarMenuItem addItem(int id, int icon, int backgroundColor) {
        return addItem(id, icon, null, backgroundColor, null, AndroidUtilities.dp(48), null);
    }

    public ActionBarMenuItem addItemWithWidth(int id, int icon, int width) {
        return addItem(id, icon, null, isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor, null, width, null);
    }

    public ActionBarMenuItem addItemWithWidth(int id, int icon, int width, CharSequence title) {
        return addItem(id, icon, null, isActionMode ? parentActionBar.itemsActionModeBackgroundColor : parentActionBar.itemsBackgroundColor, null, width, title);
    }

    public ActionBarMenuItem addItem(int id, int icon, CharSequence text, int backgroundColor, Drawable drawable, int width, CharSequence title) {
        ActionBarMenuItem menuItem = new ActionBarMenuItem(getContext(), this, backgroundColor, isActionMode ? parentActionBar.itemsActionModeColor : parentActionBar.itemsColor, text != null);
        menuItem.setTag(id);
        if (text != null) {
            menuItem.textView.setText(text);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(width != 0 ? width : ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.leftMargin = layoutParams.rightMargin = AndroidUtilities.dp(14);
            addView(menuItem, layoutParams);
        } else {
            if (drawable != null) {
                menuItem.iconView.setImageDrawable(drawable);
            } else if (icon != 0) {
                menuItem.iconView.setImageResource(icon);
            }
            addView(menuItem, new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        menuItem.setOnClickListener(view -> {
            ActionBarMenuItem item = (ActionBarMenuItem) view;
            if (item.hasSubMenu()) {
                if (parentActionBar.actionBarMenuOnItemClick.canOpenMenu()) {
                    item.toggleSubMenu();
                }
            } else if (item.isSearchField()) {
                parentActionBar.onSearchFieldVisibilityChanged(item.toggleSearch(true));
            } else {
                onItemClick((Integer) view.getTag());
            }
        });
        if (title != null) {
            menuItem.setContentDescription(title);
        }
        return menuItem;
    }

    public void hideAllPopupMenus() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ((ActionBarMenuItem) view).closeSubMenu();
            }
        }
    }

    protected void setPopupItemsColor(int color, boolean icon) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                item.setPopupItemsColor(color, icon);
            }
        }
    }

    protected void redrawPopup(int color) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                item.redrawPopup(color);
            }
        }
    }

    public void onItemClick(int id) {
        if (parentActionBar.actionBarMenuOnItemClick != null) {
            parentActionBar.actionBarMenuOnItemClick.onItemClick(id);
        }
    }

    public void clearItems() {
        removeAllViews();
    }

    public void onMenuButtonPressed() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.getVisibility() != VISIBLE) {
                    continue;
                }
                if (item.hasSubMenu()) {
                    item.toggleSubMenu();
                    break;
                } else if (item.overrideMenuClick) {
                    onItemClick((Integer) item.getTag());
                    break;
                }
            }
        }
    }

    public void closeSearchField(boolean closeKeyboard) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    parentActionBar.onSearchFieldVisibilityChanged(false);
                    item.toggleSearch(closeKeyboard);
                    break;
                }
            }
        }
    }

    public void setSearchTextColor(int color, boolean placeholder) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    if (placeholder) {
                        item.getSearchField().setHintTextColor(color);
                    } else {
                        item.getSearchField().setTextColor(color);
                    }
                    break;
                }
            }
        }
    }

    public void setSearchFieldText(String text) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    item.setSearchFieldText(text, false);
                    item.getSearchField().setSelection(text.length());
                    break;
                }
            }
        }
    }

    public void onSearchPressed() {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    item.onSearchPressed();
                    break;
                }
            }
        }
    }

    public void openSearchField(boolean toggle, String text, boolean animated) {
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            if (view instanceof ActionBarMenuItem) {
                ActionBarMenuItem item = (ActionBarMenuItem) view;
                if (item.isSearchField()) {
                    if (toggle) {
                        parentActionBar.onSearchFieldVisibilityChanged(item.toggleSearch(true));
                    }
                    item.setSearchFieldText(text, animated);
                    item.getSearchField().setSelection(text.length());
                    break;
                }
            }
        }
    }

    public ActionBarMenuItem getItem(int id) {
        View v = findViewWithTag(id);
        if (v instanceof ActionBarMenuItem) {
            return (ActionBarMenuItem) v;
        }
        return null;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        int count = getChildCount();
        for (int a = 0; a < count; a++) {
            View view = getChildAt(a);
            view.setEnabled(enabled);
        }
    }
}
