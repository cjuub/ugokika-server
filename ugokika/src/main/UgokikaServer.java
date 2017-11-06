import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class UgokikaServer {
    public UgokikaServer(Config config) {

        while(true) {
            try {
                ServerSocket socket = new ServerSocket(config.listenPort);
                System.out.println("Matte imasu!");
                Socket client = socket.accept();

                System.out.println("Ohayou!");

                Reader reader = new InputStreamReader(client.getInputStream());

                long activatedTime = 0;
                boolean isActivated = false;
                while (true) {
                    client.setSoTimeout(2 * 60000);
                    int result = 0;

                    try {
                        result = getResult(reader);
                    } catch (SocketTimeoutException e) {
                        break;
                    }

                    if (result == 1) {
                        System.out.println("Turn on light");
                        sendToHue(config.hueIp, config.hueKey, true);
                        activatedTime = System.currentTimeMillis();
                        isActivated = true;
                    } else if (result == 2) {
                        System.out.println("Heartbeat received");
                    }

                    long currTime = System.currentTimeMillis();
                    while (isActivated && currTime < (activatedTime + config.timeActivated * 1000)) {
                        client.setSoTimeout(200);

                        try {
                            result = getResult(reader);
                        } catch (SocketTimeoutException e) {
                            result = 0;
                        }

                        if (result == 1) {
                            activatedTime = System.currentTimeMillis();
                            System.out.println("on");
                        } else {
                            result = 0;
                            System.out.println("off");
                        }

                        currTime = System.currentTimeMillis();
                    }

                    if (result != 2) {
                        System.out.println("Turn off light");
                        sendToHue(config.hueIp, config.hueKey, false);
                    }

                    isActivated = false;
                }
            } catch (IOException e) {
                System.out.println("Disconnected");
            }
        }
    }

    private int getResult(Reader reader) throws SocketTimeoutException, IOException {
        int result = 0;

        result = reader.read();
        result = Integer.valueOf(result - '0');

        return result;
    }

    public void sendToHue(String hueIp, String hueKey, boolean on) throws IOException {
        String postUrl = "http://" + hueIp + "/api/" + hueKey + "/groups/6/action";
        Gson gson = new Gson();
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPut post = new HttpPut(postUrl);

        Action action = new Action();
        action.setOn(on);
        StringEntity postingString = new StringEntity(gson.toJson(action));
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(post);
    }

    public static void main(String[] args) {
        Config config = new Config();

        File file = new File(System.getProperty("user.dir") + "/config");
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            config.hueIp = br.readLine();
            config.listenPort = Integer.parseInt(br.readLine());
            config.hueKey = br.readLine();
            config.timeActivated = Integer.parseInt(br.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }

        new UgokikaServer(config);
    }
}
