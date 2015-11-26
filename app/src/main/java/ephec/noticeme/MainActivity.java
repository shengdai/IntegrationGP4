package ephec.noticeme;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.gcm.Task;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

//TODO BUG QUAND ON CHANGE DE USER

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{

    private final String PROX_ALERT_INTENT = "ephec.noticeme";
    private static final long MINIMUM_TIME_BETWEEN_UPDATE = 15000 ;//en millisecondes
    private static final float MINIMUM_DISTANCECHANGE_FOR_UPDATE = 50;
    public static FragmentManager fragmentManager;
    private MenuItem itemMenu;
    private Toolbar toolbar;
    private float radius = 50;
    private static ArrayList<Alarm> LAlarm ;
    private LocationManager locManager;
    private RemoveTask mAuthTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Memo List");
        LAlarm = new ArrayList<>();

        locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE, new myLocationListener());
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MINIMUM_TIME_BETWEEN_UPDATE, MINIMUM_DISTANCECHANGE_FOR_UPDATE, new myLocationListener());

        Bundle extras = getIntent().getExtras();
        if(extras != null){
            if(extras.containsKey("Title")){
                launchMemoAlarms(extras.getString("Title"));
            }
        }

        TextView userTxtView = (TextView)findViewById(R.id.username);

        DBHelper db = new DBHelper(this.getApplicationContext());
        db.getReadableDatabase();
        User current = db.getCurrentUSer();
        db.close();
        userTxtView.setText(current.getMail());

        fragmentManager = getSupportFragmentManager();
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            MemoList firstFragment = new MemoList();

            // In case this activity was started with special instructions from an
            // Intent, pass the Intent's extras to the fragment as arguments
            firstFragment.setArguments(getIntent().getExtras());

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, firstFragment).commit();
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Button to add a memo", Snackbar.LENGTH_LONG)
                //      .setAction("Action", null).show();
                Intent intent = new Intent(view.getContext(), AddMemoActivity.class);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_list);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        String toolbarTitle = toolbar.getTitle().toString();
        itemMenu = menu.findItem(R.id.action_delete);
        if(!toolbarTitle.equals("Memo List")){
            itemMenu.setVisible(false);
        }else{
            itemMenu.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if(id == R.id.action_refresh){
            //TODO handle this button
            /*Intent intent = new Intent(this,LoginActivity.class);
            intent.putExtra("sync","sync");
            startActivity(intent);*/
            return true;
        }
        if(id == R.id.action_delete){
            DBHelper db = new DBHelper(this);
            db.getWritableDatabase();
            User usr = db.getCurrentUSer();
            mAuthTask = new RemoveTask(usr.getMail(),Connector.decrypt(Connector.decrypt(usr.getPassword())));
            mAuthTask.execute((Void)null);
            return true;
        }
        if(id == R.id.action_deco){
            DBHelper db1 = new DBHelper(this);
            db1.getReadableDatabase();
            User usr = db1.getCurrentUSer();
            db1.setCurrentToFalse(usr);
            db1.close();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment newFragment=null;

        if (id == R.id.nav_list) {
            newFragment = new MemoList();
            toolbar.setTitle("Memo List");
            itemMenu.setVisible(true);
        } else if (id == R.id.nav_add) {
            //newFragment = new AddMemo();
            Intent intent= new Intent(this, AddMemoActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_maps) {
            newFragment = new MapFragment();
            toolbar.setTitle("Memo on map");
            itemMenu.setVisible(false);
        } else if (id == R.id.nav_edit) {
            Intent intent = new Intent(this,EditProfile.class);
            startActivity(intent);
        }
        if (newFragment != null) {
            // Replace whatever is in the fragment_container view with this fragment,
            // and add the transaction to the back stack so the user can navigate back
            transaction.replace(R.id.fragment_container, newFragment);
            transaction.addToBackStack(null);

            // Commit the transaction
            transaction.commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void launchMemoAlarms(String title){
        DBHelper db = new DBHelper(this.getApplicationContext());
        db.getReadableDatabase();

        Alarm memo = db.getAlarm(title);
        db.close();
        if(!memo.getAlarmDate().equals("&")){
            setTimedAlert(memo);
        }
        setProximityAlert(memo);
    }

    @SuppressLint("NewApi")
    private void setTimedAlert(Alarm memo) {
        Intent intentAlarm = new Intent(this,AlarmReceiver.class);
        intentAlarm.putExtra("memoTitle", memo.getTitle());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, memo.getId()-(2*memo.getId()), intentAlarm, 0);

        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.setExact(AlarmManager.RTC_WAKEUP, getTime(memo), pendingIntent);
    }

    public long getTime(Alarm memo) {
        int year;
        int month;
        int day;
        int hour;
        int minute;

        String[] dueAlarm = memo.getAlarmDate().split("&");
        String[] date = dueAlarm[0].split("/");
        String[] hours = dueAlarm[1].split(":");

        year = Integer.parseInt(date[0]);
        month = Integer.parseInt(date[1])-1;
        day = Integer.parseInt(date[2]);

        hour = Integer.parseInt(hours[0]);
        minute = Integer.parseInt(hours[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(year, month, day, hour, minute, 0);

        return calendar.getTimeInMillis();
    }

    private void setProximityAlert(Alarm memo) {

        Intent intent = new Intent(this, GeoReceiver.class);

        intent.putExtra("memoTitle", memo.getTitle());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, memo.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);

        locManager.addProximityAlert(memo.getLatitude(), memo.getLongitude(), radius, -1, pendingIntent);
    }

    public static void addAlarm(Alarm alarm){
        LAlarm.add(alarm);
    }
    public static void removeAlarm(Alarm alarm){
        LAlarm.remove(alarm);
    }
    public static void clearList(){
        LAlarm = new ArrayList<>();
    }

    public class myLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public class myBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context c, Intent i) {

            Intent intentAlarm = new Intent(getApplicationContext(),AlarmService.class);
            intentAlarm.putExtra("memoTitle", i.getExtras().getString("memoTitle"));
            startActivity(intentAlarm);
        }
    }

    public void relaunchAlarms() {
        DBHelper db = new DBHelper(this.getApplicationContext());
        db.getReadableDatabase();

        ArrayList<Alarm> memos = db.getAllAlarm();
        int j = 0;
        while (memos.size() > j) {
            launchMemoAlarms(memos.get(j).getTitle());
            j++;
        }
        db.close();
    }

    public class RemoveTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        RemoveTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            DBHelper db = new DBHelper(MainActivity.this);
            db.getReadableDatabase();
            Iterator<Alarm> it = LAlarm.iterator();
            while(it.hasNext()){
                Connector co = new Connector();
                if(!co.connect("http://ephecnoticeme.me/app.php/android/removememo")) return false;
                Alarm temp = it.next();
                String response = co.delMemo(mEmail,mPassword,temp.getId());
                if(response.equals("ERROR")) return false;
                if(response.equals("0")){
                    System.out.println("Error Login");
                    return false;
                }
                if(!co.disconnect()) return false;
                db.deleteAlarm(temp.getTitle());
            }
            db.close();
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                Fragment newFragment = new MemoList();
                //toolbar.setTitle("Memo List");
                itemMenu.setVisible(true);
                transaction.replace(R.id.fragment_container, newFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } else {
                Toast.makeText(MainActivity.this,"error during suppression",Toast.LENGTH_LONG).show();
            }
        }
    }
}
