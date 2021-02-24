package com.nis.modules.sys.shiro;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CustomSSLSocketFactory extends SSLSocketFactory{
    private SSLSocketFactory socketFactory;
    public CustomSSLSocketFactory()
    {
        try {
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, new TrustManager[]{ new DummyTrustManager()}, new SecureRandom());
            socketFactory = ctx.getSocketFactory();
            System.setProperty("https.protocols", "TLSv1.2,TLSv1.1,SSLv3");
        } catch ( Exception ex ){ ex.printStackTrace(System.err);  }
    }
    public static SocketFactory getDefault(){
        return new CustomSSLSocketFactory();
    }
    @Override
    public String[] getDefaultCipherSuites()
    {
        return socketFactory.getDefaultCipherSuites();
    }
    @Override
    public String[] getSupportedCipherSuites()
    {
        return socketFactory.getSupportedCipherSuites();
    }
    @Override
    public Socket createSocket(Socket socket, String string, int num, boolean bool) throws IOException
    {
        return socketFactory.createSocket(socket, string, num, bool);
    }
    @Override
    public Socket createSocket(String string, int num) throws IOException, UnknownHostException
    {
        return socketFactory.createSocket(string, num);
    }
    @Override
    public Socket createSocket(String string, int num, InetAddress netAdd, int i) throws IOException, UnknownHostException
    {
        return socketFactory.createSocket(string, num, netAdd, i);
    }
    @Override
    public Socket createSocket(InetAddress netAdd, int num) throws IOException
    {
        return socketFactory.createSocket(netAdd, num);
    }
    @Override
    public Socket createSocket(InetAddress netAdd1, int num, InetAddress netAdd2, int i) throws IOException
    {
        return socketFactory.createSocket(netAdd1, num, netAdd2, i);
    }


    /**
     * 证书
     */
    public static class DummyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] cert, String string) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] cert, String string) throws CertificateException {
        }
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }
    }
}

