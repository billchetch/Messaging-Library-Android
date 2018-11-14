package com.bulan_baru.surf_forecast_data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Singleton
public class SurfForecastServiceManager {
    private HashMap<String, SurfForecastService> services = new HashMap<>();
    private HashMap<String, OkHttpClient> clients = new HashMap<>();
    private String apiBaseURL = null;

    @Inject
    SurfForecastServiceManager(){

    }

    public void setApiBaseURL(String apiBaseURL){
        if(apiBaseURL != null) {
            this.apiBaseURL = apiBaseURL.toLowerCase();
        } else {
            //TODO: throw exception
        }
    }
    public String getApiBaseURL(){ return apiBaseURL; }

    public void cancelAllCalls(){
        OkHttpClient client = clients.get(apiBaseURL);
        if(client != null){
            client.dispatcher().cancelAll();
        }
    }

    public SurfForecastService getService(){
        if(apiBaseURL == null)throw new Error("API Base URL cannot be NULL");

        SurfForecastService service = services.get(apiBaseURL);
        if(service == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            httpClient.addInterceptor(chain -> {
                Request original = chain.request();
                //String noCache = Long.toString(System.currentTimeMillis());

                Request request = original.newBuilder()
                        .header("User-Agent", SurfForecastService.USER_AGENT)
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            });


            OkHttpClient client = httpClient.build();

            Gson gson = new GsonBuilder()
                    .setDateFormat(SurfForecastService.DATE_FORMAT)
                    .create();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(apiBaseURL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();

            service = retrofit.create(SurfForecastService.class);

            services.put(apiBaseURL, service);
            clients.put(apiBaseURL, client);
        }
        return service;
    }
}
