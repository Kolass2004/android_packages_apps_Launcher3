/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.view.View;

import com.android.launcher3.model.data.ItemInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller for managing multi-select state on the workspace.
 * Tracks selected {@link ItemInfo} items and their corresponding views,
 * provides selection toggle operations, and coordinates with visual
 * feedback on {@link BubbleTextView} instances.
 */
public class MultiSelectController {

    private static final String TAG = "MultiSelectController";

    private final Launcher mLauncher;
    private boolean mIsSelectionMode;

    // Ordered set of selected items, preserving insertion order for batch placement
    private final LinkedHashSet<ItemInfo> mSelectedItems = new LinkedHashSet<>();

    // Map from selected ItemInfo to its corresponding View for visual updates
    private final LinkedHashMap<ItemInfo, View> mSelectedViews = new LinkedHashMap<>();

    // Listeners for selection mode changes
    private final List<SelectionModeListener> mListeners = new ArrayList<>();

    public MultiSelectController(Launcher launcher) {
        mLauncher = launcher;
    }

    /**
     * Enters multi-select mode. Notifies listeners of the mode change.
     */
    public void enterSelectionMode() {
        if (mIsSelectionMode) return;
        mIsSelectionMode = true;
        for (SelectionModeListener listener : mListeners) {
            listener.onSelectionModeChanged(true);
        }
    }

    /**
     * Exits multi-select mode. Clears all selections and visual state.
     */
    public void exitSelectionMode() {
        if (!mIsSelectionMode) return;
        clearSelections();
        mIsSelectionMode = false;
        for (SelectionModeListener listener : mListeners) {
            listener.onSelectionModeChanged(false);
        }
    }

    /**
     * @return true if currently in multi-select mode
     */
    public boolean isSelectionMode() {
        return mIsSelectionMode;
    }

    /**
     * Toggles the selection state of an item. If the item is already selected,
     * it will be deselected (and visual state cleared). Otherwise, it will be
     * added to the selection set and the view will be visually highlighted.
     *
     * If removing the last item, does NOT auto-exit selection mode — the caller
     * can decide whether to exit.
     *
     * @param view the view associated with the item
     * @param info the ItemInfo to toggle
     */
    public void toggleSelection(View view, ItemInfo info) {
        if (mSelectedItems.contains(info)) {
            // Deselect
            mSelectedItems.remove(info);
            mSelectedViews.remove(info);
            setViewSelected(view, false);
        } else {
            // Select
            mSelectedItems.add(info);
            mSelectedViews.put(info, view);
            setViewSelected(view, true);
        }
    }

    /**
     * Selects an item without toggling. No-op if already selected.
     */
    public void selectItem(View view, ItemInfo info) {
        if (!mSelectedItems.contains(info)) {
            mSelectedItems.add(info);
            mSelectedViews.put(info, view);
            setViewSelected(view, true);
        }
    }

    /**
     * @return true if the given item is in the selection set
     */
    public boolean isSelected(ItemInfo info) {
        return mSelectedItems.contains(info);
    }

    /**
     * @return the number of selected items
     */
    public int getSelectedCount() {
        return mSelectedItems.size();
    }

    /**
     * @return an unmodifiable view of the selected items in insertion order
     */
    public Set<ItemInfo> getSelectedItems() {
        return Collections.unmodifiableSet(mSelectedItems);
    }

    /**
     * @return an unmodifiable view of selected item-view pairs
     */
    public Map<ItemInfo, View> getSelectedViews() {
        return Collections.unmodifiableMap(mSelectedViews);
    }

    /**
     * Clears all selections and resets visual state on all tracked views.
     */
    public void clearSelections() {
        for (Map.Entry<ItemInfo, View> entry : mSelectedViews.entrySet()) {
            setViewSelected(entry.getValue(), false);
        }
        mSelectedItems.clear();
        mSelectedViews.clear();
    }

    /**
     * Called when a batch drag operation begins.
     * Hides all selected views so the user sees only the drag preview.
     */
    public void onDragStarted() {
        for (View v : mSelectedViews.values()) {
            v.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Called when a batch drag completes successfully.
     * Exits selection mode (visual cleanup happens in exitSelectionMode).
     */
    public void onDragCompleted() {
        exitSelectionMode();
    }

    /**
     * Called when a batch drag is cancelled.
     * Restores visibility of all selected views, then exits selection mode.
     */
    public void onDragCancelled() {
        for (View v : mSelectedViews.values()) {
            v.setVisibility(View.VISIBLE);
        }
        exitSelectionMode();
    }

    /**
     * Sets the multi-selected visual state on a view.
     * If the view is a BubbleTextView, delegates to its setMultiSelected method.
     */
    private void setViewSelected(View view, boolean selected) {
        if (view instanceof BubbleTextView) {
            ((BubbleTextView) view).setMultiSelected(selected);
        }
    }

    public void addListener(SelectionModeListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(SelectionModeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Listener interface for selection mode state changes.
     */
    public interface SelectionModeListener {
        void onSelectionModeChanged(boolean isSelectionMode);
    }
}
