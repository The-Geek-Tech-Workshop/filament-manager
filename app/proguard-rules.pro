# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# HiveMQ MQTT client - suppress warnings for optional/unavailable dependencies
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.AsyncTask
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.Buffer
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.CertificateCallback
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.CertificateCompressionAlgo
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.CertificateVerifier
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.Library
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SSL
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SSLContext
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SSLPrivateKeyMethod
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SSLSessionCache
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SessionTicketKey
-dontwarn com.hivemq.client.internal.shaded.io.netty.internal.tcnative.SniHostNameMatcher
-dontwarn io.netty.util.internal.Hidden$NettyBlockHoundIntegration
-dontwarn org.apache.log4j.Level
-dontwarn org.apache.log4j.Logger
-dontwarn org.apache.log4j.Priority
-dontwarn org.apache.logging.log4j.Level
-dontwarn org.apache.logging.log4j.LogManager
-dontwarn org.apache.logging.log4j.Logger
-dontwarn org.apache.logging.log4j.message.MessageFactory
-dontwarn org.apache.logging.log4j.spi.ExtendedLogger
-dontwarn org.apache.logging.log4j.spi.ExtendedLoggerWrapper
-dontwarn org.bouncycastle.asn1.pkcs.PrivateKeyInfo
-dontwarn org.bouncycastle.openssl.PEMDecryptorProvider
-dontwarn org.bouncycastle.openssl.PEMEncryptedKeyPair
-dontwarn org.bouncycastle.openssl.PEMKeyPair
-dontwarn org.bouncycastle.openssl.PEMParser
-dontwarn org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
-dontwarn org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder
-dontwarn org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
-dontwarn org.bouncycastle.operator.InputDecryptorProvider
-dontwarn org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
-dontwarn org.conscrypt.BufferAllocator
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.HandshakeListener
-dontwarn org.eclipse.jetty.alpn.ALPN$ClientProvider
-dontwarn org.eclipse.jetty.alpn.ALPN$Provider
-dontwarn org.eclipse.jetty.alpn.ALPN$ServerProvider
-dontwarn org.eclipse.jetty.alpn.ALPN
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ClientProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$Provider
-dontwarn org.eclipse.jetty.npn.NextProtoNego$ServerProvider
-dontwarn org.eclipse.jetty.npn.NextProtoNego
-dontwarn org.slf4j.ILoggerFactory
-dontwarn org.slf4j.Logger
-dontwarn org.slf4j.LoggerFactory
-dontwarn org.slf4j.Marker
-dontwarn org.slf4j.helpers.FormattingTuple
-dontwarn org.slf4j.helpers.MessageFormatter
-dontwarn org.slf4j.helpers.NOPLoggerFactory
-dontwarn org.slf4j.spi.LocationAwareLogger