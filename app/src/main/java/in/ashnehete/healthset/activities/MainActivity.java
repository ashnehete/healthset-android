package in.ashnehete.healthset.activities;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import in.ashnehete.healthset.R;
import in.ashnehete.healthset.fragments.DevicesFragment;
import in.ashnehete.healthset.fragments.NotificationsFragment;
import in.ashnehete.healthset.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity
        implements DevicesFragment.OnFragmentInteractionListener,
        ProfileFragment.OnFragmentInteractionListener,
        NotificationsFragment.OnFragmentInteractionListener {

    private static final int REQUEST_ENABLE_BT = 10;
    private static final String TAG = "MainActivity";

    @BindView(R.id.navigation)
    BottomNavigationView navigation;

    ProfileFragment profileFragment;
    DevicesFragment devicesFragment;
    NotificationsFragment notificationsFragment;
    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        profileFragment = ProfileFragment.newInstance("a", "b");
        devicesFragment = DevicesFragment.newInstance("a", "b");
        notificationsFragment = NotificationsFragment.newInstance("a", "b");

        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                switch (item.getItemId()) {
                    case R.id.navigation_profile:
                        transaction.replace(R.id.fragment_container, profileFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        return true;

                    case R.id.navigation_devices:
                        transaction.replace(R.id.fragment_container, devicesFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        return true;

                    case R.id.navigation_notifications:
                        transaction.replace(R.id.fragment_container, notificationsFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                        return true;
                }
                return false;
            }
        });
        navigation.setSelectedItemId(R.id.navigation_devices);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        return;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(TAG, "onStart enabled");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Bluetooth Enable
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth Enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth Enable Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
