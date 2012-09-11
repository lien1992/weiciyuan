package org.qii.weiciyuan.ui.actionmenu;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.v4.app.Fragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.MessageBean;
import org.qii.weiciyuan.dao.destroy.DestroyStatusDao;
import org.qii.weiciyuan.support.error.WeiboException;
import org.qii.weiciyuan.support.lib.MyAsyncTask;
import org.qii.weiciyuan.support.utils.GlobalContext;
import org.qii.weiciyuan.ui.Abstract.IToken;
import org.qii.weiciyuan.ui.adapter.StatusesListAdapter;
import org.qii.weiciyuan.ui.basefragment.AbstractTimeLineFragment;
import org.qii.weiciyuan.ui.send.CommentNewActivity;
import org.qii.weiciyuan.ui.send.RepostNewActivity;

import java.util.List;

/**
 * User: qii
 * Date: 12-9-9
 */
public class StatusSingleChoiceModeListener implements ActionMode.Callback {

    ListView listView;
    StatusesListAdapter adapter;
    Fragment activity;
    ActionMode mode;
    MessageBean bean;
    ShareActionProvider mShareActionProvider;

    public void finish() {
        if (mode != null)
            mode.finish();
    }

    public StatusSingleChoiceModeListener(ListView listView, StatusesListAdapter adapter, Fragment activity, MessageBean bean) {
        this.listView = listView;
        this.activity = activity;
        this.adapter = adapter;
        this.bean = bean;
    }

    private Activity getActivity() {
        return activity.getActivity();
    }


    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (this.mode == null)
            this.mode = mode;

        return true;

    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        menu.clear();
        if (bean.getUser().getId().equals(GlobalContext.getInstance().getCurrentAccountId())) {
            inflater.inflate(R.menu.fragment_status_listview_item_contextual_menu_myself, menu);
        } else {
            inflater.inflate(R.menu.fragment_status_listview_item_contextual_menu, menu);
        }

        mode.setTitle(bean.getUser().getScreen_name());

        MenuItem item = menu.findItem(R.id.menu_share);
        mShareActionProvider = (ShareActionProvider) item.getActionProvider();

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, bean.getText());
        PackageManager packageManager = getActivity().getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(sharingIntent, 0);
        boolean isIntentSafe = activities.size() > 0;
        if (isIntentSafe && mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(sharingIntent);
        }

        return true;


    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        Intent intent;
        long[] ids = listView.getCheckedItemIds();
        switch (item.getItemId()) {
            case R.id.menu_repost:
                MessageBean msg = (MessageBean) adapter.getItem(listView.getCheckedItemPositions().keyAt(listView.getCheckedItemCount() - 1) - 1);
                intent = new Intent(getActivity(), RepostNewActivity.class);
                intent.putExtra("token", ((IToken) getActivity()).getToken());
                intent.putExtra("id", String.valueOf(ids[0]));
                intent.putExtra("msg", msg);
                getActivity().startActivity(intent);
                listView.clearChoices();
                mode.finish();
                break;
            case R.id.menu_comment:
                intent = new Intent(getActivity(), CommentNewActivity.class);
                intent.putExtra("token", ((IToken) getActivity()).getToken());
                intent.putExtra("id", String.valueOf(ids[0]));
                getActivity().startActivity(intent);
                listView.clearChoices();
                mode.finish();

                break;
            case R.id.menu_fav:
                Toast.makeText(getActivity(), "fav", Toast.LENGTH_SHORT).show();
                listView.clearChoices();
                listView.clearChoices();
                mode.finish();
                break;
            case R.id.menu_remove:
                String token = ((IToken) getActivity()).getToken();
                int position = listView.getCheckedItemPosition() - 1;
                new RemoveTask(token, bean.getId(),position).executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                listView.clearChoices();
                mode.finish();
                break;
            case R.id.menu_share:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, bean.getText());
                PackageManager packageManager = getActivity().getPackageManager();
                List<ResolveInfo> activities = packageManager.queryIntentActivities(sharingIntent, 0);
                boolean isIntentSafe = activities.size() > 0;
                if (isIntentSafe && mShareActionProvider != null) {
                    mShareActionProvider.setShareIntent(sharingIntent);
                }
                break;
        }


        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.mode = null;
        listView.clearChoices();
        adapter.notifyDataSetChanged();
        ((AbstractTimeLineFragment) activity).setmActionMode(null);

    }


    class RemoveTask extends MyAsyncTask<Void, Void, Boolean> {

        String token;
        String id;
        int positon;
        WeiboException e;

        public RemoveTask(String token, String id, int positon) {
            this.token = token;
            this.id = id;
            this.positon = positon;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DestroyStatusDao dao = new DestroyStatusDao(token, id);
            try {
                return dao.destroy();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return false;
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            if (this.e != null) {
                Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                adapter.removeItem(positon);
            }
        }
    }
}