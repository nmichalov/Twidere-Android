/*
 * Twidere - Twitter client for Android
 *
 *  Copyright (C) 2012-2015 Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util

import android.accounts.AccountManager
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Parcelable
import android.support.annotation.UiThread
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.MenuItemCompat
import android.support.v7.widget.ShareActionProvider
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import org.mariotaku.kpreferences.get
import org.mariotaku.ktextension.Bundle
import org.mariotaku.ktextension.set
import org.mariotaku.ktextension.setItemAvailability
import org.mariotaku.microblog.library.annotation.mastodon.StatusVisibility
import org.mariotaku.twidere.Constants.*
import org.mariotaku.twidere.R
import org.mariotaku.twidere.activity.BaseActivity
import org.mariotaku.twidere.activity.ColorPickerDialogActivity
import org.mariotaku.twidere.app.TwidereApplication
import org.mariotaku.twidere.constant.favoriteConfirmationKey
import org.mariotaku.twidere.constant.iWantMyStarsBackKey
import org.mariotaku.twidere.extension.getDetails
import org.mariotaku.twidere.extension.model.isAccountRetweet
import org.mariotaku.twidere.extension.model.isAccountStatus
import org.mariotaku.twidere.extension.model.isOfficial
import org.mariotaku.twidere.extension.promise
import org.mariotaku.twidere.fragment.AddStatusFilterDialogFragment
import org.mariotaku.twidere.fragment.BaseFragment
import org.mariotaku.twidere.fragment.SetUserNicknameDialogFragment
import org.mariotaku.twidere.fragment.status.*
import org.mariotaku.twidere.fragment.timeline.AbsTimelineFragment
import org.mariotaku.twidere.graphic.PaddingDrawable
import org.mariotaku.twidere.menu.FavoriteItemProvider
import org.mariotaku.twidere.menu.SupportStatusShareProvider
import org.mariotaku.twidere.model.AccountDetails
import org.mariotaku.twidere.model.ParcelableStatus
import org.mariotaku.twidere.task.CreateFavoriteTask
import org.mariotaku.twidere.task.DestroyFavoriteTask
import org.mariotaku.twidere.task.DestroyStatusTask
import org.mariotaku.twidere.task.RetweetStatusTask
import org.mariotaku.twidere.view.ExtendedRecyclerView
import java.io.IOException

object MenuUtils {

    fun addIntentToMenu(context: Context, menu: Menu, queryIntent: Intent,
            groupId: Int = Menu.NONE) {
        val pm = context.packageManager
        val res = context.resources
        val density = res.displayMetrics.density
        val padding = Math.round(density * 4)
        val activities = pm.queryIntentActivities(queryIntent, 0)
        for (info in activities) {
            val intent = Intent(queryIntent)
            val icon = info.loadIcon(pm)
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name)
            val item = menu.add(groupId, Menu.NONE, Menu.NONE, info.loadLabel(pm))
            item.intent = intent
            val iw = icon.intrinsicWidth
            val ih = icon.intrinsicHeight
            if (iw > 0 && ih > 0) {
                val iconWithPadding = PaddingDrawable(icon, padding)
                iconWithPadding.setBounds(0, 0, iw, ih)
                item.icon = iconWithPadding
            } else {
                item.icon = icon
            }
        }
    }

    fun setupForStatus(context: Context, menu: Menu, preferences: SharedPreferences,
            manager: UserColorNameManager, status: ParcelableStatus) {
        val account = AccountManager.get(context).getDetails(status.account_key, true) ?: return
        setupForStatus(context, menu, preferences, manager, status, account)
    }

    @UiThread
    fun setupForStatus(context: Context, menu: Menu, preferences: SharedPreferences,
            manager: UserColorNameManager, status: ParcelableStatus, details: AccountDetails) {
        if (menu is ContextMenu) {
            val displayName = manager.getDisplayName(status.user_key, status.user_name,
                    status.user_screen_name)
            menu.setHeaderTitle(context.getString(R.string.status_menu_title_format, displayName,
                    status.text_unescaped))
        }
        val isMyRetweet = when {
            RetweetStatusTask.isRunning(status.account_key, status.id) -> true
            DestroyStatusTask.isRunning(status.account_key, status.id) -> false
            else -> status.retweeted || status.isAccountRetweet
        }
        val isMyStatus = status.isAccountRetweet || status.isAccountStatus
        menu.setItemAvailability(R.id.delete, isMyStatus)
        if (isMyStatus) {
            val isPinned = status.is_pinned_status
            menu.setItemAvailability(R.id.pin, !isPinned)
            menu.setItemAvailability(R.id.unpin, isPinned)
        } else {
            menu.setItemAvailability(R.id.pin, false)
            menu.setItemAvailability(R.id.unpin, false)
        }

        menu.findItem(R.id.retweet)?.apply {
            when (status.extras?.visibility) {
                StatusVisibility.PRIVATE -> {
                    setIcon(R.drawable.ic_action_lock)
                }
                StatusVisibility.DIRECT -> {
                    setIcon(R.drawable.ic_action_message)
                }
                else -> {
                    setIcon(R.drawable.ic_action_retweet)
                }
            }
            if (isMyRetweet) {
                MenuItemCompat.setIconTintList(this, ContextCompat.getColorStateList(context, R.color.highlight_retweet))
                setTitle(R.string.action_cancel_retweet)
            } else {
                MenuItemCompat.setIconTintList(this, ThemeUtils.getColorStateListFromAttribute(context, android.R.attr.textColorSecondary))
                setTitle(R.string.action_retweet)
            }
        }

        menu.findItem(R.id.favorite)?.apply {
            val isFavorite = when {
                CreateFavoriteTask.isRunning(status.account_key, status.id) -> true
                DestroyFavoriteTask.isRunning(status.account_key, status.id) -> false
                else -> status.is_favorite
            }
            val provider = MenuItemCompat.getActionProvider(this)
            val useStar = preferences[iWantMyStarsBackKey]
            if (provider is FavoriteItemProvider) {
                provider.setIsFavorite(this, isFavorite)
            } else {
                MenuItemCompat.setIconTintList(this, when {
                    !isFavorite -> ThemeUtils.getColorStateListFromAttribute(context, android.R.attr.textColorSecondary)
                    useStar -> ContextCompat.getColorStateList(context, R.color.highlight_favorite)
                    else -> ContextCompat.getColorStateList(context, R.color.highlight_like)
                })
            }
            if (useStar) {
                setTitle(if (isFavorite) R.string.action_unfavorite else R.string.action_favorite)
                setIcon(R.drawable.ic_action_star)
            } else {
                setTitle(if (isFavorite) R.string.action_undo_like else R.string.action_like)
                setIcon(R.drawable.ic_action_heart)
            }
        }

        val linkAvailable = LinkCreator.hasWebLink(status)

        menu.setItemAvailability(R.id.translate, details.isOfficial(context))

        menu.setItemAvailability(R.id.copy_url, linkAvailable)
        menu.setItemAvailability(R.id.open_in_browser, linkAvailable)

        menu.removeGroup(MENU_GROUP_STATUS_EXTENSION)
        addIntentToMenuForExtension(context, menu, MENU_GROUP_STATUS_EXTENSION,
                INTENT_ACTION_EXTENSION_OPEN_STATUS, EXTRA_STATUS, EXTRA_STATUS_JSON, status)
        val shareItem = menu.findItem(R.id.share)
        val shareProvider = MenuItemCompat.getActionProvider(shareItem)
        when {
            shareProvider is SupportStatusShareProvider -> shareProvider.status = status
            shareProvider is ShareActionProvider -> {
                val shareIntent = Utils.createStatusShareIntent(context, status)
                shareProvider.setShareIntent(shareIntent)
            }
            shareItem.hasSubMenu() -> {
                val shareSubMenu = shareItem.subMenu
                val shareIntent = Utils.createStatusShareIntent(context, status)
                shareSubMenu.removeGroup(MENU_GROUP_STATUS_SHARE)
                addIntentToMenu(context, shareSubMenu, shareIntent, MENU_GROUP_STATUS_SHARE)
            }
            else -> {
                val shareIntent = Utils.createStatusShareIntent(context, status)
                val chooserIntent = Intent.createChooser(shareIntent, context.getString(R.string.share_status))
                shareItem.intent = chooserIntent
            }
        }

    }

    fun handleStatusClick(context: Context, fragment: Fragment?, fm: FragmentManager,
            preferences: SharedPreferences, colorNameManager: UserColorNameManager,
            status: ParcelableStatus, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.copy -> {
                if (ClipboardUtils.setText(context, status.text_plain)) {
                    Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
                }
            }
            R.id.retweet -> when {
                fragment is BaseFragment -> fragment.executeAfterFragmentResumed {
                    RetweetQuoteDialogFragment.show(it.childFragmentManager, status.account_key,
                            status.id, status)
                }
                context is BaseActivity -> context.executeAfterFragmentResumed {
                    RetweetQuoteDialogFragment.show(it.supportFragmentManager, status.account_key,
                            status.id, status)
                }
                else -> RetweetQuoteDialogFragment.show(fm, status.account_key,
                        status.id, status)
            }
            R.id.quote -> {
                val intent = Intent(INTENT_ACTION_QUOTE)
                intent.putExtra(EXTRA_STATUS, status)
                context.startActivity(intent)
            }
            R.id.reply -> {
                val intent = Intent(INTENT_ACTION_REPLY)
                intent.putExtra(EXTRA_STATUS, status)
                context.startActivity(intent)
            }
            R.id.favorite -> {
                if (preferences[favoriteConfirmationKey]) when {
                    fragment is BaseFragment -> fragment.executeAfterFragmentResumed {
                        FavoriteConfirmDialogFragment.show(it.childFragmentManager,
                                status.account_key, status.id, status)
                    }
                    context is BaseActivity -> context.executeAfterFragmentResumed {
                        FavoriteConfirmDialogFragment.show(it.supportFragmentManager,
                                status.account_key, status.id, status)
                    }
                    else -> FavoriteConfirmDialogFragment.show(fm, status.account_key, status.id,
                            status)
                } else if (status.is_favorite) {
                    DestroyFavoriteTask(context, status.account_key, status.id).promise()
                } else {
                    val provider = MenuItemCompat.getActionProvider(item)
                    if (provider is FavoriteItemProvider) {
                        provider.invokeItem(item, AbsTimelineFragment.DefaultOnLikedListener(context, status))
                    } else {
                        CreateFavoriteTask(context, status.account_key, status).promise()
                    }
                }
            }
            R.id.delete -> {
                DestroyStatusDialogFragment.show(fm, status)
            }
            R.id.pin -> {
                PinStatusDialogFragment.show(fm, status)
            }
            R.id.unpin -> {
                UnpinStatusDialogFragment.show(fm, status)
            }
            R.id.add_to_filter -> {
                AddStatusFilterDialogFragment.show(fm, status)
            }
            R.id.set_color -> {
                val intent = Intent(context, ColorPickerDialogActivity::class.java)
                val color = colorNameManager.getUserColor(status.user_key)
                if (color != 0) {
                    intent.putExtra(EXTRA_COLOR, color)
                }
                intent.putExtra(EXTRA_CLEAR_BUTTON, color != 0)
                intent.putExtra(EXTRA_ALPHA_SLIDER, false)
                if (fragment != null) {
                    fragment.startActivityForResult(intent, REQUEST_SET_COLOR)
                } else if (context is Activity) {
                    context.startActivityForResult(intent, REQUEST_SET_COLOR)
                }
            }
            R.id.clear_nickname -> {
                colorNameManager.clearUserNickname(status.user_key)
            }
            R.id.set_nickname -> {
                val nick = colorNameManager.getUserNickname(status.user_key)
                val df = SetUserNicknameDialogFragment.create(status.user_key, nick)
                if (fragment != null) {
                    df.setTargetFragment(fragment, REQUEST_SET_NICKNAME)
                }
                df.show(fm, SetUserNicknameDialogFragment.FRAGMENT_TAG)
            }
            R.id.open_with_account -> {
                val itemId = (item.menuInfo as? ExtendedRecyclerView.ContextMenuInfo)?.itemId ?: -1
                val intent = AbsTimelineFragment.selectAccountIntent(context, status, itemId, true)
                if (fragment != null) {
                    fragment.startActivityForResult(intent, AbsTimelineFragment.REQUEST_OPEN_SELECT_ACCOUNT)
                } else if (context is Activity) {
                    context.startActivityForResult(intent, AbsTimelineFragment.REQUEST_OPEN_SELECT_ACCOUNT)
                }
            }
            R.id.open_in_browser -> {
                val uri = LinkCreator.getStatusWebLink(status) ?: return true
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.`package` = IntentUtils.getDefaultBrowserPackage(context, uri, true)
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    intent.`package` = null
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_open_in_browser)))
                }
            }
            R.id.copy_url -> {
                val uri = LinkCreator.getStatusWebLink(status) ?: return true
                ClipboardUtils.setText(context, uri.toString())
                Toast.makeText(context, R.string.message_toast_link_copied_to_clipboard,
                        Toast.LENGTH_SHORT).show()
            }
            R.id.mute_users -> {
                val df = MuteStatusUsersDialogFragment()
                df.arguments = Bundle {
                    this[EXTRA_STATUS] = status
                }
                df.show(fm, "mute_users_selector")
            }
            R.id.block_users -> {
                val df = BlockStatusUsersDialogFragment()
                df.arguments = Bundle {
                    this[EXTRA_STATUS] = status
                }
                df.show(fm, "block_users_selector")
            }
            else -> {
                if (item.intent != null) {
                    try {
                        context.startActivity(item.intent)
                    } catch (e: ActivityNotFoundException) {
                        Log.w(LOGTAG, e)
                        return false
                    }

                }
            }
        }
        return true
    }


    fun addIntentToMenuForExtension(context: Context, menu: Menu, groupId: Int, action: String,
            parcelableKey: String, jsonKey: String, obj: Parcelable) {
        val pm = context.packageManager
        val res = context.resources
        val density = res.displayMetrics.density
        val padding = Math.round(density * 4)
        val queryIntent = Intent(action)
        queryIntent.setExtrasClassLoader(TwidereApplication::class.java.classLoader)
        val activities = pm.queryIntentActivities(queryIntent, PackageManager.GET_META_DATA)
        val parcelableJson = try {
            JsonSerializer.serialize(obj)
        } catch (e: IOException) {
            null
        }
        for (info in activities) {
            val intent = Intent(queryIntent)
            if (Utils.isExtensionUseJSON(info) && parcelableJson != null) {
                intent.putExtra(jsonKey, parcelableJson)
            } else {
                intent.putExtra(parcelableKey, obj)
            }
            intent.setClassName(info.activityInfo.packageName, info.activityInfo.name)
            val item = menu.add(groupId, Menu.NONE, Menu.NONE, info.loadLabel(pm))
            item.intent = intent
            val metaDataDrawable = Utils.getMetadataDrawable(pm, info.activityInfo, METADATA_KEY_EXTENSION_ICON)
            val actionIconColor = ThemeUtils.getThemeForegroundColor(context)
            if (metaDataDrawable != null) {
                metaDataDrawable.mutate()
                DrawableCompat.setTint(metaDataDrawable, actionIconColor)
                item.icon = metaDataDrawable
            } else {
                val icon = info.loadIcon(pm)
                val iw = icon.intrinsicWidth
                val ih = icon.intrinsicHeight
                if (iw > 0 && ih > 0) {
                    val iconWithPadding = PaddingDrawable(icon, padding)
                    iconWithPadding.setBounds(0, 0, iw, ih)
                    item.icon = iconWithPadding
                } else {
                    item.icon = icon
                }
            }

        }
    }

}

