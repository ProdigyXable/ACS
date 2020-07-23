package cn.edu.pku.sei.plde.ACS.gatherer;

/**
 * Created by yjxxtd on 4/16/16.
 */
import java.io.IOException;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public class ThreadPoolHttpClientGithubUrlList {

    public ThreadPoolHttpClientGithubUrlList() {
    }

    public void fetch(List<String> urlList) {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

        cm.setMaxTotal(10);
        CloseableHttpClient httpclient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        try {
            GetThread[] getThreads = new GetThread[urlList.size()];
            for (int i = 0; i < urlList.size(); i++) {
                HttpGet get = new HttpGet(urlList.get(i));
                getThreads[i] = new GetThread(httpclient, get, i + 1);
            }

            for (GetThread gt : getThreads) {
                gt.start();
            }

            for (GetThread gt : getThreads) {
                gt.join();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class GetThread extends Thread {

        private final CloseableHttpClient httpClient;
        private final HttpContext context;
        private final HttpGet httpget;
        private final int id;

        public GetThread(CloseableHttpClient httpClient, HttpGet httpget, int id) {
            this.httpClient = httpClient;
            this.context = new BasicHttpContext();
            this.httpget = httpget;
            this.id = id;
        }

        /**
         * Executes the GetMethod and prints some status information.
         */
        @Override
        public void run() {
            try {
                //System.out.println(id + " - about to get something from " + httpget.getURI());
                CloseableHttpResponse response = httpClient.execute(httpget, context);
                try {
                    // get the response body as an array of bytes
                    HttpEntity entity = response.getEntity();
                    System.out.println("length " + entity.getContentLength());
//                    if (entity != null) {
//                        writeFile("experiment//searchcode//" + packageName + "//" + id + ".java", EntityUtils.toString(entity));
//                    }
                } finally {
                    response.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
