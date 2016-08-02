package io.github.sunsetsucks.iogame.network;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import io.github.sunsetsucks.iogame.R;
import io.github.sunsetsucks.iogame.Util;

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
public class DeviceListFragment extends ListFragment implements PeerListListener
{
    private List<WifiP2pDevice> peers = new ArrayList<>();
    ProgressDialog progressDialog = null;
    View contentView = null;
    private WifiP2pDevice device;
    Button connectButton = null;

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        this.setListAdapter(new WiFiPeerListAdapter(R.layout.row_devices, peers));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        contentView = inflater.inflate(R.layout.device_list, null);
        connectButton = (Button) contentView.findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(getListView().getCheckedItemCount() > 0)
                {
                    SparseBooleanArray positions = getListView().getCheckedItemPositions();
                    for(int i = 0; i < getListView().getCount(); i++)
                    {
                        if(positions.get(i))
                        {
                            WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(i);
                            ((DeviceActionListener) getActivity()).itemSelected(device);
                        }
                    }
                }
                else
                {
                    Util.alert("Oops!", "You must select at least one device to connect to!", null);
                }
            }
        });
        return contentView;
    }

    /**
     * @return this device
     */
    public WifiP2pDevice getDevice()
    {
        return device;
    }

    private static String getDeviceStatus(int deviceStatus)
    {
        Log.d("wifidirect", "Peer status :" + deviceStatus);
        switch (deviceStatus)
        {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";

        }
    }

    /**
     * Initiate a connection with the peer.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
//        WifiP2pDevice device = (WifiP2pDevice) getListAdapter().getItem(position);
//        ((DeviceActionListener) getActivity()).itemSelected(device);
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private class WiFiPeerListAdapter extends ArrayAdapter<WifiP2pDevice>
    {
        private List<WifiP2pDevice> items;

        public WiFiPeerListAdapter(int textViewResourceId, List<WifiP2pDevice> objects)
        {
            super(Util.context, textViewResourceId, objects);
            items = objects;

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View v = convertView;
            if (v == null)
            {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.row_devices, null);
            }
            WifiP2pDevice device = items.get(position);
            if (device != null)
            {
                TextView top = (TextView) v.findViewById(R.id.device_name);
                TextView bottom = (TextView) v.findViewById(R.id.device_details);
                if (top != null)
                {
                    top.setText(device.deviceName);
                }
                if (bottom != null)
                {
                    bottom.setText(getDeviceStatus(device.status));
                }
            }

            return v;

        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    public void updateThisDevice(WifiP2pDevice device)
    {
        this.device = device;
        TextView view = (TextView) contentView.findViewById(R.id.my_name);
        view.setText(device.deviceName);
        view = (TextView) contentView.findViewById(R.id.my_status);
        view.setText(getDeviceStatus(device.status));
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList)
    {
        if (progressDialog != null && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0)
        {
            Log.d("wifidirect", "No devices found");
        }
    }

    public void clearPeers()
    {
        peers.clear();
        ((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     *
     */
    public void onInitiateDiscovery()
    {
        if (progressDialog != null && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
        progressDialog = ProgressDialog.show(getActivity(), "Press back to cancel", "finding peers", true,
                true, new DialogInterface.OnCancelListener()
                {

                    @Override
                    public void onCancel(DialogInterface dialog)
                    {

                    }
                });
    }

    public interface DeviceActionListener
    {

        void itemSelected(WifiP2pDevice device);

        void cancelDisconnect();

        void connect(WifiP2pConfig config);

        void disconnect();
    }

}