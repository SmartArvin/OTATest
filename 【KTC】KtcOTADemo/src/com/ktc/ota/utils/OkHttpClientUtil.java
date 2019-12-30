package com.ktc.ota.utils;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class OkHttpClientUtil{
	
	/**
	 * 解决Https请求安全证书异常问题
	 * @return OkHttpClient
	 */
	public static OkHttpClient getUnsafeOkHttpClient() {
		try {
			// Create a trust manager that does not validate certificate chains
			final TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {
						
						@Override
						public X509Certificate[] getAcceptedIssuers() {
							return new java.security.cert.X509Certificate[]{};
						}
						
						@Override
						public void checkServerTrusted(X509Certificate[] chain, String authType)
								throws CertificateException {
							
						}
						
						@Override
						public void checkClientTrusted(X509Certificate[] chain, String authType)
								throws CertificateException {
							
						}
					}
			};

			// Install the all-trusting trust manager
			final SSLContext sslContext = SSLContext.getInstance("SSL");
			sslContext.init(null, trustAllCerts,
					new java.security.SecureRandom());
			// Create an ssl socket factory with our all-trusting manager
			final SSLSocketFactory sslSocketFactory = sslContext
					.getSocketFactory();

			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.sslSocketFactory(sslSocketFactory);
			builder.hostnameVerifier(new HostnameVerifier() {

				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return true;
				}
			});

			OkHttpClient okHttpClient = builder.build();
			return okHttpClient;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
