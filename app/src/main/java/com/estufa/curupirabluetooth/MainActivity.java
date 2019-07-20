package com.estufa.curupirabluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button B_CONECTAR, B_LED1, B_LED2, B_LED3;

    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;
    private static final int MESSAGE_READ = 3;
    ConnectedThread connectedThread;

    Handler mHandler;
    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;

    private static String MAC = null;
    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        B_CONECTAR = (Button)findViewById(R.id.B_CONECTAR);
        B_LED1 = (Button)findViewById(R.id.B_LED1);
        B_LED2 = (Button)findViewById(R.id.B_LED2);
        B_LED3 = (Button)findViewById(R.id.B_LED3);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (meuBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(),"Seu dispositivo não possui Bluetooth.", Toast.LENGTH_LONG).show();
        }else if(!meuBluetoothAdapter.isEnabled()){
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
        }

        B_CONECTAR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao){
                   try{
                       meuSocket.close();
                       Toast.makeText(getApplicationContext(),"Bluetooth DESCONECTADO.", Toast.LENGTH_LONG).show();
                       conexao = false;
                       B_CONECTAR.setText("CONECTAR");
                   }catch (IOException erro){
                       Toast.makeText(getApplicationContext(),"Ocorreu um erro: " + erro, Toast.LENGTH_LONG).show();
                   }
                }else{
                    //conectar
                    Intent abreLista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abreLista, SOLICITA_CONEXAO);

                }
            }
        });

        B_LED1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao){
                    connectedThread.enviar("led1");
                }else {
                    Toast.makeText(getApplicationContext(),"Bluetooth não está conectado", Toast.LENGTH_LONG).show();
                }

            }
        });
        B_LED2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao){
                    connectedThread.enviar("led2");
                }else {
                    Toast.makeText(getApplicationContext(),"Bluetooth não está conectado", Toast.LENGTH_LONG).show();
                }

            }
        });
        B_LED3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (conexao){
                    connectedThread.enviar("led3");
                }else {
                    Toast.makeText(getApplicationContext(),"Bluetooth não está conectado", Toast.LENGTH_LONG).show();
                }

            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                if(msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;

                    dadosBluetooth.append(recebidos);

                    int fimInformacao = dadosBluetooth.indexOf("}");

                    if(fimInformacao >= 0){
                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);
                        int tamInformacao = dadosCompletos.length();
                        if (dadosBluetooth.charAt(0) == '{'){
                            String dadosFinais = dadosBluetooth.substring(1, tamInformacao);
                            Log.d("Recebidos", "DADOS RECEBIDOS: " + dadosFinais);
                            Toast.makeText(getApplicationContext(), "Status Leds: " + dadosFinais, Toast.LENGTH_LONG).show();
                            if(dadosFinais.indexOf("l1on")>=0){
                                B_LED1.setBackgroundColor(Color.GREEN);
                            }else{
                                B_LED1.setBackgroundColor(Color.RED);
                            }
                            if(dadosFinais.indexOf("l2on")>=0){
                                B_LED2.setBackgroundColor(Color.GREEN);
                            }else{
                                B_LED2.setBackgroundColor(Color.RED);
                            }
                            if(dadosFinais.indexOf("l3on")>=0){
                                B_LED3.setBackgroundColor(Color.GREEN);
                            }else{
                                B_LED3.setBackgroundColor(Color.RED);
                            }
                        }
                        dadosBluetooth.delete(0, dadosBluetooth.length());
                    }
                }
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(),"O Bluetooth foi ATIVADO.", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(),"O Bluetooth NÃO foi ativado, o app será encerrado.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){
                    MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(getApplicationContext(), "MAC final: " + MAC, Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);
                    try{
                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);
                        meuSocket.connect();
                        conexao = true;

                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();
                        B_CONECTAR.setText("DESCONECTAR");
                        Toast.makeText(getApplicationContext(), "Você foi conectado com: " + MAC, Toast.LENGTH_LONG).show();
                    } catch (IOException erro){
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro: " + erro, Toast.LENGTH_LONG).show();
                        conexao = false;
                        B_CONECTAR.setText("CONECTAR");
                    }

                }else {
                    Toast.makeText(getApplicationContext(), "Falha ao obter o MAC", Toast.LENGTH_LONG).show();
                }
        }
    }


    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {

            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {

            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            mmBuffer = new byte[1024];
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    String dadosBt = new String(mmBuffer, 0, numBytes);

                    // Send the obtained bytes to the UI activity.
                    mHandler.obtainMessage(MESSAGE_READ, numBytes, -1, dadosBt).sendToTarget();

                } catch (IOException e) {
                //    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device.
        private void enviar(String dadosEnviar) {
            try {
                byte[] msgBuffer = dadosEnviar.getBytes();
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Falha ao enviar o dado ", Toast.LENGTH_LONG).show();
            }
        }
    }
}
