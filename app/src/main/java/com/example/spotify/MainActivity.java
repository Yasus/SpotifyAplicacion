package com.example.spotify;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.spotify.Constants;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.ContentApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.types.Capabilities;
import com.spotify.protocol.types.Item;
import com.spotify.protocol.types.ListItem;
import com.spotify.protocol.types.ListItems;

import java.util.ArrayList;

import static com.spotify.android.appremote.api.ContentApi.ContentType.DEFAULT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Aplicación";

    private SpotifyAppRemote mSpotifyAppRemote;
    private String listaReproduccion="";
    private String textoListaReproduccion="";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.play_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connected();

            }
        });

        Button pause = findViewById(R.id.pause_button);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSpotifyAppRemote.getPlayerApi().pause();

            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        performAuthorization();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        disconnect();
    }

    private void performAuthorization() {
        Log.d(TAG, "Performing authorization...");

        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(Constants.CLIENT_ID)
                        .setRedirectUri(Constants.REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.CONNECTOR.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d(TAG, "Conectado!");

                        // Now you can start interacting with App Remote
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e(TAG, throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void disconnect() {
        if (mSpotifyAppRemote != null) {
            SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);
        }
    }

    private void playUri(String uri) {
        mSpotifyAppRemote
                .getPlayerApi()
                .play(uri);

    }

    private void connected() {
        CallResult<Capabilities> capabilities = mSpotifyAppRemote.getUserApi().getCapabilities().setResultCallback(new CallResult.ResultCallback<Capabilities>() {
            @Override
            public void onResult(Capabilities capabilities) {
                Log.d(TAG, "Premium user : " + capabilities.canPlayOnDemand);
            }
        });
        getRecommendedContentItems(null);
        //TODO: Lógica de espera para pider reproducir el cresultado del callback
        /*synchronized (listaReproduccion) {
            // lógica para sincronziar el callback
            try {
                listaReproduccion.wait();
                Log.d(TAG, "Uri de reproducción : " + listaReproduccion);
                mSpotifyAppRemote.getPlayerApi().play(listaReproduccion);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } */


    }
    // Default  obtiene lista por dfecto y la lista de reproducción del usuario
    public void getRecommendedContentItems(View view){ mSpotifyAppRemote.getContentApi().getRecommendedContentItems(ContentApi.ContentType.DEFAULT).setResultCallback(new CallResult.ResultCallback() {
        @Override
        public void onResult(Object o) {
            ListItems listItems = (ListItems) o;
            Log.i("MainActivity", listItems.toString());
            // En este caso se obtiene el primero, se puede obtener el que se necesite
            getChildrenOfItem(listItems.items[0]);

        }


    });
    }

    public void getChildrenOfItem(ListItem listItem) {
        mSpotifyAppRemote.getContentApi().getChildrenOfItem(listItem, 0, 0).setResultCallback(new CallResult.ResultCallback() {
            @Override
            public void onResult(Object o) {
                ListItems listItems = (ListItems) o;
                Log.i("MainActivity", "Play List=" + listItems.toString());
                // Seleccionamos la lista de reproducción

                // Recorremos una lista de reproducción que contiene Queen
                for(int i = 0; i < listItems.total; i++) {
                    if(listItems.items[i].playable && !listItems.items[i].uri.isEmpty() && listItems.items[i].title.contains("Queen")) {
                        listaReproduccion = listItems.items[i].uri;
                        textoListaReproduccion = listItems.items[i].title;
                        Log.i("Play List", "Title=" + listItems.items[i].title );
                        break;

                    }
                }
                Log.d(TAG, "Uri de reproducción : " + listaReproduccion);
                EditText edtText = (EditText) findViewById(R.id.ListaUsuarioID);
                edtText.setText(textoListaReproduccion);
                mSpotifyAppRemote.getPlayerApi().play(listaReproduccion);
                synchronized (listaReproduccion) {
                    listaReproduccion.notifyAll();
                }
            }
        });
    }
}
