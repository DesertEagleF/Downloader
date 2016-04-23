package com.deserteaglefe.eighthweek;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // 常量
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String DOWNLOAD_PATH = "download";

    // 控件
    private EditText mEditText;
    private Button mDownloadButton;
    private ListView mListView;
    private List<DownloadTask> mDownloadTasks = new ArrayList<>();
    private TaskAdapter mTaskAdapter;

    // 受保护的文件名
    private HashSet<String> hashSet = new HashSet<>();

    // Toast内容常量
    private static final String[] TOAST_TEXT = new String[]{
            "下载成功",                    // 0: download success
            "非法URL",                     // 1: MalformedURLException
            "文件不存在或URL输入错误",     // 2: FileNotFoundException
            "无法解析的主机名，请检查URL", // 3: UnknownHostException
            "IO错误，请检查存储设备",      // 4: IOException
            "同名文件下载中，请稍后重试",  // 5: Duplicate
            "未知错误"                     // 6: Unknown
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("下载器");
        findViews();
        setListeners();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.download_button:
                // TODO: Start download
                if(mDownloadTasks.size() == 0){
                    mDownloadTasks.add((DownloadTask) new DownloadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
                    mTaskAdapter = new TaskAdapter(MainActivity.this, mDownloadTasks);
                    mListView.setAdapter(mTaskAdapter);
                    mTaskAdapter.notifyDataSetChanged();
                }else{
                    mDownloadTasks.add((DownloadTask) new DownloadTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR));
                    refresh();
                }
                break;
        }
    }

    // 刷新视图
    private void refresh() {
        mTaskAdapter.refresh(mDownloadTasks);
    }

    private void findViews() {
        mEditText = (EditText) findViewById(R.id.edit_text);
        mDownloadButton = (Button) findViewById(R.id.download_button);
        mListView = (ListView) findViewById(R.id.list_view);
    }

    private void setListeners() {
        mDownloadButton.setOnClickListener(this);
    }

    // AsyncTask
    public class DownloadTask extends AsyncTask<Integer, Integer, Integer> {
        private String mUrlString;           // URL地址
        private String mDownloadFoldersName; // 文件夹名
        private String mName;                // 文件名
        private String mFileName;            // 完整的文件名
        private int mProgress;               // 进度
        private String mProgressText;        // 进度，用于显示进度（正常）或失败原因（异常）
        private int status;                  // 状态

        public DownloadTask(){
            Log.i(TAG, mEditText.getText().toString());
            mUrlString = mEditText.getText().toString();
            // 获取文件名
            int index = 0;
            for (int i = 0; i < mUrlString.length(); i++) {
                if (mUrlString.charAt(i) == '/') {
                    index = i;
                }
            }
            mName = mUrlString.substring(index + 1); // 文件名
            if(hashSet.contains(mName)){
                status = 5; // 保护正在下载中的文件
            }else {
                hashSet.add(mName);
                status = 0;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDownloadFoldersName = Environment.getExternalStorageDirectory() + File.separator + DOWNLOAD_PATH + File.separator; // 文件夹名
            mFileName = mDownloadFoldersName + mName; // 完整的文件名
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            if(status == 0){
                try {
                    URL url = new URL(mUrlString);
                    URLConnection urlConnection = url.openConnection();

                    InputStream inputStream = urlConnection.getInputStream();

                    int contentLength = urlConnection.getContentLength(); // 要下载的文件的大小
                    Log.i(TAG, "the download file's content length: " + contentLength);
                    if (contentLength <= 0) {
                        status = 2;
                        publishProgress(status);
                    } else {
                        File file = new File(mDownloadFoldersName);

                        if (!file.exists()) { // 如果不存在
                            file.mkdir();   // 创建
                        }

                        File apkFile = new File(mFileName);
                        if (apkFile.exists()) { // 如果存在
                            apkFile.delete(); // 删除
                        }

                        int downloadSize = 0;
                        byte[] bytes = new byte[1024];
                        int length;
                        OutputStream outputStream = new FileOutputStream(mFileName);
                        while ((length = (inputStream.read(bytes))) != -1) {
                            outputStream.write(bytes, 0, length);
                            downloadSize += length;
                            int progress = downloadSize * 100 / contentLength;
                            publishProgress(progress); // 会调用onProgressUpdate()来更新进度
                            //Log.i(TAG, "download progress: " + progress);
                        }
                        Log.i(TAG, "download success");
                        inputStream.close();
                        outputStream.close();
                        status = 0;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    status = 1;
                    publishProgress(status);
                    Log.i(TAG, "MalformedURLException");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    status = 2;
                    publishProgress(status);
                    Log.i(TAG, "FileNotFoundException");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    status = 3;
                    publishProgress(status);
                    Log.i(TAG, "UnknownHostException");
                } catch (IOException e) {
                    e.printStackTrace();
                    status = 4;
                    publishProgress(status);
                    Log.i(TAG, "IOException");
                }
            }else {
                publishProgress(status);
            }
            return status;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // publishProgress()时更新进度，在UI线程
            super.onProgressUpdate(values);
            mProgress = values[0];
            mProgressText = values[0] + "%";
            if(status != 0){
                mProgressText = TOAST_TEXT[status];
            }
            refresh();
        }

        @Override
        protected void onPostExecute(Integer status) {
            super.onPostExecute(status);
            if(status != 5){
                hashSet.remove(mName); // 下载完了的和出错了的文件不再受保护，已完成可以覆盖的要求
            }
            String toastString = mFileName + TOAST_TEXT[status];
            Toast.makeText(MainActivity.this, toastString, Toast.LENGTH_SHORT).show();
        }

        public String getName() {
            return mName;
        }

        public int getProgress() {
            return mProgress;
        }

        public String getProgressText() {
            return mProgressText;
        }

    }

    public class TaskAdapter extends BaseAdapter{

        private Context mContext;

        private LayoutInflater mLayoutInflater;

        private List<DownloadTask> mDownloadTasks = new ArrayList<>();

        public TaskAdapter(Context context, List<DownloadTask> downloadTasks) {
            mContext = context;
            mDownloadTasks = downloadTasks;
            mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mDownloadTasks.size();
        }

        @Override
        public Object getItem(int position) {
            return mDownloadTasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.item, null);
                viewHolder = new ViewHolder();
                // 获取控件
                viewHolder.mNameText = (TextView) convertView.findViewById(R.id.name_text);
                viewHolder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.progress_bar);
                viewHolder.mTextView = (TextView) convertView.findViewById(R.id.progress_text_view);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // 和数据之间进行绑定
            viewHolder.mNameText.setText(mDownloadTasks.get(position).getName());
            viewHolder.mProgressBar.setProgress(mDownloadTasks.get(position).getProgress());
            viewHolder.mTextView.setText(mDownloadTasks.get(position).getProgressText());

            return convertView;
        }

        public void refresh(List<DownloadTask> downloadTasks) {
            mDownloadTasks = downloadTasks;
            notifyDataSetChanged();
        }

        class ViewHolder {
            TextView mNameText;
            ProgressBar mProgressBar;
            TextView mTextView;
        }
    }
}


/*TODO:用AsyncTask来实现文件下载，要求：
*TODO:a)  可在文本框中输入请求路径，点击按钮开始下载
*TODO:b)  在界面上实时更新下载进度
*TODO:c)  如果文件已存在，则删除原文件再进行下载
*/