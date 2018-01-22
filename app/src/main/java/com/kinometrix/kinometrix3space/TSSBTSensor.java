package com.kinometrix.kinometrix3space;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

public class TSSBTSensor{
    private BluetoothSocket btSocket = null;
    private OutputStream BTOutStream = null;
    private InputStream BTInStream = null;
    private ReentrantLock call_lock;

    private static TSSBTSensor instance;

    protected TSSBTSensor() throws Exception{
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        UUID MY_UUID =
                UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        String server_mac = null;

        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        // If there are paired devices, set the device mac address string as needed
        //(for now we assume the only paired device is the 3-Space sensor)
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if(device.getName().contains("YEI_3SpaceBT"))
                {
                    server_mac = device.getAddress();
                    break;
                }
            }
        }

        if (server_mac != null)
        {
            //Get a reference to the remote device
            BluetoothDevice remote_device = mBluetoothAdapter.getRemoteDevice(server_mac);
            //Create a socket
            btSocket = remote_device.createRfcommSocketToServiceRecord(MY_UUID);
            //Stop discovery if it is enabled
            mBluetoothAdapter.cancelDiscovery();
            //Try to connect to the remote device.
            btSocket.connect();
            //Now lets create the in/out streams
            BTOutStream = btSocket.getOutputStream();
            BTInStream = btSocket.getInputStream();
            call_lock = new ReentrantLock();
        }
        else
        {
            throw new Exception();
        }
    }

    public static TSSBTSensor getInstance() throws Exception
    {
        if(instance == null)
        {
            instance = new TSSBTSensor();
        }
        return instance;
    }

    public byte createChecksum(byte[] data)
    {
        byte checksum = 0;
        for(int i = 0; i < data.length; i++)
        {
            checksum += data[i] % 256;
        }
        return checksum;
    }

    public byte createChecksum(Byte[] data)
    {
        byte checksum = 0;
        for(int i = 0; i < data.length; i++)
        {
            checksum += data[i] % 256;
        }
        return checksum;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public void write(byte[] data)
    {
        byte[] msgBuffer = new byte[data.length + 2];
        System.arraycopy(data, 0, msgBuffer, 1, data.length);
        msgBuffer[0] = (byte)0xf7;
        msgBuffer[data.length + 1] = createChecksum(data);
        //        Log.d("TSSBTSensor", "length of outgoing packet is: "+msgBuffer.length);
        Log.d("TSSBTSensor", "msgBuffer: "+bytesToHex(msgBuffer));

        try {
            BTOutStream.write(msgBuffer);
            BTOutStream.flush();
            msgBuffer = null;
        }
        catch (IOException e) {
            Log.d("TSSBTSensor:write()", "error with write?");
        }
    }

    public byte[] read(int amnt)
    {
        byte[] response = new byte[amnt];
        int amnt_read = 0;
        while (amnt_read < amnt)
        {

            try {
                amnt_read += BTInStream.read(response, amnt_read, amnt - amnt_read);
            }
            catch (IOException e) {
                Log.d("TSSBTSensor:read()", "IOException e: "+e.getMessage());
            }

        }
        return response;
    }

    public void close()
    {
        //We are done, so lets close the connection
        try {
            btSocket.close();
        }
        catch (IOException e) {
            Log.d("TSSBTSensor:close()", "error with close?");
        }
    }

    public float[] binToFloat(byte[] b)
    {
        if (b.length % 4 != 0)
        {
            return new float[0];
        }
        float[] return_array = new float[b.length / 4];
        for (int i = 0; i < b.length; i += 4)
        {
            //We account for endieness here
            int asInt = (b[i + 3] & 0xFF)
                    | ((b[i + 2] & 0xFF) << 8)
                    | ((b[i + 1] & 0xFF) << 16)
                    | ((b[i] & 0xFF) << 24);

            return_array[i / 4] = Float.intBitsToFloat(asInt);
        }
        return return_array;
    }

    public int[] binToInt(byte[] b)
    {
        if (b.length % 4 != 0)
        {
            return new int[0];
        }
        int[] return_array = new int[b.length / 4];
        for (int i = 0; i < b.length; i += 4)
        {
            //We account for endieness here
            return_array[i / 4] = (b[i + 3] & 0xFF)
                    | ((b[i + 2] & 0xFF) << 8)
                    | ((b[i + 1] & 0xFF) << 16)
                    | ((b[i] & 0xFF) << 24);

        }
        return return_array;
    }

    public short[] binToShort(byte[] b)
    {
        if (b.length % 2 != 0)
        {
            return new short[0];
        }
        short[] return_array = new short[b.length / 2];
        for (int i = 0; i < b.length; i += 2)
        {
            //We account for endieness here
            return_array[i / 2] = (short)((b[i + 1] & 0xFF)
                    | ((b[i] & 0xFF) << 8));

        }
        return return_array;
    }

    public byte[] floatToBin(float[] f)
    {
        ByteBuffer byteBuf = ByteBuffer.allocate(4 * f.length);
        FloatBuffer floatBuf = byteBuf.asFloatBuffer();
        floatBuf.put(f);
        return byteBuf.array();
    }

    public void setLEDColor(float red, float green, float blue)
    {
        call_lock.lock();
        byte[] float_data = floatToBin(new float[]{red,green,blue});
        byte[] send_data = new byte[]{(byte)0xee, float_data[0], float_data[1], float_data[2], float_data[3],
                float_data[4], float_data[5], float_data[6], float_data[7],
                float_data[8], float_data[9], float_data[10], float_data[11]};
        write(send_data);
        call_lock.unlock();
    }

    public float[] getLEDColor()
    {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0xef};
        write(send_data);
        byte[] float_data = read(12);
        call_lock.unlock();
        return binToFloat(float_data);
    }



    public void setTareCurrentOrient()
    {
        Log.d("TSSBTSensor","setTareCurrentOrient");
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x60};
        write(send_data);
        call_lock.unlock();
    }

    public void setAxisDirections(String axis_order, boolean neg_x, boolean neg_y, boolean neg_z)
    {
        byte val = 0;
        if(axis_order.compareTo("XYZ") == 0)
        {
            val = 0x0;
        }
        else if(axis_order.compareTo("XZY") == 0)
        {
            val = 0x1;
        }
        else if(axis_order.compareTo("YXZ") == 0)
        {
            val = 0x2;
        }
        else if(axis_order.compareTo("YZX") == 0)
        {
            val = 0x3;
        }
        else if(axis_order.compareTo("ZXY") == 0)
        {
            val = 0x4;
        }
        else if (axis_order.compareTo("ZYX") == 0)
        {
            val = 0x5;
        }
        else
        {
            return;
        }
        if (neg_x)
        {
            val = (byte)(val | 0x20);
        }
        if (neg_y)
        {
            val = (byte)(val | 0x10);
        }
        if (neg_z)
        {
            val = (byte)(val | 0x8);
        }
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x74, val};
        write(send_data);
        call_lock.unlock();
    }

    public AxisDirectionStruct getAxisDirections()
    {
        AxisDirectionStruct axis_dir = new AxisDirectionStruct();
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x8f};
        write(send_data);
        byte[] response = read(1);
        call_lock.unlock();
        //Determine the axis order
        int axis_order_num = response[0] & 7;
        if(axis_order_num == 0)
        {
            axis_dir.axis_order = "XYZ";
        }
        else if(axis_order_num == 1)
        {
            axis_dir.axis_order = "XZY";
        }
        else if(axis_order_num == 2)
        {
            axis_dir.axis_order = "YXZ";
        }
        else if(axis_order_num == 3)
        {
            axis_dir.axis_order = "YZX";
        }
        else if(axis_order_num == 4)
        {
            axis_dir.axis_order = "ZXY";
        }
        else if(axis_order_num == 5)
        {
            axis_dir.axis_order = "ZYX";
        }
        //Determine if any axes are negated
        if((response[0] & 0x20) > 0)
        {
            axis_dir.neg_x = true;
        }
        if((response[0] & 0x10) > 0)
        {
            axis_dir.neg_y = true;
        }
        if((response[0] & 0x8) > 0)
        {
            axis_dir.neg_z = true;
        }
        return axis_dir;
    }

    public String getSoftwareVersion()
    {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0xdf};
        write(send_data);
        byte[] response = read(12);
        call_lock.unlock();
        return new String(response);
    }

    public String getHardwareVersion()
    {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0xe6};
        write(send_data);
        byte[] response = read(32);
        call_lock.unlock();
        return new String(response);
    }

    public int getSerialNumber()
    {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0xed};
        write(send_data);
        byte[] response = read(4);
        call_lock.unlock();
        return binToInt(response)[0];
    }

    public void getButtonState()
    {
        call_lock.lock();
        //I also have to figure out how I want to
        //do this function
        call_lock.unlock();
    }

    public void setLEDMode(int mode)
    {
        call_lock.lock();
        byte[] send_data =new byte[]{(byte)0xc4, (byte)mode};
        write(send_data);
        call_lock.unlock();
    }

    public void setFilterMode(int mode)
    {
		/*Changingthisparametercan be useful for tuning filter-performance versus orientation-update rates.
    	Passing in a parameter of 0 places the sensor into IMU mode,
    	a 1 places the sensor into Kalman Filtered Mode (Default mode),
    	a 2 places the sensor into Alternating Kalman Filter Mode,
    	a 3 places the sensor into Complementary Filter Mode and
    	a 4 places the sensor into Quaternion Gradient Descent Filter Mode.
		 */
        Log.d("TSSBTSensor","setFilterMode");
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x7b, (byte)mode};
        write(send_data);
        call_lock.unlock();
    }

    public int getBatteryStatus()
    {
        call_lock.lock();
        byte[] send_data =new byte[]{(byte)0xcb};
        write(send_data);
        byte[] response = read(1);
        call_lock.unlock();
        return (int)response[0];
    }

    public int getBatteryLife()
    {
        call_lock.lock();
        byte[] send_data =new byte[]{(byte)0xca};
        write(send_data);
        byte[] response = read(2);
        call_lock.unlock();
        return (int)binToShort(response)[0];
    }

	/*   public float[] getFiltTaredOrientQuat()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x00};
    	write(send_data);
    	byte[] response = read(16);
    	call_lock.unlock();
    	return binToFloat(response);
    }
	 */


	/*   public float[] getFiltTaredOrientRot()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x02};
    	write(send_data);
    	byte[] response = read(36);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    public float[] getFiltTaredOrientAxis()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x03};
    	write(send_data);
    	byte[] response = read(16);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    public float[] getFiltTaredOrientTwoVec()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x04};
    	write(send_data);
    	byte[] response = read(24);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    public float[] differenceQuat()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x05};
    	write(send_data);
    	byte[] response = read(16);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    public float[] getNormalComponentData()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x20};
    	write(send_data);
    	byte[] response = read(36);
    	call_lock.unlock();
    	return binToFloat(response);
    }
    public float[] getNormalGyroData()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x21};
    	write(send_data);
    	byte[] response = read(12);
    	call_lock.unlock();
    	return binToFloat(response);
    }
	 */
    //    public Float[] getNormalAccelData()
    //    {
    //    	call_lock.lock();
    //    	byte[] send_data = new byte[]{(byte)0x22};
    //    	write(send_data);
    //    	byte[] response = read(12);
    //    	call_lock.unlock();
    //    	return binToFloat(response);
    //    }
	/*
    public float[] getCorrectedComponentData()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x25};
    	write(send_data);
    	byte[] response = read(36);
    	call_lock.unlock();
    	return binToFloat(response);
    }
	 */


    public float[] getEulerAngles()
    {
        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x01};
        write(send_data);
        byte[] response = read(12);
        call_lock.unlock();
        return binToFloat(response);
    }

    public float[] getLinAcc()
    {

        call_lock.lock();
        byte[] send_data = new byte[]{(byte)0x29};
        write(send_data);
        byte[] response = read(12);
        call_lock.unlock();
        return binToFloat(response);
    }
	/*
    public float[] getRawComponentData()
    {
    	call_lock.lock();
    	byte[] send_data = new byte[]{(byte)0x40};
    	write(send_data);
    	byte[] response = read(36);
    	call_lock.unlock();
    	return binToFloat(response);
    }
	 */

    public static byte TSS_GET_TARED_ORIENTATION_AS_QUATERNION = 0x00;
    public static byte TSS_GET_CORRECTED_LINEAR_ACCELERATION_IN_GLOBAL_SPACE = 0x29;
    public static byte TSS_GET_CORRECTED_COMPASS = (byte)0x28; //really 28 ML
    public static byte TSS_GET_CORRECTED_VALUES = 0x25;
    public static byte TSS_GET_RAW_VALUES = 0x40;
    public static byte TSS_NULL = (byte) 0xff;
    public static byte TSS_SET_STREAMING_SLOTS = 0x50;
    public static byte TSS_SET_STREAMING_TIMING = 0x52;
    public static byte TSS_START_STREAMING = 0x55;
    public static byte TSS_STOP_STREAMING = 0x56;

    public static byte TSS_GET_NORMAL_GYRO = 0x21;
    public static byte TSS_GET_EULERANGLES = 0x01;
    public static byte TSS_GET_NORMAL_ACCEL = 0x22;


    public static int TSS_QUAT_LEN = 16;
    public static int TSS_LIN_ACC_LEN = 12;
    public static int TSS_COMPASS_LEN = 12;
    public static int TSS_CORRECTED_LEN = 36;
    public static int TSS_RAW_LEN = 36;
    public static int TSS_NORMAL_GYRO_LEN = 12;
    public static int TSS_EULERANGLES_LEN = 12;
    public static int TSS_NORMAL_ACCEL = 12;

    public void setupStreaming() {
        //# There are 8 streaming slots available for use, and each one can hold one of the streamable commands.
        //# Unused slots should be filled with 0xff so that they will output nothing.
        call_lock.lock();

        //byte[] send_data = {TSS_SET_STREAMING_SLOTS, TSS_GET_TARED_ORIENTATION_AS_QUATERNION, TSS_GET_CORRECTED_LINEAR_ACCELERATION_IN_GLOBAL_SPACE,
        //        TSS_GET_CORRECTED_VALUES, TSS_GET_RAW_VALUES, TSS_NULL, TSS_NULL, TSS_NULL, TSS_NULL};

        byte[] send_data = {TSS_SET_STREAMING_SLOTS, TSS_GET_TARED_ORIENTATION_AS_QUATERNION, TSS_GET_NORMAL_GYRO,
                TSS_GET_EULERANGLES, TSS_GET_NORMAL_ACCEL, TSS_NULL, TSS_NULL, TSS_NULL, TSS_NULL};
        Log.d("TSSBTSensor","setupStreaming send_data");
        write(send_data);
        call_lock.unlock();

        call_lock.lock();
        //Interval determines how often the streaming session will output data from the requested commands
        //An interval of 0 will output data at the max filter rate
        byte interval = 0; //0xc350 = 50000 microseconds

        // Duration determines how long the streaming session will run for
        // A duration of 0xffffffff will have the streaming session run till the stop stream command is called
        byte duration = 0xffffffff;


        // Delay determines how long the sensor should wait after a start command is issued to actually begin
        // streaming
        int delay = 500000; // microseconds

        //byte [] timing = {0x52,0x00, 0x00,0x00, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x00, 0x07, (byte)0xa1, 0x20};
        byte [] timing = {0x52,0x00, 0x00,0x00, 0x00, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x00, 0x07, (byte)0xa1, 0x20};
        write(timing);

        call_lock.unlock();
    }

    public void startStreaming(){
        call_lock.lock();
        byte[] send_data ={TSS_START_STREAMING};
        Log.d("TSSBTSensor","startStreaming");
        write(send_data);
        call_lock.unlock();
    }

    public void stopStreaming(){
        call_lock.lock();
        byte[] send_data = {TSS_STOP_STREAMING};
        Log.d("TSSBTSensor","stopStreaming");
        write(send_data);
        clearInputStream();
        call_lock.unlock();
    }

    private void clearInputStream(){
		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}*/

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @SuppressLint("LongLogTag")
            public void run() {

                try {
                    long skipped = BTInStream.skip(10000);
                    Log.d("TSSBTSensor:clearInputStream()", "skipped "+skipped+" bytes");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Log.d("TSSBTSensor:clearInputStream()", "error with skip?");
                    e.printStackTrace();
                }


            }
        }, 1000);

    }
}